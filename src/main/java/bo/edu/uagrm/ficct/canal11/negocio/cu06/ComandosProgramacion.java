package bo.edu.uagrm.ficct.canal11.negocio.cu06;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-06, primera parte: programas, grilla, plantillas y cortes comerciales (Tabla 84 del Anexo A).
 */
public final class ComandosProgramacion {

    private static final String CU = "CU-06";
    static final Tipo TIPO_PROGRAMA = Tipo.opcion("noticiero", "especial", "otro");
    static final Tipo RITMO = Tipo.entero(80, 250);

    private ComandosProgramacion() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISPRG", "Listar programas")
                        .param("tipo", Tipo.FILTRO)
                        .ejemplo("LISPRG[\"noticiero\"]").build(),
                CommandSpec.de(CU, "INSPRG", "Registrar programa")
                        .param("nombre", Tipo.texto(100)).param("tipo", TIPO_PROGRAMA).opcional("descripcion", Tipo.texto(255))
                        .ejemplo("INSPRG[\"Noticiero Mediodía\",\"noticiero\",\"Edición de las 13:00\"]").build(),
                CommandSpec.de(CU, "UPDPRG", "Modificar programa")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(100)).opcional("tipo", TIPO_PROGRAMA)
                        .opcional("descripcion", Tipo.texto(255)).omitible("estado", Tipo.opcion("activo", "inactivo"))
                        .ejemplo("UPDPRG[\"5\",\"Noticiero Mediodía\",\"noticiero\",\"Edición de las 13:00 a 13:30\"]").build(),
                CommandSpec.de(CU, "DELPRG", "Desactivar programa")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPRG[\"4\"]").build(),
                CommandSpec.de(CU, "LISGRI", "Ver la grilla de un rango de fechas")
                        .param("fecha_desde", Tipo.FECHA_O_TODOS).omitible("fecha_hasta", Tipo.FECHA)
                        .ejemplo("LISGRI[\"2026-10-12\",\"2026-10-18\"]").build(),
                CommandSpec.de(CU, "INSGRI", "Agregar horario a la grilla (valida choques)")
                        .param("id_programa", Tipo.ID).param("dia_semana", Tipo.entero(1, 7)).param("hora_inicio", Tipo.HORA_GRILLA)
                        .param("hora_fin", Tipo.HORA_GRILLA).opcional("vigente_desde", Tipo.FECHA).opcional("vigente_hasta", Tipo.FECHA)
                        .ejemplo("INSGRI[\"5\",\"1\",\"13:00\",\"13:30\",\"2026-10-19\",\"\"]").build(),
                CommandSpec.de(CU, "UPDGRI", "Modificar horario de la grilla")
                        .param("id", Tipo.ID).opcional("hora_inicio", Tipo.HORA_GRILLA).opcional("hora_fin", Tipo.HORA_GRILLA)
                        .opcional("vigente_hasta", Tipo.FECHA)
                        .ejemplo("UPDGRI[\"8\",\"06:30\",\"07:30\",\"2026-12-31\"]").build(),
                CommandSpec.de(CU, "DELGRI", "Quitar horario de la grilla")
                        .param("id", Tipo.ID)
                        .ejemplo("DELGRI[\"13\"]").build(),
                CommandSpec.de(CU, "LISPLA", "Ver plantilla, bloques y tiempo neto")
                        .param("id", Tipo.ID_O_TODOS)
                        .ejemplo("LISPLA[\"1\"]").build(),
                CommandSpec.de(CU, "INSPLA", "Registrar plantilla")
                        .param("id_programa", Tipo.ID).param("nombre", Tipo.texto(100)).param("duracion_min", Tipo.entero(1, 600))
                        .param("cantidad_bloques", Tipo.entero(1, 10)).opcional("palabras_por_minuto", RITMO)
                        .ejemplo("INSPLA[\"5\",\"Noticiero Mediodía\",\"30\",\"2\",\"155\"]").build(),
                CommandSpec.de(CU, "UPDPLA", "Modificar plantilla")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(100)).opcional("duracion_min", Tipo.entero(1, 600))
                        .opcional("cantidad_bloques", Tipo.entero(1, 10)).opcional("palabras_por_minuto", RITMO)
                        .ejemplo("UPDPLA[\"2\",\"Noticiero Matinal\",\"60\",\"3\",\"165\"]").build(),
                CommandSpec.de(CU, "DELPLA", "Desactivar plantilla")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPLA[\"3\"]").build(),
                CommandSpec.de(CU, "LISCOR", "Cortes comerciales de una plantilla")
                        .param("id_plantilla", Tipo.ID_O_TODOS)
                        .ejemplo("LISCOR[\"1\"]").build(),
                CommandSpec.de(CU, "INSCOR", "Agregar corte comercial")
                        .param("id_plantilla", Tipo.ID).param("numero", Tipo.entero(1, 9)).param("minuto_inicio", Tipo.entero(1, 600))
                        .param("duracion", Tipo.DURACION)
                        .ejemplo("INSCOR[\"4\",\"1\",\"15\",\"02:00\"]").build(),
                CommandSpec.de(CU, "UPDCOR", "Modificar corte comercial")
                        .param("id", Tipo.ID).opcional("minuto_inicio", Tipo.entero(1, 600)).opcional("duracion", Tipo.DURACION)
                        .ejemplo("UPDCOR[\"2\",\"29\",\"03:00\"]").build(),
                CommandSpec.de(CU, "DELCOR", "Eliminar corte comercial")
                        .param("id", Tipo.ID)
                        .ejemplo("DELCOR[\"7\"]").build());
    }
}
