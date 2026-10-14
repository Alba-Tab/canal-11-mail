package bo.edu.uagrm.ficct.canal11.negocio.cu07;

import bo.edu.uagrm.ficct.canal11.datos.Fila;
import bo.edu.uagrm.ficct.canal11.negocio.Contexto;
import bo.edu.uagrm.ficct.canal11.negocio.ErrorNegocio;
import bo.edu.uagrm.ficct.canal11.negocio.Formato;
import bo.edu.uagrm.ficct.canal11.negocio.Periodo;
import bo.edu.uagrm.ficct.canal11.negocio.Resultado;
import bo.edu.uagrm.ficct.canal11.negocio.Totales;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.sql.SQLException;
import java.util.List;

/**
 * CU-07 Gestión de Analítica Periodística (Tabla 85 del Anexo A): indicadores por período, con las
 * consultas ya probadas en {@code db/consultas_negocio.sql}.
 *
 * <p>Las tres últimas letras de un ANA nombran el indicador, no una tabla (PER: periodistas, EMI:
 * emisiones, COB: coberturas, NOT: notas). Los porcentajes se muestran con «%», los tiempos de
 * aprobación en {@code h:mm} y las duraciones en {@code mm:ss} (R25, R26). Sin datos, «No hay
 * datos para el período indicado» (A2).</p>
 */
public final class ComandosAnalitica {

    private static final String CU = "CU-07";

