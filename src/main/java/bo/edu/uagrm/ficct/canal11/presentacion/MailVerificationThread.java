package bo.edu.uagrm.ficct.canal11.presentacion;

import bo.edu.uagrm.ficct.canal11.config.Config;
import bo.edu.uagrm.ficct.canal11.negocio.Enmascarador;
import bo.edu.uagrm.ficct.canal11.negocio.HTMLResponseBuilder;
import bo.edu.uagrm.ficct.canal11.negocio.ProcesadorCorreo;
import bo.edu.uagrm.ficct.canal11.negocio.Adjunto;
import bo.edu.uagrm.ficct.canal11.negocio.RespuestaCorreo;
import bo.edu.uagrm.ficct.canal11.negocio.SolicitudCorreo;
import bo.edu.uagrm.ficct.canal11.presentacion.correo.CorreoSaliente;
import bo.edu.uagrm.ficct.canal11.presentacion.correo.MensajeCorreo;
import bo.edu.uagrm.ficct.canal11.presentacion.correo.MimeParser;
import bo.edu.uagrm.ficct.canal11.presentacion.correo.Pop3Client;
import bo.edu.uagrm.ficct.canal11.presentacion.correo.Pop3Exception;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receptor POP3 («MailVerificationThread» del documento): el único hilo que lee y procesa los
 * correos, de a uno y en orden de llegada (decisión D3).
 *
 * <p>Cada vuelta:</p>
 * <ol>
 *   <li>ejecuta la tarea previa (pasar a {@code en_curso} las coberturas que ya empezaron, D4);</li>
 *   <li>saca el primer correo del buzón en su propia sesión POP: {@code RETR 1} → {@code DELE 1} →
 *       {@code QUIT}, y lo procesa solo si {@code QUIT} respondió {@code +OK}. Si la app se cae a
 *       mitad de camino, ningún comando se ejecuta dos veces;</li>
 *   <li>repite hasta vaciar el buzón y espera el intervalo configurado.</li>
 * </ol>
 * <p>Que nada la tumbe (D9): un correo que falla recibe una respuesta de error y se sigue con el
 * siguiente; si se cae POP3 o la base, se escribe en el log y se reintenta en la vuelta siguiente.
 * Los correos de más del tamaño máximo no se descargan: se leen sus cabeceras con TOP, se borran y
 * se responde que el sistema no acepta adjuntos. A los correos automáticos (rebotes, respuestas
 * automáticas) y a los del propio sistema no se les responde.</p>
 */
public final class MailVerificationThread extends Thread {

    private static final Logger LOG = Logger.getLogger(MailVerificationThread.class.getName());

    /** Tope por vuelta: si el buzón se llena de golpe, igual se ejecuta la tarea previa cada tanto. */
    static final int MAX_CORREOS_POR_VUELTA = 100;
    static final String ERROR_GENERAL = "Error: no se pudo procesar el comando; intente de nuevo más tarde";
    private static final String NOMBRE_REMITENTE = "Sistema Canal 11";

    /** Algo que se ejecuta al inicio de cada vuelta. Si falla, la vuelta sigue igual. */
    @FunctionalInterface
    public interface TareaPrevia {
        void ejecutar() throws Exception;
    }

    /** Un correo sacado del buzón: completo, o solo sus cabeceras si pasaba el tamaño máximo. */
    private record Extraido(byte[] contenido, boolean demasiadoGrande, long tamano) {
    }

    private final Config config;
    private final ProcesadorCorreo procesador;
    private final EnvioRespuestas envio;
    private final TareaPrevia tareaPrevia;
    private final Object pausa = new Object();
    private volatile boolean detenido;

    public MailVerificationThread(Config config, ProcesadorCorreo procesador, EnvioRespuestas envio, TareaPrevia tareaPrevia) {
        super("MailVerificationThread");
        this.config = config;
        this.procesador = procesador;
        this.envio = envio;
        this.tareaPrevia = tareaPrevia;
    }

    @Override
    public void run() {
        LOG.info("Lectura del buzón iniciada: " + config.pop3Usuario() + "@" + config.pop3Host()
                + ", cada " + config.intervaloSegundos() + " s");
        while (!detenido) {
            try {
                procesarUnaVez();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Error inesperado en la vuelta; se reintenta en la siguiente", t);
            }
            dormir(config.intervaloSegundos() * 1000L);
        }
        LOG.info("Lectura del buzón detenida");
    }

    /** Detiene el bucle al terminar el correo en curso y espera a que el hilo termine (hasta {@code esperaMs}). */
    public void detener(long esperaMs) throws InterruptedException {
        detenido = true;
        synchronized (pausa) {
            pausa.notifyAll();
        }
        join(esperaMs);
    }

