package bo.edu.uagrm.ficct.canal11.negocio;

import bo.edu.uagrm.ficct.canal11.datos.Bitacora;
import bo.edu.uagrm.ficct.canal11.datos.Fila;
import bo.edu.uagrm.ficct.canal11.datos.Repositorio;
import bo.edu.uagrm.ficct.canal11.datos.TraductorErrores;
import bo.edu.uagrm.ficct.canal11.datos.TraductorErrores.Operacion;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Ayuda;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Comando;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Invalido;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Catalogo;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Parametros;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Procesador de comandos («CommandProcessor» del documento): sigue el flujo de la sección 4.2.1
 * para cada correo.
 *
 * <ol>
 *   <li>AYUDA: el menú del rol del remitente (P1).</li>
 *   <li>El código existe en el catálogo; si no, «comando no reconocido».</li>
 *   <li>El remitente está registrado y activo, y su rol tiene el permiso del comando (R1, D16).</li>
 *   <li>Cantidad y tipo de los parámetros; INS, UPD y DEL sin corchetes piden el formato.</li>
 *   <li>La acción del comando, en una transacción: si falla, se deshace todo (D12).</li>
 *   <li>La bitácora, aparte y siempre, con el asunto enmascarado (R2, D6).</li>
 * </ol>
 *
 * <p>Nunca lanza excepciones: todo termina en una respuesta para el remitente.</p>
 */
public final class CommandProcessor implements ProcesadorCorreo {

    private static final Logger LOG = Logger.getLogger(CommandProcessor.class.getName());

    static final String NO_REGISTRADO = "No está registrado en el sistema. Contacte al Director del canal.";
    static final String ERROR_GENERAL = "Error: no se pudo completar el comando; intente de nuevo más tarde";

    /** Quién da la conexión: {@code DBConnection} en la aplicación, una conexión de prueba en los tests. */
    @FunctionalInterface
    public interface FuenteConexion {
        Connection obtener() throws SQLException;
    }

    private static final String REMITENTE = """
            SELECT u.id, u.nombre, u.apellido, u.correo, u.estado, u.rol_id, r.nombre AS rol,
                   p.id AS personal_id, p.cargo, p.estado AS estado_personal
            FROM usuario u
            JOIN rol r ON r.id = u.rol_id
            LEFT JOIN personal p ON p.usuario_id = u.id
            WHERE lower(u.correo) = lower(?)
            ORDER BY (u.correo = ?) DESC
            """;

    private static final String PERMISO = """
            SELECT 1 FROM rol_permiso rp JOIN permiso p ON p.id = rp.permiso_id
            WHERE rp.rol_id = ? AND p.comando = ?
            """;

    private final FuenteConexion fuente;
    private final Repositorio repositorio;
    private final Catalogo catalogo;
    private final Clock reloj;
    private final Bitacora bitacora;
    private final MenuAyuda menuAyuda;

    public CommandProcessor(FuenteConexion fuente, Repositorio repositorio, Catalogo catalogo, Clock reloj) {
        this.fuente = fuente;
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.reloj = reloj;
        this.bitacora = new Bitacora(repositorio);
        this.menuAyuda = new MenuAyuda(repositorio, catalogo);
    }

    /** Cómo terminó un correo: la página HTML, los adjuntos y lo que se anota en la bitácora. */
    private record Atencion(String html, String resumen, boolean exito, Integer usuarioId, String comando, List<Adjunto> adjuntos) {
        Atencion(String html, String resumen, boolean exito, Integer usuarioId, String comando) {
            this(html, resumen, exito, usuarioId, comando, List.of());
        }
    }

    @Override
    public RespuestaCorreo procesar(SolicitudCorreo s) {
        String asuntoVisible = Enmascarador.asunto(s.asunto());
        ComandoParseado parseado = CommandParser.analizar(s.asunto());
        Connection c = null;
        Atencion a;
        try {
            c = fuente.obtener();
            a = atender(c, s, parseado);
        } catch (SQLException e) {
            LOG.warning("Base de datos no disponible al atender a " + s.remitente() + ": " + e.getMessage());
            String m = TraductorErrores.traducir(e, Operacion.CONSULTAR);
            a = new Atencion(HTMLResponseBuilder.error(m), m, false, null, codigoDe(parseado));
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Error inesperado con el correo de " + s.remitente() + " («" + asuntoVisible + "»)", e);
            a = new Atencion(HTMLResponseBuilder.error(ERROR_GENERAL), ERROR_GENERAL, false, null, codigoDe(parseado));
        }
        if (c != null) {
            try {
                bitacora.registrar(c, s.remitente(), a.usuarioId(), asuntoVisible, a.comando(), a.exito(), a.resumen());
            } catch (SQLException | RuntimeException e) {
                LOG.warning("No se pudo escribir la bitácora del correo de " + s.remitente() + ": " + e.getMessage());
            }
        }
        LOG.info((a.exito() ? "Atendido" : "Rechazado") + " «" + asuntoVisible + "» de " + s.remitente() + ": " + a.resumen());
        return new RespuestaCorreo("Re: " + (asuntoVisible.isBlank() ? "(sin asunto)" : asuntoVisible), a.html(), a.adjuntos());
    }

