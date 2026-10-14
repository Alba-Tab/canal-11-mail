package bo.edu.uagrm.ficct.canal11.negocio.cu08;

import bo.edu.uagrm.ficct.canal11.datos.Fila;
import bo.edu.uagrm.ficct.canal11.negocio.Contexto;
import bo.edu.uagrm.ficct.canal11.negocio.ErrorNegocio;
import bo.edu.uagrm.ficct.canal11.negocio.Periodo;
import bo.edu.uagrm.ficct.canal11.negocio.Resultado;
import bo.edu.uagrm.ficct.canal11.negocio.Totales;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * CU-08 Reportes y Estadísticas (Tabla 86 del Anexo A), con las consultas ya probadas en
 * {@code db/consultas_negocio.sql}.
 *
 * <ul>
 *   <li>Los REP reciben un período (ver {@link Periodo}) y responden una tabla con su fila de
 *       totales (plantilla P9); sin datos, «No hay datos para el período indicado».</li>
 *   <li>REPREC muestra solo los recursos que se usaron y cuenta los que no (D30).</li>
 *   <li>LISBIT (solo el Director) filtra por fecha, por remitente o por comando; sin filtro, los
 *       últimos {@value #ULTIMOS_BITACORA} registros. El asunto ya está enmascarado (D6).</li>
 * </ul>
 */
public final class ComandosReportes {

    private static final String CU = "CU-08";
    static final int ULTIMOS_BITACORA = 100;
    private static final int LARGO_RESPUESTA = 120;

    private ComandosReportes() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                periodo("REPCOB", "Coberturas por periodista").accion(ComandosReportes::repcob).build(),
                periodo("REPREC", "Uso de equipos y unidades móviles (horas)").accion(ComandosReportes::reprec).build(),
                periodo("REPMAN", "Incidencias de mantenimiento del período, con su cierre y días fuera de servicio")
                        .accion(ComandosReportes::repman).build(),
                periodo("REPNOT", "Notas emitidas, observadas y flotadas").accion(ComandosReportes::repnot).build(),
                CommandSpec.de(CU, "LISBIT", "Bitácora de comandos recibidos (RNF-24)")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISBIT[\"2026-10-12\"]").accion(ComandosReportes::lisbit).build());
    }

    private static CommandSpec.Builder periodo(String codigo, String descripcion) {
        return CommandSpec.de(CU, codigo, descripcion)
                .param("fecha_desde", Tipo.FECHA).param("fecha_hasta", Tipo.FECHA)
                .ejemplo(codigo + "[\"2026-10-01\",\"2026-10-31\"]");
    }

    // =================================================================== reportes (P9)

    private static Resultado repcob(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                SELECT u.nombre || ' ' || u.apellido AS "Periodista",
                       COUNT(*) FILTER (WHERE c.estado = 'cumplida')     AS "Cumplidas",
                       COUNT(*) FILTER (WHERE c.estado = 'cancelada')    AS "Canceladas",
                       COUNT(*) FILTER (WHERE c.estado = 'reprogramada') AS "Reprogramadas",
                       COUNT(*) FILTER (WHERE c.estado IN ('planificada', 'en_curso')) AS "Pendientes",
                       COUNT(*) AS "Total"
                FROM cobertura c
                JOIN cobertura_personal cp ON cp.cobertura_id = c.id AND cp.funcion = 'periodista'
                JOIN personal p ON p.id = cp.personal_id
                JOIN usuario u ON u.id = p.usuario_id
                WHERE c.fecha_hora_inicio::date BETWEEN ? AND ?
                GROUP BY u.nombre, u.apellido
                ORDER BY "Total" DESC, u.apellido
                """, per.desde(), per.hasta());
        return reporte("Coberturas por periodista " + per.texto(), filas,
                "Cumplidas", "Canceladas", "Reprogramadas", "Pendientes", "Total");
    }

    private static Resultado reprec(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                SELECT r.codigo AS "Código", tr.nombre AS "Tipo", r.marca || ' ' || r.modelo AS "Equipo",
                       COUNT(c.id) AS "Coberturas",
                       ROUND(SUM(EXTRACT(EPOCH FROM (c.fecha_hora_fin - c.fecha_hora_inicio)) / 3600)::numeric, 1) AS "Horas"
                FROM recurso r
                JOIN tipo_recurso tr ON tr.id = r.tipo_recurso_id
                JOIN cobertura_recurso cr ON cr.recurso_id = r.id
                JOIN cobertura c ON c.id = cr.cobertura_id AND c.estado <> 'cancelada'
                WHERE c.fecha_hora_inicio::date BETWEEN ? AND ?
                GROUP BY r.codigo, tr.nombre, r.marca, r.modelo
                ORDER BY "Horas" DESC, r.codigo
                """, per.desde(), per.hasta());
        Resultado r = reporte("Uso de equipos y unidades móviles " + per.texto(), filas, "Coberturas", "Horas");
        if (!filas.isEmpty()) {
            long sinUso = ctx.uno("""
                    SELECT count(*) AS n FROM recurso r
                    WHERE r.estado <> 'baja'
                      AND NOT EXISTS (SELECT 1 FROM cobertura_recurso cr JOIN cobertura c ON c.id = cr.cobertura_id
                                      WHERE cr.recurso_id = r.id AND c.estado <> 'cancelada'
                                        AND c.fecha_hora_inicio::date BETWEEN ? AND ?)
                    """, per.desde(), per.hasta()).orElseThrow().largo("n");
            r.parrafo(sinUso == 0 ? "Todos los recursos se usaron en el período."
                    : sinUso + " recurso(s) no se usaron en el período (no se cuentan los dados de baja).");
        }
        return r;
    }

    private static Resultado repman(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                SELECT r.codigo AS "Código", tr.nombre AS "Tipo", i.fecha_hora AS "Incidencia", i.descripcion AS "Descripción",
                       COALESCE(a.tipo, 'abierta') AS "Cierre", a.fecha_hora AS "Cerrada en",
                       ROUND((EXTRACT(EPOCH FROM (COALESCE(a.fecha_hora, LOCALTIMESTAMP) - i.fecha_hora)) / 86400)::numeric, 1) AS "Días fuera",
                       r.estado AS "Estado actual"
                FROM mantenimiento i
                JOIN recurso r       ON r.id = i.recurso_id
                JOIN tipo_recurso tr ON tr.id = r.tipo_recurso_id
                LEFT JOIN LATERAL (SELECT m.tipo, m.fecha_hora FROM mantenimiento m
                                   WHERE m.recurso_id = i.recurso_id AND m.tipo IN ('alta', 'baja') AND m.fecha_hora > i.fecha_hora
                                   ORDER BY m.fecha_hora LIMIT 1) a ON TRUE
                WHERE i.tipo = 'incidencia' AND i.fecha_hora::date BETWEEN ? AND ?
                ORDER BY i.fecha_hora
                """, per.desde(), per.hasta());
        Resultado r = reporte("Incidencias de mantenimiento " + per.texto(), filas, "Días fuera");
        long abiertas = filas.stream().filter(f -> "abierta".equals(f.texto("Cierre"))).count();
        if (abiertas > 0) {
            r.parrafo(abiertas + " incidencia(s) siguen abiertas: sus días fuera de servicio se cuentan hasta hoy.");
        }
        return r;
    }

    private static Resultado repnot(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> emitidas = ctx.consultar("""
                SELECT n.id AS "Id", n.slug AS "Slug", n.titulo AS "Título", n.fecha AS "Fecha", n.tipo_pieza AS "Tipo"
                FROM nota n WHERE n.estado = 'emitida' AND n.fecha BETWEEN ? AND ? ORDER BY n.fecha, n.slug
                """, per.desde(), per.hasta());
        List<Fila> observadas = ctx.consultar("""
                SELECT n.id AS "Id", n.slug AS "Slug", count(*) AS "Observaciones", max(r.fecha_hora) AS "Última", n.estado AS "Estado actual"
                FROM revision_nota r JOIN nota n ON n.id = r.nota_id
                WHERE r.accion = 'observacion' AND r.fecha_hora::date BETWEEN ? AND ?
                GROUP BY n.id, n.slug, n.estado ORDER BY max(r.fecha_hora)
                """, per.desde(), per.hasta());
        List<Fila> flotadas = ctx.consultar("""
                SELECT e.fecha AS "Emisión", n.slug AS "Slug", COALESCE(n.titulo, i.titulo) AS "Título", i.bloque AS "Bloque"
                FROM item_escaleta i JOIN escaleta e ON e.id = i.escaleta_id LEFT JOIN nota n ON n.id = i.nota_id
                WHERE i.estado = 'flotado' AND e.fecha BETWEEN ? AND ? ORDER BY e.fecha, i.bloque, i.orden
                """, per.desde(), per.hasta());
        String titulo = "Notas emitidas, observadas y flotadas " + per.texto();
        if (emitidas.isEmpty() && observadas.isEmpty() && flotadas.isEmpty()) {
            return Resultado.informacion(titulo, Periodo.SIN_DATOS);
        }
        Resultado r = Resultado.informacion(titulo, "Emitidas: " + emitidas.size() + " · Observadas: " + observadas.size()
                + " · Flotadas: " + flotadas.size() + ".");
        seccion(r, "Emitidas", emitidas);
        seccion(r, "Observadas", observadas);
        seccion(r, "Flotadas", flotadas);
        return r;
    }

    private static void seccion(Resultado r, String titulo, List<Fila> filas) {
        r.subtitulo(titulo + " (" + filas.size() + ")");
        if (filas.isEmpty()) {
            r.parrafo("Ninguna en el período.");
        } else {
            r.tabla(filas);
        }
    }

    /** Plantilla P9: tabla con su fila de totales, o «No hay datos para el período indicado». */
    private static Resultado reporte(String titulo, List<Fila> filas, String... columnasSumadas) {
        if (filas.isEmpty()) {
            return Resultado.informacion(titulo, Periodo.SIN_DATOS);
        }
        return Resultado.informacion(titulo, filas.size() + (filas.size() == 1 ? " fila." : " filas."))
                .tabla(Totales.conTotal(filas, columnasSumadas));
    }

    // =================================================================== bitácora (RNF-24)

    private static final String BITACORA = """
            SELECT b.fecha_hora AS "Fecha", b.remitente AS "Remitente", b.comando AS "Comando", b.asunto AS "Asunto",
                   b.resultado AS "Resultado",
                   CASE WHEN length(b.respuesta) > %d THEN left(b.respuesta, %d) || '…' ELSE b.respuesta END AS "Respuesta"
            FROM bitacora_comando b
            """.formatted(LARGO_RESPUESTA, LARGO_RESPUESTA);

    private static Resultado lisbit(Contexto ctx) throws SQLException {
        String filtro = ctx.p().texto("filtro");
        List<Fila> filas;
        String titulo;
        if (filtro == null) {
            titulo = "Bitácora: últimos registros";
            filas = ctx.consultar(BITACORA + " ORDER BY b.fecha_hora DESC, b.id DESC LIMIT ?", ULTIMOS_BITACORA);
        } else if (comoFecha(filtro) != null) {
            LocalDate dia = comoFecha(filtro);
            titulo = "Bitácora del " + dia;
            filas = ctx.consultar(BITACORA + " WHERE b.fecha_hora::date = ? ORDER BY b.fecha_hora DESC, b.id DESC", dia);
        } else if (ctx.catalogo().existe(filtro.toUpperCase(Locale.ROOT)) || filtro.equalsIgnoreCase("AYUDA")) {
            String comando = filtro.toUpperCase(Locale.ROOT);
            titulo = "Bitácora de " + comando;
            filas = ctx.consultar(BITACORA + " WHERE b.comando = ? ORDER BY b.fecha_hora DESC, b.id DESC LIMIT ?", comando, ULTIMOS_BITACORA);
        } else {
            titulo = "Bitácora de «" + filtro + "»";
            filas = ctx.consultar(BITACORA + " WHERE lower(b.remitente) LIKE ? ESCAPE '\\' ORDER BY b.fecha_hora DESC, b.id DESC LIMIT ?",
                    "%" + filtro.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%",
                    ULTIMOS_BITACORA);
        }
        Resultado r = Resultado.listado(titulo, filas, "No hay registros en la bitácora con ese filtro. Use \"*\", una fecha AAAA-MM-DD, "
                + "un remitente o un código de comando.");
        if (filas.size() == ULTIMOS_BITACORA) {
            r.parrafo("Se muestran los últimos " + ULTIMOS_BITACORA + ": filtre por fecha, remitente o comando para ver otros.");
        }
        return r;
    }

    /** El filtro como fecha, o {@code null} si no es una fecha. */
    private static LocalDate comoFecha(String texto) {
        try {
            return (LocalDate) Tipo.FECHA.convertir(texto, "filtro");
        } catch (ErrorNegocio e) {
            return null;
        }
    }
}
