package bo.edu.uagrm.ficct.canal11.negocio.cu06;

import bo.edu.uagrm.ficct.canal11.datos.Fila;
import bo.edu.uagrm.ficct.canal11.datos.TablaCrud;
import bo.edu.uagrm.ficct.canal11.negocio.Contexto;
import bo.edu.uagrm.ficct.canal11.negocio.ErrorNegocio;
import bo.edu.uagrm.ficct.canal11.negocio.Formato;
import bo.edu.uagrm.ficct.canal11.negocio.Resultado;
import bo.edu.uagrm.ficct.canal11.negocio.Sql;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Parametros;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CU-06, primera parte: programas, grilla, plantillas y cortes comerciales (Tabla 84 del Anexo A).
 *
 * <ul>
 *   <li>R16: un horario de grilla no cruza la medianoche (como máximo hasta 24:00).</li>
 *   <li>R17: un horario no choca con otro del mismo día si sus vigencias se cruzan.</li>
 *   <li>R18: una plantilla está lista con {@code cantidad_bloques − 1} cortes, numerados sin huecos,
 *       sin solaparse y dentro de la duración ({@code v_plantilla_control}).</li>
 *   <li>R19: los cortes y la estructura de una plantilla no cambian mientras tenga escaletas en
 *       borrador.</li>
 * </ul>
 */
public final class ComandosProgramacion {

    private static final String CU = "CU-06";
    private static final List<String> TIPOS = List.of("noticiero", "especial", "otro");
    static final Tipo TIPO_PROGRAMA = Tipo.opcion(TIPOS.toArray(String[]::new));
    static final Tipo RITMO = Tipo.entero(80, 250);
    private static final int MAXIMO_DIAS_GRILLA = 31;

