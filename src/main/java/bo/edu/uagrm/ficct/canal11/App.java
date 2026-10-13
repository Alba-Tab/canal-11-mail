package bo.edu.uagrm.ficct.canal11;

import bo.edu.uagrm.ficct.canal11.config.BloqueoInstancia;
import bo.edu.uagrm.ficct.canal11.config.Config;
import bo.edu.uagrm.ficct.canal11.config.Registro;
import bo.edu.uagrm.ficct.canal11.datos.DBConnection;
import bo.edu.uagrm.ficct.canal11.datos.Repositorio;
import bo.edu.uagrm.ficct.canal11.negocio.CoberturasEnCurso;
import bo.edu.uagrm.ficct.canal11.negocio.ProcesadorInicial;
import bo.edu.uagrm.ficct.canal11.presentacion.MailVerificationThread;
import bo.edu.uagrm.ficct.canal11.presentacion.SendEmailThread;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto de entrada de la aplicación de consola.
 *
 * <p>Uso: {@code java -Duser.timezone=America/La_Paz -jar canal11-mail.jar [config.properties]}</p>
 *
 * <p>Arma las tres capas y arranca los dos hilos: {@link MailVerificationThread}, que lee el buzón,
 * y {@link SendEmailThread}, que envía las respuestas. Se detiene con Ctrl+C o con {@code kill}: el
 * gancho de apagado termina el correo en curso y envía las respuestas pendientes.</p>
 */
public final class App {

    static {
        // Antes del primer Logger: el log debe sobrevivir hasta el final del apagado
        Registro.usarGestorPropio();
    }

    private static final Logger LOG = Logger.getLogger(App.class.getName());
    private static final long ESPERA_APAGADO_MS = 30_000;

    private App() {
    }

    public static void main(String[] args) {
        Registro.iniciarConsola(ZoneId.of("America/La_Paz"));
        Path archivo = Path.of(args.length > 0 ? args[0] : Config.ARCHIVO_POR_DEFECTO);
        Config config;
        try {
            config = Config.cargar(archivo);
            Registro.iniciar(config.carpetaLogs(), config.zonaHoraria());
        } catch (Config.ConfigException e) {
            LOG.severe(e.getMessage());
            return;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "No se pudo leer la configuración", e);
            return;
        }

        try {
            Optional<BloqueoInstancia> bloqueo = BloqueoInstancia.tomar(config.archivoBloqueo());
            if (bloqueo.isEmpty()) {
                LOG.severe("Ya hay otra copia de la aplicación corriendo (bloqueo en " + config.archivoBloqueo().toAbsolutePath()
                        + "). Esta copia termina.");
                return;
            }
            try (BloqueoInstancia b = bloqueo.get()) {
                ejecutar(config);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "La aplicación terminó por un error", e);
        }
    }

    private static void ejecutar(Config config) throws InterruptedException {
        LOG.info("Canal 11 vía correo: " + config.resumen());
        Clock reloj = Clock.system(config.zonaHoraria());
        DBConnection db = new DBConnection(config);
        Repositorio repositorio = new Repositorio(config.timeoutSegundos());
        CoberturasEnCurso enCurso = new CoberturasEnCurso(repositorio);

        SendEmailThread emisor = new SendEmailThread(config, reloj);
        MailVerificationThread lector = new MailVerificationThread(config,
                new ProcesadorInicial(db, repositorio), emisor,
                () -> {
                    int cambiadas = enCurso.actualizar(db.obtener());
                    if (cambiadas > 0) {
                        LOG.info(cambiadas + " cobertura(s) pasaron a en_curso");
                    }
                });

        Thread apagado = new Thread(() -> {
            LOG.info("Deteniendo la aplicación…");
            try {
                lector.detener(ESPERA_APAGADO_MS);
                emisor.detener(ESPERA_APAGADO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                db.cerrar();
                LOG.info("Aplicación detenida");
                Registro.terminar();
            }
        }, "apagado");
        Runtime.getRuntime().addShutdownHook(apagado);

        emisor.start();
        lector.start();
        lector.join();
        emisor.join();
    }
}
