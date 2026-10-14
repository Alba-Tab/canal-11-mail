package bo.edu.uagrm.ficct.canal11.negocio.cu05;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-05 Gestión de Redactar Notas (NRCS), Gestionar Material Audiovisual y Flujo de Aprobación
 * (Tabla 83 del Anexo A).
 */
public final class ComandosNotas {

    private static final String CU = "CU-05";
    static final Tipo TIPO_PIEZA = Tipo.opcion("lectura", "vo", "vo_sot", "nota_completa", "enlace_vivo", "entrevista_estudio");
    static final Tipo ESTADO_NOTA = Tipo.opcion("borrador", "en_revision", "observada", "aprobada", "emitida", "baja");
    static final Tipo TIPO_MATERIAL = Tipo.opcion("video", "sot", "grafico", "foto", "audio");
    static final Tipo ESTADO_MATERIAL = Tipo.opcion("pendiente", "ingestado", "en_edicion", "listo");
    static final Tipo RITMO = Tipo.entero(80, 250);

    private ComandosNotas() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISNOT", "Listar notas")
                        .opcional("fecha", Tipo.FILTRO).omitible("estado", ESTADO_NOTA)
                        .ejemplo("LISNOT[\"2026-10-12\",\"aprobada\"]").build(),
                CommandSpec.de(CU, "INSNOT", "Redactar nota (el guion va en el cuerpo del correo)")
                        .param("slug", Tipo.texto(40)).param("titulo", Tipo.texto(200)).param("tipo_pieza", TIPO_PIEZA)
                        .param("fecha", Tipo.FECHA).opcional("id_cobertura", Tipo.ID).opcional("palabras_por_minuto", RITMO)
                        .guionEnCuerpo()
                        .ejemplo("INSNOT[\"PARO-TRANSPORTE\",\"Transportistas bloquean el 4to anillo\",\"vo_sot\",\"2026-10-12\",\"2\",\"\"]").build(),
                CommandSpec.de(CU, "UPDNOT", "Modificar datos de la nota")
                        .param("id", Tipo.ID).opcional("slug", Tipo.texto(40)).opcional("titulo", Tipo.texto(200))
                        .opcional("tipo_pieza", TIPO_PIEZA).opcional("fecha", Tipo.FECHA).opcional("id_cobertura", Tipo.ID)
                        .opcional("palabras_por_minuto", RITMO)
                        .ejemplo("UPDNOT[\"4\",\"INCENDIOS-CHIQUITANIA\",\"COED reporta 14 focos de calor\",\"lectura\",\"2026-10-12\",\"6\",\"130\"]").build(),
                CommandSpec.de(CU, "DELNOT", "Dar de baja la nota")
                        .param("id", Tipo.ID)
                        .ejemplo("DELNOT[\"5\"]").build(),
                CommandSpec.de(CU, "GUINOT", "Reemplazar el guion (cuerpo del correo; crea una versión)")
                        .param("id", Tipo.ID)
                        .guionEnCuerpo()
                        .ejemplo("GUINOT[\"1\"]").build(),
                CommandSpec.de(CU, "LISVER", "Versiones del guion")
                        .param("id_nota", Tipo.ID)
                        .ejemplo("LISVER[\"6\"]").build(),
                CommandSpec.de(CU, "ENVNOT", "Enviar la nota a revisión")
                        .param("id", Tipo.ID)
                        .ejemplo("ENVNOT[\"5\"]").build(),
                CommandSpec.de(CU, "APRNOT", "Aprobar la nota")
                        .param("id", Tipo.ID)
                        .ejemplo("APRNOT[\"3\"]").build(),
                CommandSpec.de(CU, "OBSNOT", "Observar la nota con motivo")
                        .param("id", Tipo.ID).opcional("motivo", Tipo.texto(255))
                        .ejemplo("OBSNOT[\"6\",\"Falta la versión de la Alcaldía\"]").build(),
                CommandSpec.de(CU, "LISREV", "Historial de revisión")
                        .param("id_nota", Tipo.ID)
                        .ejemplo("LISREV[\"10\"]").build(),
                CommandSpec.de(CU, "LISROT", "Rótulos de la nota")
                        .param("id_nota", Tipo.ID)
                        .ejemplo("LISROT[\"2\"]").build(),
                CommandSpec.de(CU, "INSROT", "Agregar rótulo")
                        .param("id_nota", Tipo.ID).opcional("orden", Tipo.entero(1, 99)).param("nombre", Tipo.texto(100))
                        .opcional("cargo", Tipo.texto(150))
                        .ejemplo("INSROT[\"2\",\"3\",\"Juan Pérez\",\"Vecino del 4to anillo\"]").build(),
                CommandSpec.de(CU, "UPDROT", "Modificar rótulo")
                        .param("id", Tipo.ID).opcional("orden", Tipo.entero(1, 99)).opcional("nombre", Tipo.texto(100))
                        .opcional("cargo", Tipo.texto(150))
                        .ejemplo("UPDROT[\"3\",\"2\",\"María Eugenia Suárez\",\"Pasajera\"]").build(),
                CommandSpec.de(CU, "DELROT", "Eliminar rótulo")
                        .param("id", Tipo.ID)
                        .ejemplo("DELROT[\"3\"]").build(),
                CommandSpec.de(CU, "LISMAT", "Material audiovisual de una nota")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISMAT[\"pendiente\"]").build(),
                CommandSpec.de(CU, "INSMAT", "Registrar material")
                        .param("id_nota", Tipo.ID).param("descripcion", Tipo.texto(200)).param("tipo", TIPO_MATERIAL)
                        .opcional("duracion", Tipo.DURACION).opcional("ubicacion", Tipo.texto(255))
                        .ejemplo("INSMAT[\"3\",\"Imágenes del acto\",\"video\",\"00:40\",\"/media/2026/autoridades.mxf\"]").build(),
                CommandSpec.de(CU, "UPDMAT", "Actualizar estado y duración del material")
                        .param("id", Tipo.ID).opcional("estado", ESTADO_MATERIAL).opcional("duracion", Tipo.DURACION)
                        .opcional("ubicacion", Tipo.texto(255))
                        .ejemplo("UPDMAT[\"2\",\"listo\",\"00:55\",\"/media/2026/paro_final.mxf\"]").build(),
                CommandSpec.de(CU, "DELMAT", "Eliminar material")
                        .param("id", Tipo.ID)
                        .ejemplo("DELMAT[\"4\"]").build());
    }
}
