package bo.edu.uagrm.ficct.canal11.negocio;

import bo.edu.uagrm.ficct.canal11.datos.DBConnection;
import bo.edu.uagrm.ficct.canal11.datos.Fila;
import bo.edu.uagrm.ficct.canal11.datos.Repositorio;
import bo.edu.uagrm.ficct.canal11.datos.TraductorErrores;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Ayuda;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Comando;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Invalido;
import bo.edu.uagrm.ficct.canal11.negocio.HTMLResponseBuilder.Tipo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Procesador provisional del primer despliegue: con él se prueba de punta a punta que el sistema
 * lee el buzón, identifica al remitente y responde, antes de tener los 112 comandos.
 *
 * <ul>
 *   <li>Remitente no registrado o inactivo: AYUDA responde la decisión M8 («No está registrado…»)
 *       y un comando responde «no tiene permiso» (flujo alternativo A1).</li>
 *   <li>AYUDA: saluda al usuario con su rol y recuerda el formato.</li>
 *   <li>Un comando: confirma que lo recibió, con sus parámetros enmascarados.</li>
 *   <li>Un asunto inválido: el mensaje de la Tabla 77.</li>
 * </ul>
 * <p>Lo reemplaza {@code CommandProcessor} en la rama {@code feat/procesador-y-ayuda}.</p>
 */
public final class ProcesadorInicial implements ProcesadorCorreo {

    private static final Logger LOG = Logger.getLogger(ProcesadorInicial.class.getName());

    static final String NO_REGISTRADO = "No está registrado en el sistema. Contacte al Director del canal.";

    private static final String USUARIO_POR_CORREO = """
            SELECT u.id, u.nombre, u.apellido, u.estado, r.nombre AS rol
            FROM usuario u
            JOIN rol r ON r.id = u.rol_id
            WHERE lower(u.correo) = lower(?)
            """;

    private final DBConnection db;
    private final Repositorio repositorio;

    public ProcesadorInicial(DBConnection db, Repositorio repositorio) {
        this.db = db;
        this.repositorio = repositorio;
    }

    @Override
    public RespuestaCorreo procesar(SolicitudCorreo s) {
        String asuntoRespuesta = "Re: " + (s.asunto().isBlank() ? "(sin asunto)" : Enmascarador.asunto(s.asunto()));
        ComandoParseado comando = CommandParser.analizar(s.asunto());

        Optional<Fila> usuario;
        try {
            usuario = repositorio.consultarUno(db.obtener(), USUARIO_POR_CORREO, s.remitente());
        } catch (SQLException e) {
            LOG.warning("No se pudo buscar al remitente " + s.remitente() + ": " + e.getMessage());
            return new RespuestaCorreo(asuntoRespuesta,
                    HTMLResponseBuilder.error(TraductorErrores.traducir(e, TraductorErrores.Operacion.CONSULTAR)));
        }

        boolean activo = usuario.isPresent() && "activo".equals(usuario.get().texto("estado"));
        if (!activo) {
            String mensaje = comando instanceof Comando c
                    ? "Error: no tiene permiso para ejecutar " + c.codigo() + ". " + NO_REGISTRADO
                    : NO_REGISTRADO;
            return new RespuestaCorreo(asuntoRespuesta, HTMLResponseBuilder.error(mensaje));
        }
        Fila u = usuario.get();

        if (comando instanceof Ayuda) {
            return new RespuestaCorreo(asuntoRespuesta, ayuda(u));
        }
        if (comando instanceof Comando c) {
            return new RespuestaCorreo(asuntoRespuesta, recibido(c));
        }
        return new RespuestaCorreo(asuntoRespuesta, HTMLResponseBuilder.error(((Invalido) comando).mensaje()));
    }

    private static String ayuda(Fila u) {
        String nombre = u.texto("nombre") + " " + u.texto("apellido");
        List<String> ejemplos = List.of(
                HTMLResponseBuilder.codigo("LISCOB[\"2026-10-12\"]") + " — agenda de coberturas de un día",
                HTMLResponseBuilder.codigo("LISNOT[\"2026-10-12\",\"aprobada\"]") + " — notas aprobadas de un día",
                HTMLResponseBuilder.codigo("AYUDA") + " — esta ayuda");
        String contenido = HTMLResponseBuilder.mensaje(Tipo.INFORMACION,
                "Hola, " + nombre + " (" + u.texto("rol") + "). El sistema está en marcha: recibe sus comandos y le responde.")
                + HTMLResponseBuilder.parrafo("Escriba el comando en el asunto del correo. Algunos ejemplos:")
                + HTMLResponseBuilder.lista(ejemplos)
                + HTMLResponseBuilder.parrafo("La lista completa de los comandos de su rol se habilita en la próxima versión.");
        return HTMLResponseBuilder.pagina("Ayuda", contenido);
    }

    private static String recibido(Comando c) {
        List<String> parametros = new ArrayList<>();
        for (String p : Enmascarador.parametros(c)) {
            parametros.add(HTMLResponseBuilder.codigo(p.isEmpty() ? "(vacío)" : p));
        }
        String contenido = HTMLResponseBuilder.mensaje(Tipo.ADVERTENCIA,
                "Comando " + c.codigo() + " recibido, pero todavía no está habilitado en esta versión.")
                + (parametros.isEmpty()
                ? HTMLResponseBuilder.parrafo(c.conCorchetes() ? "Sin parámetros." : "Sin corchetes ni parámetros.")
                : HTMLResponseBuilder.parrafo("Parámetros recibidos (" + parametros.size() + "):") + HTMLResponseBuilder.lista(parametros));
        return HTMLResponseBuilder.pagina(c.codigo(), contenido);
    }
}