    private ComandosAnalitica() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                periodo("ANAPER", "Producción por periodista: notas, % aprobadas a la primera y tiempo medio de aprobación")
                        .accion(ComandosAnalitica::anaper).build(),
                periodo("ANAEMI", "Precisión de las emisiones: duración real frente a la suma de la escaleta, e ítems de última hora")
                        .accion(ComandosAnalitica::anaemi).build(),
                periodo("ANACOB", "Cumplimiento de coberturas por tipo").accion(ComandosAnalitica::anacob).build(),
                periodo("ANANOT", "Duración media por tipo de pieza").accion(ComandosAnalitica::ananot).build());
    }

    private static CommandSpec.Builder periodo(String codigo, String descripcion) {
        return CommandSpec.de(CU, codigo, descripcion)
                .param("fecha_desde", Tipo.FECHA).param("fecha_hasta", Tipo.FECHA)
                .ejemplo(codigo + "[\"2026-10-01\",\"2026-10-31\"]");
    }

    /** Un porcentaje entero con «%», o {@code NULL} si no hay base para calcularlo. */
    private static String porcentaje(String parte, String total) {
        return "(ROUND(100.0 * " + parte + " / NULLIF(" + total + ", 0), 0)::int)::text || '%'";
    }

    /** Minutos como {@code h:mm}. */
    private static String horasMinutos(String minutos) {
        return "CASE WHEN " + minutos + " IS NULL THEN NULL ELSE (ROUND(" + minutos + ")::int / 60)::text || ':' || "
                + "lpad((ROUND(" + minutos + ")::int % 60)::text, 2, '0') END";
    }

    // =================================================================== indicadores

    private static Resultado anaper(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                WITH rev AS (
                  SELECT nota_id,
                         MIN(fecha_hora) FILTER (WHERE accion = 'envio')       AS primer_envio,
                         MAX(fecha_hora) FILTER (WHERE accion = 'aprobacion')  AS aprobada_en,
                         COUNT(*)        FILTER (WHERE accion = 'observacion') AS observaciones
                  FROM revision_nota GROUP BY nota_id),
                base AS (
                  SELECT u.nombre || ' ' || u.apellido AS autor, n.estado, r.aprobada_en, r.observaciones,
                         EXTRACT(EPOCH FROM (r.aprobada_en - r.primer_envio)) / 60 AS minutos
                  FROM nota n
                  JOIN personal p ON p.id = n.autor_id JOIN usuario u ON u.id = p.usuario_id
                  LEFT JOIN rev r ON r.nota_id = n.id
                  WHERE n.fecha BETWEEN ? AND ?)
                SELECT autor AS "Autor",
                       COUNT(*) AS "Notas",
                       COUNT(*) FILTER (WHERE estado IN ('aprobada', 'emitida')) AS "Aprobadas o emitidas",
                       %s AS "Aprobadas a la primera",
                       %s AS "Tiempo medio de aprobación (h:mm)"
                FROM base
                GROUP BY autor
                ORDER BY "Notas" DESC, autor
                """.formatted(porcentaje("COUNT(*) FILTER (WHERE aprobada_en IS NOT NULL AND observaciones = 0)",
                        "COUNT(*) FILTER (WHERE aprobada_en IS NOT NULL)"),
                horasMinutos("AVG(minutos)")), per.desde(), per.hasta());
        return indicador("Producción por periodista " + per.texto(), filas, "Notas", "Aprobadas o emitidas")
                .parrafo("«A la primera»: aprobadas sin ninguna observación. El tiempo de aprobación va del primer envío a revisión a la aprobación.");
    }

    private static Resultado anaemi(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                SELECT e.fecha AS "Fecha", p.nombre AS "Programa",
                       %s AS "Plantilla", %s AS "Suma de la escaleta", %s AS "Real", %s AS "Desviación",
                       (SELECT COUNT(*) FROM item_escaleta i WHERE i.escaleta_id = e.id AND i.ultima_hora) AS "Ítems de última hora",
                       (SELECT ROUND(AVG(g.duracion_real - g.duracion)::numeric, 1) FROM v_escaleta_grilla g
                         WHERE g.escaleta_id = e.id AND g.duracion_real IS NOT NULL AND g.tipo <> 'corte') AS "Desviación media por ítem (s)"
                FROM escaleta e
                JOIN grilla gr ON gr.id = e.grilla_id
                JOIN programa p ON p.id = gr.programa_id
                CROSS JOIN LATERAL (SELECT COALESCE(SUM(g.duracion), 0)::int AS estimada FROM v_escaleta_grilla g WHERE g.escaleta_id = e.id) est
                WHERE e.estado = 'emitida' AND e.fecha BETWEEN ? AND ?
                ORDER BY e.fecha, e.hora_inicio
                """.formatted(Formato.mmss("e.duracion_total"), Formato.mmss("est.estimada"), Formato.mmss("e.duracion_real"),
                Formato.mmss("(e.duracion_real - est.estimada)")), per.desde(), per.hasta());
        return indicador("Precisión de las emisiones " + per.texto(), filas, "Ítems de última hora")
                .parrafo("Desviación = duración real − suma de la escaleta (positiva: duró más de lo previsto).");
    }

    private static Resultado anacob(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                SELECT tipo AS "Tipo",
                       COUNT(*) AS "Coberturas",
                       COUNT(*) FILTER (WHERE estado = 'cumplida')     AS "Cumplidas",
                       COUNT(*) FILTER (WHERE estado = 'cancelada')    AS "Canceladas",
                       COUNT(*) FILTER (WHERE estado = 'reprogramada') AS "Reprogramadas",
                       COUNT(*) FILTER (WHERE estado IN ('planificada', 'en_curso')) AS "Sin retorno",
                       %s AS "Cumplimiento"
                FROM cobertura
                WHERE fecha_hora_inicio::date BETWEEN ? AND ?
                GROUP BY tipo
                ORDER BY "Coberturas" DESC, tipo
                """.formatted(porcentaje("COUNT(*) FILTER (WHERE estado = 'cumplida')",
                "COUNT(*) FILTER (WHERE estado IN ('cumplida', 'cancelada', 'reprogramada'))")), per.desde(), per.hasta());
        return indicador("Cumplimiento de coberturas por tipo " + per.texto(), filas,
                "Coberturas", "Cumplidas", "Canceladas", "Reprogramadas", "Sin retorno")
                .parrafo("Cumplimiento: cumplidas sobre las que ya tienen retorno (las pendientes no cuentan).");
    }

    private static Resultado ananot(Contexto ctx) throws SQLException, ErrorNegocio {
        Periodo per = Periodo.de(ctx.p());
        List<Fila> filas = ctx.consultar("""
                SELECT d.tipo_pieza AS "Tipo de pieza", COUNT(*) AS "Notas",
                       %s AS "Duración media", %s AS "Lectura media", %s AS "Video y SOT medio"
                FROM v_nota_duracion d JOIN nota n ON n.id = d.nota_id
                WHERE n.fecha BETWEEN ? AND ? AND n.estado <> 'baja'
                GROUP BY d.tipo_pieza
                ORDER BY AVG(d.duracion_total) DESC
                """.formatted(Formato.mmss("ROUND(AVG(d.duracion_total))::int"), Formato.mmss("ROUND(AVG(d.duracion_lectura))::int"),
                Formato.mmss("ROUND(AVG(d.duracion_video))::int")), per.desde(), per.hasta());
        return indicador("Duración media por tipo de pieza " + per.texto(), filas, "Notas")
                .parrafo("La lectura se estima con el ritmo propio de la nota o 150 palabras por minuto (R25).");
    }

    /** Tabla del indicador con su fila de totales, o «No hay datos para el período indicado». */
    private static Resultado indicador(String titulo, List<Fila> filas, String... columnasSumadas) {
        if (filas.isEmpty()) {
            return Resultado.informacion(titulo, Periodo.SIN_DATOS);
        }
        return Resultado.informacion(titulo, filas.size() + (filas.size() == 1 ? " fila." : " filas."))
                .tabla(Totales.conTotal(filas, columnasSumadas));
    }
}