    private Atencion atender(Connection c, SolicitudCorreo s, ComandoParseado parseado) throws SQLException {
        Optional<Remitente> remitente = buscarRemitente(c, s.remitente());
        Integer usuarioId = remitente.map(Remitente::usuarioId).orElse(null);

        if (parseado instanceof Ayuda) {
            if (remitente.isEmpty()) {
                return error(NO_REGISTRADO, null, "AYUDA");
            }
            Resultado r = menuAyuda.armar(c, remitente.get());
            return new Atencion(HTMLResponseBuilder.pagina(r.titulo(), r.contenidoHtml()), r.mensaje(), true, usuarioId, "AYUDA");
        }
        if (parseado instanceof Invalido i) {
            Optional<CommandSpec> spec = catalogo.buscar(i.codigo());
            return errorDeComando(i.mensaje(), "", spec.orElse(null), usuarioId);
        }

        Comando cmd = (Comando) parseado;
        Optional<CommandSpec> buscado = catalogo.buscar(cmd.codigo());
        if (buscado.isEmpty()) {
            return error(CommandParser.NO_RECONOCIDO, usuarioId, null);
        }
        CommandSpec spec = buscado.get();
        if (remitente.isEmpty()) {
            return error("Error: no tiene permiso para ejecutar " + spec.codigo() + ". " + NO_REGISTRADO, null, spec.codigo());
        }
        if (repositorio.consultar(c, PERMISO, remitente.get().rolId(), spec.codigo()).isEmpty()) {
            return error("Error: no tiene permiso para ejecutar " + spec.codigo(), usuarioId, spec.codigo());
        }

        List<String> valores = cmd.parametros();
        if (spec.operacion().equals("LIS") && valores.isEmpty()) {
            valores = List.of("*");  // LIS sin corchetes (o con [] vacíos) lista todo (4.2.2)
        } else if (!cmd.conCorchetes()) {
            return errorDeComando("Error: formato esperado " + spec.formato(), "", spec, usuarioId);
        }
        Parametros p;
        try {
            p = spec.validar(valores);
        } catch (ErrorNegocio e) {
            return errorDeComando(e.getMessage(), "", spec, usuarioId);
        }
        return ejecutar(c, spec, new Contexto(c, repositorio, remitente.get(), spec.codigo(), p, s.cuerpo(), reloj, catalogo));
    }

    /** Ejecuta la acción en una transacción; cualquier error la deshace. */
    private Atencion ejecutar(Connection c, CommandSpec spec, Contexto ctx) {
        Integer usuarioId = ctx.remitente().usuarioId();
        try {
            c.setAutoCommit(false);
            Resultado r = spec.accion().ejecutar(ctx);
            try (Statement st = c.createStatement()) {
                // Las restricciones diferidas (el orden de la escaleta) se verifican aquí, con mensaje traducido
                st.execute("SET CONSTRAINTS ALL IMMEDIATE");
            }
            c.commit();
            return new Atencion(HTMLResponseBuilder.pagina(r.titulo(), r.contenidoHtml()), r.mensaje(), true, usuarioId, spec.codigo(),
                    r.adjuntos());
        } catch (ErrorNegocio e) {
            deshacer(c);
            return errorDeComando(e.getMessage(), e.detalleHtml(), spec, usuarioId);
        } catch (SQLException e) {
            deshacer(c);
            LOG.warning(spec.codigo() + " falló en la base de datos (" + e.getSQLState() + "): " + e.getMessage());
            return errorDeComando(TraductorErrores.traducir(e, Operacion.delComando(spec.codigo())), "", spec, usuarioId);
        } catch (RuntimeException e) {
            deshacer(c);
            LOG.log(Level.SEVERE, spec.codigo() + " falló por un error inesperado", e);
            return error(ERROR_GENERAL, usuarioId, spec.codigo());
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException e) {
                LOG.log(Level.FINE, "No se pudo volver a autocommit", e);
            }
        }
    }

    private static void deshacer(Connection c) {
        try {
            c.rollback();
        } catch (SQLException e) {
            LOG.log(Level.FINE, "No se pudo deshacer la transacción", e);
        }
    }

    private Optional<Remitente> buscarRemitente(Connection c, String correo) throws SQLException {
        List<Fila> filas = repositorio.consultar(c, REMITENTE, correo, correo);
        if (filas.isEmpty() || !"activo".equals(filas.get(0).texto("estado"))) {
            return Optional.empty();  // no registrado o inactivo: lo mismo (D16)
        }
        Fila f = filas.get(0);
        boolean fichaActiva = f.get("personal_id") != null && "activo".equals(f.texto("estado_personal"));
        return Optional.of(new Remitente(f.entero("id"), f.texto("nombre"), f.texto("apellido"), f.texto("correo"),
                f.entero("rol_id"), f.texto("rol"), fichaActiva ? f.entero("personal_id") : null, fichaActiva ? f.texto("cargo") : null));
    }

    private static Atencion error(String mensaje, Integer usuarioId, String comando) {
        return new Atencion(HTMLResponseBuilder.error(mensaje), mensaje, false, usuarioId, comando);
    }

    /** Error con el formato y el ejemplo del comando, si se sabe cuál es (plantilla P5). */
    private static Atencion errorDeComando(String mensaje, String detalleHtml, CommandSpec spec, Integer usuarioId) {
        if (spec == null) {
            return new Atencion(HTMLResponseBuilder.errorDeComando("Error", mensaje, detalleHtml, null, null), mensaje, false, usuarioId, null);
        }
        String html = HTMLResponseBuilder.errorDeComando(spec.codigo(), mensaje, detalleHtml, spec.formato(), spec.ejemplo());
        return new Atencion(html, mensaje, false, usuarioId, spec.codigo());
    }

    private String codigoDe(ComandoParseado p) {
        if (p instanceof Ayuda) {
            return "AYUDA";
        }
        if (p instanceof Comando c && catalogo.existe(c.codigo())) {
            return c.codigo();
        }
        return null;
    }
}