    private ComandosProgramacion() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISPRG", "Listar programas")
                        .param("tipo", Tipo.FILTRO)
                        .ejemplo("LISPRG[\"noticiero\"]").accion(ComandosProgramacion::lisprg).build(),
                CommandSpec.de(CU, "INSPRG", "Registrar programa")
                        .param("nombre", Tipo.texto(100)).param("tipo", TIPO_PROGRAMA).opcional("descripcion", Tipo.texto(255))
                        .ejemplo("INSPRG[\"Noticiero Mediodía\",\"noticiero\",\"Edición de las 13:00\"]").accion(ComandosProgramacion::insprg).build(),
                CommandSpec.de(CU, "UPDPRG", "Modificar programa")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(100)).opcional("tipo", TIPO_PROGRAMA)
                        .opcional("descripcion", Tipo.texto(255)).omitible("estado", Tipo.opcion("activo", "inactivo"))
                        .ejemplo("UPDPRG[\"5\",\"Noticiero Mediodía\",\"noticiero\",\"Edición de las 13:00 a 13:30\"]")
                        .accion(ComandosProgramacion::updprg).build(),
                CommandSpec.de(CU, "DELPRG", "Desactivar programa")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPRG[\"4\"]").accion(ComandosProgramacion::delprg).build(),
                CommandSpec.de(CU, "LISGRI", "Ver la grilla de un rango de fechas")
                        .param("fecha_desde", Tipo.FECHA_O_TODOS).omitible("fecha_hasta", Tipo.FECHA)
                        .ejemplo("LISGRI[\"2026-10-12\",\"2026-10-18\"]").accion(ComandosProgramacion::lisgri).build(),
                CommandSpec.de(CU, "INSGRI", "Agregar horario a la grilla (valida choques)")
                        .param("id_programa", Tipo.ID).param("dia_semana", Tipo.entero(1, 7)).param("hora_inicio", Tipo.HORA_GRILLA)
                        .param("hora_fin", Tipo.HORA_GRILLA).opcional("vigente_desde", Tipo.FECHA).opcional("vigente_hasta", Tipo.FECHA)
                        .ejemplo("INSGRI[\"5\",\"1\",\"13:00\",\"13:30\",\"2026-10-19\",\"\"]").accion(ComandosProgramacion::insgri).build(),
                CommandSpec.de(CU, "UPDGRI", "Modificar horario de la grilla")
                        .param("id", Tipo.ID).opcional("hora_inicio", Tipo.HORA_GRILLA).opcional("hora_fin", Tipo.HORA_GRILLA)
                        .opcional("vigente_hasta", Tipo.FECHA)
                        .ejemplo("UPDGRI[\"8\",\"06:30\",\"07:30\",\"2026-12-31\"]").accion(ComandosProgramacion::updgri).build(),
                CommandSpec.de(CU, "DELGRI", "Quitar horario de la grilla")
                        .param("id", Tipo.ID)
                        .ejemplo("DELGRI[\"13\"]").accion(ComandosProgramacion::delgri).build(),
                CommandSpec.de(CU, "LISPLA", "Ver plantilla, bloques y tiempo neto")
                        .param("id", Tipo.ID_O_TODOS)
                        .ejemplo("LISPLA[\"1\"]").accion(ComandosProgramacion::lispla).build(),
                CommandSpec.de(CU, "INSPLA", "Registrar plantilla")
                        .param("id_programa", Tipo.ID).param("nombre", Tipo.texto(100)).param("duracion_min", Tipo.entero(1, 600))
                        .param("cantidad_bloques", Tipo.entero(1, 10)).opcional("palabras_por_minuto", RITMO)
                        .ejemplo("INSPLA[\"5\",\"Noticiero Mediodía\",\"30\",\"2\",\"155\"]").accion(ComandosProgramacion::inspla).build(),
                CommandSpec.de(CU, "UPDPLA", "Modificar plantilla")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(100)).opcional("duracion_min", Tipo.entero(1, 600))
                        .opcional("cantidad_bloques", Tipo.entero(1, 10)).opcional("palabras_por_minuto", RITMO)
                        .ejemplo("UPDPLA[\"2\",\"Noticiero Matinal\",\"60\",\"3\",\"165\"]").accion(ComandosProgramacion::updpla).build(),
                CommandSpec.de(CU, "DELPLA", "Desactivar plantilla")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPLA[\"3\"]").accion(ComandosProgramacion::delpla).build(),
                CommandSpec.de(CU, "LISCOR", "Cortes comerciales de una plantilla")
                        .param("id_plantilla", Tipo.ID_O_TODOS)
                        .ejemplo("LISCOR[\"1\"]").accion(ComandosProgramacion::liscor).build(),
                CommandSpec.de(CU, "INSCOR", "Agregar corte comercial")
                        .param("id_plantilla", Tipo.ID).param("numero", Tipo.entero(1, 9)).param("minuto_inicio", Tipo.entero(1, 600))
                        .param("duracion", Tipo.DURACION)
                        .ejemplo("INSCOR[\"4\",\"1\",\"15\",\"02:00\"]").accion(ComandosProgramacion::inscor).build(),
                CommandSpec.de(CU, "UPDCOR", "Modificar corte comercial")
                        .param("id", Tipo.ID).opcional("minuto_inicio", Tipo.entero(1, 600)).opcional("duracion", Tipo.DURACION)
                        .ejemplo("UPDCOR[\"2\",\"29\",\"03:00\"]").accion(ComandosProgramacion::updcor).build(),
                CommandSpec.de(CU, "DELCOR", "Eliminar corte comercial")
                        .param("id", Tipo.ID)
                        .ejemplo("DELCOR[\"7\"]").accion(ComandosProgramacion::delcor).build());
    }

    // =================================================================== programas

    private static final String PROGRAMAS = """
            SELECT p.id AS "Id", p.nombre AS "Programa", p.tipo AS "Tipo", p.descripcion AS "Descripción", p.estado AS "Estado",
                   (SELECT count(*) FROM grilla g WHERE g.programa_id = p.id AND g.estado = 'vigente') AS "Horarios vigentes",
                   (SELECT count(*) FROM plantilla pl WHERE pl.programa_id = p.id AND pl.estado = 'vigente') AS "Plantillas"
            FROM programa p
            """;

    private static Resultado lisprg(Contexto ctx) throws SQLException {
        String filtro = ctx.p().texto("tipo");
        List<Fila> filas;
        if (filtro == null) {
            filas = ctx.consultar(PROGRAMAS + " ORDER BY p.id");
        } else if (TIPOS.contains(Tipo.normalizarOpcion(filtro))) {
            filas = ctx.consultar(PROGRAMAS + " WHERE p.tipo = ? ORDER BY p.id", Tipo.normalizarOpcion(filtro));
        } else if (List.of("activo", "inactivo").contains(Tipo.normalizarOpcion(filtro))) {
            filas = ctx.consultar(PROGRAMAS + " WHERE p.estado = ? ORDER BY p.id", Tipo.normalizarOpcion(filtro));
        } else {
            filas = ctx.consultar(PROGRAMAS + " WHERE " + Sql.sinTildes("p.nombre") + " LIKE ? ORDER BY p.id", Sql.contiene(filtro));
        }
        return Resultado.listado("Programas", filas, "No hay programas con ese filtro. Use \"*\", un tipo (noticiero, especial, otro) o un nombre.");
    }

    private static Resultado insprg(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("nombre", p.texto("nombre"));
        v.put("tipo", p.texto("tipo"));
        v.put("descripcion", p.texto("descripcion"));
        int id = TablaCrud.insertar(ctx.repositorio(), ctx.conexion(), "programa", v);
        return Resultado.exito("Programas", "programa registrado").ficha(ctx.requerir(PROGRAMAS + " WHERE p.id = ?", id))
                .parrafo("Siguiente paso: su horario con INSGRI[\"" + id + "\",\"dia_semana\",\"hora_inicio\",\"hora_fin\",\"vigente_desde\",\"\"].");
    }

    private static Resultado updprg(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        p.requerirAlguno("nombre", "tipo", "descripcion", "estado");
        int id = p.entero("id");
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("nombre", p.texto("nombre"));
        v.put("tipo", p.texto("tipo"));
        v.put("descripcion", p.texto("descripcion"));
        v.put("estado", p.texto("estado"));
        TablaCrud.actualizar(ctx.repositorio(), ctx.conexion(), "programa", id, v).orElseThrow(ErrorNegocio::noExiste);
        return Resultado.exito("Programas", "programa modificado").ficha(ctx.requerir(PROGRAMAS + " WHERE p.id = ?", id));
    }

    private static Resultado delprg(Contexto ctx) throws SQLException, ErrorNegocio {
        int id = ctx.p().entero("id");
        Fila prg = ctx.requerir("SELECT estado FROM programa WHERE id = ?", id);
        if ("inactivo".equals(prg.texto("estado"))) {
            throw ErrorNegocio.de("el programa ya está inactivo");
        }
        TablaCrud.cambiarEstado(ctx.repositorio(), ctx.conexion(), "programa", id, "inactivo");
        return Resultado.exito("Programas", "programa desactivado").ficha(ctx.requerir(PROGRAMAS + " WHERE p.id = ?", id))
                .parrafo("Deja de figurar en la grilla. Para reactivarlo: UPDPRG[\"" + id + "\",\"\",\"\",\"\",\"activo\"].");
    }

    // =================================================================== grilla (R16, R17)

    private static Resultado lisgri(Contexto ctx) throws SQLException, ErrorNegocio {
        LocalDate desde = ctx.p().fecha("fecha_desde");
        LocalDate hasta = ctx.p().fecha("fecha_hasta");
        if (desde == null) {
            desde = ctx.hoy();
            hasta = desde.plusDays(6);
        } else if (hasta == null) {
            hasta = desde;
        }
        if (hasta.isBefore(desde)) {
            throw ErrorNegocio.de("período inválido: la fecha final es anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= MAXIMO_DIAS_GRILLA) {
            throw ErrorNegocio.de("pida hasta " + MAXIMO_DIAS_GRILLA + " días de grilla por correo");
        }
        List<Fila> filas = ctx.consultar("""
                SELECT f.fecha::date AS "Fecha", g.dia AS "Día",
                       to_char(g.hora_inicio, 'HH24:MI') || '–' || to_char(g.hora_fin, 'HH24:MI') AS "Horario",
                       g.programa AS "Programa", g.tipo AS "Tipo", g.grilla_id AS "Id grilla",
                       COALESCE(e.estado || ' (escaleta ' || e.id || ')', CASE WHEN g.tipo = 'otro' THEN '' ELSE 'sin escaleta' END) AS "Escaleta"
                FROM generate_series(CAST(? AS date), CAST(? AS date), INTERVAL '1 day') AS f(fecha)
                JOIN v_grilla_semanal g ON g.dia_semana = EXTRACT(ISODOW FROM f.fecha)
                                       AND f.fecha::date >= g.vigente_desde
                                       AND (g.vigente_hasta IS NULL OR f.fecha::date <= g.vigente_hasta)
                LEFT JOIN escaleta e ON e.grilla_id = g.grilla_id AND e.fecha = f.fecha::date
                ORDER BY f.fecha, g.hora_inicio
                """, desde, hasta);
        return Resultado.listado("Grilla del " + desde + (hasta.equals(desde) ? "" : " al " + hasta), filas,
                "No hay programas en la grilla para esas fechas.");
    }

    private static final String HORARIO = """
            SELECT g.id AS "Id", p.nombre AS "Programa",
                   (ARRAY['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'])[g.dia_semana] AS "Día",
                   to_char(g.hora_inicio, 'HH24:MI') || '–' || to_char(g.hora_fin, 'HH24:MI') AS "Horario",
                   g.vigente_desde AS "Vigente desde", g.vigente_hasta AS "Vigente hasta", g.estado AS "Estado"
            FROM grilla g JOIN programa p ON p.id = g.programa_id
            WHERE g.id = ?
            """;

    private static Resultado insgri(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        Fila prg = ctx.uno("SELECT estado FROM programa WHERE id = ?", p.entero("id_programa"))
                .orElseThrow(() -> ErrorNegocio.de("el programa indicado no existe"));
        if (!"activo".equals(prg.texto("estado"))) {
            throw ErrorNegocio.de("el programa está inactivo");
        }
        LocalDate desde = p.tiene("vigente_desde") ? p.fecha("vigente_desde") : ctx.hoy();
        LocalDate hasta = p.fecha("vigente_hasta");
        String inicio = p.texto("hora_inicio");
        String fin = p.texto("hora_fin");
        validarHorario(inicio, fin, desde, hasta);
        sinChoque(ctx, 0, p.entero("dia_semana"), inicio, fin, desde, hasta);
        int id = ctx.devolviendo("""
                INSERT INTO grilla (programa_id, dia_semana, hora_inicio, hora_fin, vigente_desde, vigente_hasta)
                VALUES (?, ?, CAST(? AS time), CAST(? AS time), ?, ?) RETURNING id
                """, p.entero("id_programa"), p.entero("dia_semana"), inicio, fin, desde, hasta).entero("id");
        return Resultado.exito("Grilla", "horario agregado").ficha(ctx.requerir(HORARIO, id));
    }

    private static Resultado updgri(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        p.requerirAlguno("hora_inicio", "hora_fin", "vigente_hasta");
        int id = p.entero("id");
        Fila g = ctx.requerir("""
                SELECT dia_semana, to_char(hora_inicio, 'HH24:MI:SS') AS inicio, to_char(hora_fin, 'HH24:MI:SS') AS fin,
                       vigente_desde, vigente_hasta
                FROM grilla WHERE id = ?
                """, id);
        String inicio = p.tiene("hora_inicio") ? p.texto("hora_inicio") : g.texto("inicio");
        String fin = p.tiene("hora_fin") ? p.texto("hora_fin") : g.texto("fin");
        LocalDate hasta = p.tiene("vigente_hasta") ? p.fecha("vigente_hasta") : g.fecha("vigente_hasta");
        validarHorario(inicio, fin, g.fecha("vigente_desde"), hasta);
        sinChoque(ctx, id, g.entero("dia_semana"), inicio, fin, g.fecha("vigente_desde"), hasta);
        ctx.ejecutar("""
                UPDATE grilla SET hora_inicio = CAST(? AS time), hora_fin = CAST(? AS time), vigente_hasta = ?,
                                  actualizado_en = CURRENT_TIMESTAMP
                WHERE id = ?
                """, inicio, fin, hasta, id);
        return Resultado.exito("Grilla", "horario modificado").ficha(ctx.requerir(HORARIO, id));
    }

    private static Resultado delgri(Contexto ctx) throws SQLException, ErrorNegocio {
        int id = ctx.p().entero("id");
        Fila antes = ctx.requerir(HORARIO, id);
        if (ctx.existe("SELECT 1 FROM escaleta WHERE grilla_id = ?", id)) {
            ctx.ejecutar("UPDATE grilla SET estado = 'inactiva', actualizado_en = CURRENT_TIMESTAMP WHERE id = ?", id);
            return Resultado.exito("Grilla", "horario quitado de la grilla").ficha(ctx.requerir(HORARIO, id))
                    .parrafo("El horario tenía escaletas: queda inactivo para conservar su historial.");
        }
        TablaCrud.eliminar(ctx.repositorio(), ctx.conexion(), "grilla", id);
        return Resultado.exito("Grilla", "horario quitado de la grilla").ficha(antes);
    }

    /** R16 y vigencia: la hora de fin es posterior (hasta 24:00) y la vigencia no termina antes de empezar. */
    private static void validarHorario(String inicio, String fin, LocalDate desde, LocalDate hasta) throws ErrorNegocio {
        if (inicio.equals("24:00:00")) {
            throw ErrorNegocio.de("un horario no puede empezar a las 24:00; use 00:00 del día siguiente");
        }
        if (fin.compareTo(inicio) <= 0) {
            throw ErrorNegocio.de("la hora de fin debe ser posterior a la de inicio: un horario no cruza la medianoche "
                    + "(un programa de 23:30 a 00:30 se registra como 23:30–24:00 y 00:00–00:30 del día siguiente)");
        }
        if (hasta != null && hasta.isBefore(desde)) {
            throw ErrorNegocio.de("la vigencia termina antes de empezar");
        }
    }

    /** R17: otro horario vigente del mismo día que se cruza en horas y en vigencia. */
    private static void sinChoque(Contexto ctx, int excepto, int dia, String inicio, String fin, LocalDate desde, LocalDate hasta)
            throws SQLException, ErrorNegocio {
        Optional<Fila> choque = ctx.uno("""
                SELECT g.programa, to_char(g.hora_inicio, 'HH24:MI') || '–' || to_char(g.hora_fin, 'HH24:MI') AS horario
                FROM v_grilla_semanal g
                WHERE g.dia_semana = ? AND g.grilla_id <> ?
                  AND CAST(? AS time) < g.hora_fin AND g.hora_inicio < CAST(? AS time)
                  AND g.vigente_desde <= COALESCE(CAST(? AS date), 'infinity'::date)
                  AND COALESCE(g.vigente_hasta, 'infinity'::date) >= CAST(? AS date)
                ORDER BY g.hora_inicio LIMIT 1
                """, dia, excepto, inicio, fin, hasta, desde);
        if (choque.isPresent()) {
            throw ErrorNegocio.de("choca con " + choque.get().texto("programa") + " " + choque.get().texto("horario"));  // A1
        }
    }

    // =================================================================== plantillas (R18, R19)

    private static final String PLANTILLAS = """
            SELECT pl.id AS "Id", pl.nombre AS "Plantilla", p.nombre AS "Programa", %s AS "Duración",
                   pl.cantidad_bloques AS "Bloques", pl.palabras_por_minuto AS "Ritmo (palabras/min)",
                   pc.cortes AS "Cortes", pc.observacion AS "Control (R18)", pl.estado AS "Estado"
            FROM plantilla pl
            JOIN programa p ON p.id = pl.programa_id
            JOIN v_plantilla_control pc ON pc.plantilla_id = pl.id
            """.formatted(Formato.mmss("pl.duracion_total"));

    private static Resultado lispla(Contexto ctx) throws SQLException, ErrorNegocio {
        Integer id = ctx.p().entero("id");
        if (id == null) {
            return Resultado.listado("Plantillas", ctx.consultar(PLANTILLAS + " ORDER BY pl.id"), "No hay plantillas registradas.")
                    .parrafo("Para ver los bloques y el tiempo neto de una: LISPLA[\"id\"].");
        }
        Fila pl = ctx.requerir(PLANTILLAS + " WHERE pl.id = ?", id);
        Resultado r = Resultado.informacion("Plantilla " + pl.texto("Plantilla"), "Plantilla " + id + ": «" + pl.texto("Plantilla") + "».")
                .ficha(pl);
        r.subtitulo("Bloques").tabla(ctx.consultar("""
                SELECT b.bloque AS "Bloque", %s AS "Empieza", %s AS "Termina", %s AS "Tiempo disponible"
                FROM v_plantilla_bloque b WHERE b.plantilla_id = ? ORDER BY b.numero_bloque
                """.formatted(Formato.mmss("b.inicio_bloque"), Formato.mmss("b.fin_bloque"), Formato.mmss("b.tiempo_disponible")), id));
        List<Fila> cortes = ctx.consultar(CORTES + " WHERE c.plantilla_id = ? ORDER BY c.numero", id);
        if (!cortes.isEmpty()) {
            r.subtitulo("Cortes comerciales").tabla(cortes);
        }
        r.ficha(ctx.requerir("""
                SELECT %s AS "Duración total", %s AS "Tiempo comercial", %s AS "Tiempo neto"
                FROM plantilla pl LEFT JOIN plantilla_corte c ON c.plantilla_id = pl.id
                WHERE pl.id = ? GROUP BY pl.id
                """.formatted(Formato.mmss("pl.duracion_total"), Formato.mmss("COALESCE(SUM(c.duracion), 0)::int"),
                Formato.mmss("(pl.duracion_total - COALESCE(SUM(c.duracion), 0))::int")), id));
        return r;
    }

    private static Resultado inspla(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        Fila prg = ctx.uno("SELECT tipo, estado FROM programa WHERE id = ?", p.entero("id_programa"))
                .orElseThrow(() -> ErrorNegocio.de("el programa indicado no existe"));
        if ("otro".equals(prg.texto("tipo"))) {
            throw ErrorNegocio.de("solo los noticieros y los especiales tienen plantilla y escaleta");
        }
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("programa_id", p.entero("id_programa"));
        v.put("nombre", p.texto("nombre"));
        v.put("duracion_total", p.entero("duracion_min") * 60);
        v.put("cantidad_bloques", p.entero("cantidad_bloques"));
        v.put("palabras_por_minuto", p.tiene("palabras_por_minuto") ? p.entero("palabras_por_minuto") : 150);
        int id = TablaCrud.insertar(ctx.repositorio(), ctx.conexion(), "plantilla", v);
        return conControl(ctx, Resultado.exito("Plantillas", "plantilla registrada"), id);
    }

    private static Resultado updpla(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        p.requerirAlguno("nombre", "duracion_min", "cantidad_bloques", "palabras_por_minuto");
        int id = p.entero("id");
        ctx.requerir("SELECT 1 FROM plantilla WHERE id = ?", id);
        if (p.tiene("duracion_min") || p.tiene("cantidad_bloques")) {
            sinEscaletasEnBorrador(ctx, id);
        }
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("nombre", p.texto("nombre"));
        v.put("duracion_total", p.tiene("duracion_min") ? p.entero("duracion_min") * 60 : null);
        v.put("cantidad_bloques", p.entero("cantidad_bloques"));
        v.put("palabras_por_minuto", p.entero("palabras_por_minuto"));
        TablaCrud.actualizar(ctx.repositorio(), ctx.conexion(), "plantilla", id, v);
        return conControl(ctx, Resultado.exito("Plantillas", "plantilla modificada"), id);
    }

    private static Resultado delpla(Contexto ctx) throws SQLException, ErrorNegocio {
        int id = ctx.p().entero("id");
        Fila pl = ctx.requerir("SELECT estado FROM plantilla WHERE id = ?", id);
        if ("inactiva".equals(pl.texto("estado"))) {
            throw ErrorNegocio.de("la plantilla ya está inactiva");
        }
        ctx.ejecutar("UPDATE plantilla SET estado = 'inactiva', actualizado_en = CURRENT_TIMESTAMP WHERE id = ?", id);
        return Resultado.exito("Plantillas", "plantilla desactivada").ficha(ctx.requerir(PLANTILLAS + " WHERE pl.id = ?", id))
                .parrafo("Las escaletas que ya la usan no cambian; las nuevas no la pueden usar.");
    }

    /** La ficha con el control R18: si no está lista, dice qué le falta. */
    private static Resultado conControl(Contexto ctx, Resultado r, int idPlantilla) throws SQLException, ErrorNegocio {
        Fila pl = ctx.requerir(PLANTILLAS + " WHERE pl.id = ?", idPlantilla);
        r.ficha(pl);
        if (!"ok".equals(pl.texto("Control (R18)"))) {
            r.aviso("La plantilla todavía no está lista: " + pl.texto("Control (R18)")
                    + ". Agregue los cortes con INSCOR[\"" + idPlantilla + "\",\"numero\",\"minuto_inicio\",\"duracion\"].");
        } else {
            r.parrafo("La plantilla está lista para generar escaletas (R18).");
        }
        return r;
    }

    // =================================================================== cortes

    private static final String CORTES = """
            SELECT c.id AS "Id", c.plantilla_id AS "Plantilla", c.numero AS "Número", 'cierra el bloque ' || CHR(64 + c.numero) AS "Ubicación",
                   'minuto ' || c.minuto_inicio AS "Empieza", %s AS "Duración"
            FROM plantilla_corte c
            """.formatted(Formato.mmss("c.duracion"));

    private static Resultado liscor(Contexto ctx) throws SQLException, ErrorNegocio {
        Integer id = ctx.p().entero("id_plantilla");
        if (id == null) {
            return Resultado.listado("Cortes comerciales", ctx.consultar(CORTES + " ORDER BY c.plantilla_id, c.numero"), "No hay cortes registrados.");
        }
        Fila pl = ctx.requerir("SELECT nombre FROM plantilla WHERE id = ?", id);
        return Resultado.listado("Cortes de «" + pl.texto("nombre") + "»", ctx.consultar(CORTES + " WHERE c.plantilla_id = ? ORDER BY c.numero", id),
                "La plantilla no tiene cortes.");
    }

    private static Resultado inscor(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        int idPlantilla = p.entero("id_plantilla");
        Fila pl = ctx.requerir("SELECT cantidad_bloques, duracion_total FROM plantilla WHERE id = ?", idPlantilla);
        sinEscaletasEnBorrador(ctx, idPlantilla);
        int numero = p.entero("numero");
        if (numero > pl.entero("cantidad_bloques") - 1) {
            throw ErrorNegocio.de("la plantilla tiene " + pl.entero("cantidad_bloques") + " bloque(s): sus cortes van del 1 al "
                    + (pl.entero("cantidad_bloques") - 1) + " (un corte menos que bloques, R18)");
        }
        dentroDelPrograma(p.entero("minuto_inicio"), p.entero("duracion"), pl.entero("duracion_total"));
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("plantilla_id", idPlantilla);
        v.put("numero", numero);
        v.put("minuto_inicio", p.entero("minuto_inicio"));
        v.put("duracion", p.entero("duracion"));
        int id = TablaCrud.insertar(ctx.repositorio(), ctx.conexion(), "plantilla_corte", v);
        Resultado r = Resultado.exito("Cortes", "corte agregado").ficha(ctx.requerir(CORTES + " WHERE c.id = ?", id));
        return conControl(ctx, r, idPlantilla);
    }

    private static Resultado updcor(Contexto ctx) throws SQLException, ErrorNegocio {
        Parametros p = ctx.p();
        p.requerirAlguno("minuto_inicio", "duracion");
        int id = p.entero("id");
        Fila c = ctx.requerir("""
                SELECT c.plantilla_id, c.minuto_inicio, c.duracion, pl.duracion_total
                FROM plantilla_corte c JOIN plantilla pl ON pl.id = c.plantilla_id WHERE c.id = ?
                """, id);
        sinEscaletasEnBorrador(ctx, c.entero("plantilla_id"));
        int minuto = p.tiene("minuto_inicio") ? p.entero("minuto_inicio") : c.entero("minuto_inicio");
        int duracion = p.tiene("duracion") ? p.entero("duracion") : c.entero("duracion");
        dentroDelPrograma(minuto, duracion, c.entero("duracion_total"));
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("minuto_inicio", p.entero("minuto_inicio"));
        v.put("duracion", p.entero("duracion"));
        TablaCrud.actualizar(ctx.repositorio(), ctx.conexion(), "plantilla_corte", id, v);
        Resultado r = Resultado.exito("Cortes", "corte modificado").ficha(ctx.requerir(CORTES + " WHERE c.id = ?", id));
        return conControl(ctx, r, c.entero("plantilla_id"));
    }

    private static Resultado delcor(Contexto ctx) throws SQLException, ErrorNegocio {
        int id = ctx.p().entero("id");
        Fila c = ctx.requerir(CORTES + " WHERE c.id = ?", id);
        sinEscaletasEnBorrador(ctx, c.entero("Plantilla"));
        TablaCrud.eliminar(ctx.repositorio(), ctx.conexion(), "plantilla_corte", id);
        return conControl(ctx, Resultado.exito("Cortes", "corte eliminado"), c.entero("Plantilla"));
    }

    private static void dentroDelPrograma(int minutoInicio, int duracion, int duracionTotal) throws ErrorNegocio {
        if (minutoInicio * 60 + duracion > duracionTotal) {
            throw ErrorNegocio.de("el corte termina después del final del programa (" + Formato.duracion(duracionTotal) + ")");
        }
    }

    /** R19: los cortes y la estructura no cambian mientras haya escaletas en borrador con esta plantilla. */
    private static void sinEscaletasEnBorrador(Contexto ctx, int idPlantilla) throws SQLException, ErrorNegocio {
        Optional<Fila> esc = ctx.uno("SELECT id, fecha FROM escaleta WHERE plantilla_id = ? AND estado = 'borrador' ORDER BY fecha LIMIT 1",
                idPlantilla);
        if (esc.isPresent()) {
            throw ErrorNegocio.de("la plantilla tiene escaletas en borrador (la " + esc.get().entero("id") + " del " + esc.get().fecha("fecha")
                    + "): no se cambian sus cortes ni sus bloques hasta cerrarlas o eliminarlas (R19)");
        }
    }
}