    /**
     * Una vuelta completa: tarea previa y todos los correos del buzón (hasta {@link #MAX_CORREOS_POR_VUELTA}).
     * Nunca lanza excepciones.
     *
     * @return cuántos correos se sacaron del buzón
     */
    public int procesarUnaVez() {
        try {
            tareaPrevia.ejecutar();
        } catch (Exception e) {
            LOG.warning("La tarea previa de la vuelta falló: " + e.getMessage());
        }
        int sacados = 0;
        while (!detenido && sacados < MAX_CORREOS_POR_VUELTA) {
            Extraido correo;
            try {
                correo = sacarPrimero();
            } catch (Pop3Exception e) {
                if (e.buzonEnUso()) {
                    LOG.info("El buzón está abierto en otra sesión POP3; se reintenta en la próxima vuelta");
                } else {
                    LOG.warning("POP3 respondió un error: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                LOG.warning("No se pudo leer el buzón (" + config.pop3Host() + ":" + config.pop3Puerto() + "): " + e);
                break;
            }
            if (correo == null) {
                break;
            }
            sacados++;
            procesarCorreo(correo);
        }
        return sacados;
    }

    /**
     * Saca el primer correo del buzón en una sesión POP propia.
     *
     * @return el correo, o {@code null} si el buzón está vacío o si el servidor no confirmó el QUIT
     *         (entonces el correo sigue en el buzón y se procesa en la próxima vuelta)
     */
    private Extraido sacarPrimero() throws IOException {
        try (Pop3Client pop = Pop3Client.conectar(config.pop3Host(), config.pop3Puerto(), config.timeoutSegundos() * 1000)) {
            pop.login(config.pop3Usuario(), config.pop3Contrasena());
            if (pop.stat().mensajes() == 0) {
                pop.quit();
                return null;
            }
            long tamano = pop.list(1);
            boolean grande = tamano > config.maxBytesCorreo();
            byte[] contenido = grande ? pop.top(1, 0) : pop.retr(1);
            pop.dele(1);
            if (!pop.quit()) {
                LOG.warning("POP3 no confirmó el QUIT: el correo queda en el buzón para la próxima vuelta");
                return null;
            }
            return new Extraido(contenido, grande, tamano);
        }
    }

    private void procesarCorreo(Extraido e) {
        MensajeCorreo m = MimeParser.leer(e.contenido());
        String asunto = Enmascarador.asunto(m.asunto());
        if (m.esAutomatico()) {
            LOG.info("Se descarta un correo automático de «" + m.remitente() + "» («" + asunto + "»)");
            return;
        }
        if (m.remitente().equals(config.correoSistema().toLowerCase(Locale.ROOT))) {
            LOG.info("Se descarta un correo del propio sistema («" + asunto + "»)");
            return;
        }
        LOG.info("Correo de " + m.remitente() + ": «" + asunto + "»");

        RespuestaCorreo respuesta;
        if (e.demasiadoGrande()) {
            respuesta = new RespuestaCorreo("Re: " + asunto, HTMLResponseBuilder.error(
                    "Error: el correo pesa " + kb(e.tamano()) + " KB y el máximo es " + kb(config.maxBytesCorreo())
                            + " KB. El sistema no acepta adjuntos: envíe el comando sin archivos."));
        } else {
            try {
                respuesta = procesador.procesar(new SolicitudCorreo(m.remitente(), m.asunto(), m.cuerpoSinCitas()));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Falló el procesamiento del correo de " + m.remitente() + " («" + asunto + "»)", ex);
                respuesta = new RespuestaCorreo("Re: " + asunto, HTMLResponseBuilder.error(ERROR_GENERAL));
            }
        }
        if (respuesta == null) {
            return;
        }
        try {
            CorreoSaliente.Builder correo = CorreoSaliente.builder()
                    .de(config.correoSistema(), NOMBRE_REMITENTE)
                    .para(m.remitente())
                    .asunto(respuesta.asunto())
                    .html(respuesta.html())
                    .enRespuestaA(m.messageId());
            for (Adjunto a : respuesta.adjuntos()) {
                correo.adjunto(a.nombre(), a.tipoContenido(), a.contenido());
            }
            envio.encolar(correo.build());
        } catch (IllegalArgumentException ex) {
            LOG.warning("No se puede responder a «" + m.remitente() + "»: " + ex.getMessage());
        }
    }

    private static long kb(long bytes) {
        return (bytes + 1023) / 1024;
    }

    private void dormir(long ms) {
        synchronized (pausa) {
            if (detenido) {
                return;
            }
            try {
                pausa.wait(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                detenido = true;
            }
        }
    }
}
