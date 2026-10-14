package bo.edu.uagrm.ficct.canal11.negocio.cu06;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-06, segunda parte: la escaleta del noticiero y sus ítems, con control de tiempos (Tabla 84 del
 * Anexo A).
 */
public final class ComandosEscaleta {

    private static final String CU = "CU-06";
    static final Tipo TIPO_ITEM = Tipo.opcion("apertura", "titulares", "nota", "enlace_vivo", "entrevista", "corte", "despedida", "otro");

    private ComandosEscaleta() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISESC", "Escaleta con tiempos y alertas por bloque")
                        .param("id_escaleta", Tipo.ID_O_TODOS)
                        .ejemplo("LISESC[\"1\"]").build(),
                CommandSpec.de(CU, "INSESC", "Generar la escaleta del día y copiar los cortes de la plantilla")
                        .param("id_grilla", Tipo.ID).param("fecha", Tipo.FECHA).param("id_plantilla", Tipo.ID)
                        .ejemplo("INSESC[\"1\",\"2026-10-12\",\"1\"]").build(),
                CommandSpec.de(CU, "UPDESC", "Cerrar, reabrir o registrar como emitida")
                        .param("id", Tipo.ID).param("estado", Tipo.opcion("borrador", "cerrada", "emitida"))
                        .opcional("hora_inicio_real", Tipo.HORA).opcional("duracion_real", Tipo.DURACION)
                        .ejemplo("UPDESC[\"2\",\"emitida\",\"20:00:20\",\"60:42\"]").build(),
                CommandSpec.de(CU, "DELESC", "Eliminar escaleta en borrador")
                        .param("id", Tipo.ID)
                        .ejemplo("DELESC[\"3\"]").build(),
                CommandSpec.de(CU, "LISITE", "Ítems de la escaleta (incluye flotados y bajas)")
                        .param("id_escaleta", Tipo.ID)
                        .ejemplo("LISITE[\"1\"]").build(),
                CommandSpec.de(CU, "INSITE", "Agregar ítem al final del bloque (solo notas aprobadas; no acepta cortes)")
                        .param("id_escaleta", Tipo.ID).param("bloque", Tipo.BLOQUE).param("tipo", TIPO_ITEM)
                        .param("id_nota o titulo", Tipo.texto(200)).opcional("id_presentador", Tipo.ID)
                        .opcional("duracion", Tipo.DURACION)
                        .ejemplo("INSITE[\"1\",\"B\",\"enlace_vivo\",\"Enlace: paro de transporte\",\"10\",\"02:00\"]").build(),
                CommandSpec.de(CU, "UPDITE", "Reordenar o ajustar un ítem")
                        .param("id", Tipo.ID).opcional("orden", Tipo.entero(1, 998)).opcional("bloque", Tipo.BLOQUE)
                        .opcional("id_presentador", Tipo.ID).opcional("duracion", Tipo.DURACION).opcional("duracion_real", Tipo.DURACION)
                        .omitible("estado", Tipo.opcion("programado"))
                        .ejemplo("UPDITE[\"9\",\"3\",\"B\",\"10\",\"02:30\",\"\"]").build(),
                CommandSpec.de(CU, "DELITE", "Dar de baja un ítem")
                        .param("id", Tipo.ID)
                        .ejemplo("DELITE[\"9\"]").build(),
                CommandSpec.de(CU, "FLOITE", "Flotar un ítem (sale del aire, queda disponible)")
                        .param("id", Tipo.ID)
                        .ejemplo("FLOITE[\"12\"]").build(),
                CommandSpec.de(CU, "EXPESC", "Exportar la escaleta (adjunto HTML)")
                        .param("id_escaleta", Tipo.ID)
                        .ejemplo("EXPESC[\"1\"]").build());
    }
}
