package bo.edu.uagrm.ficct.canal11.negocio.cu03;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-03 Gestión de Inventario, Asignación y Mantenimiento de Equipos Técnicos y Unidades Móviles
 * (Tabla 81 del Anexo A).
 */
public final class ComandosRecursos {

    private static final String CU = "CU-03";

    private ComandosRecursos() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISTRE", "Listar tipos de recurso")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISTRE[\"*\"]").build(),
                CommandSpec.de(CU, "INSTRE", "Registrar tipo de recurso")
                        .param("nombre", Tipo.texto(50)).opcional("es_unidad_movil", Tipo.BOOLEANO).opcional("descripcion", Tipo.texto(150))
                        .ejemplo("INSTRE[\"Drone\",\"false\",\"Captura aérea\"]").build(),
                CommandSpec.de(CU, "UPDTRE", "Modificar tipo de recurso")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(50)).opcional("es_unidad_movil", Tipo.BOOLEANO)
                        .opcional("descripcion", Tipo.texto(150))
                        .ejemplo("UPDTRE[\"7\",\"Drone\",\"false\",\"Captura aérea 4K\"]").build(),
                CommandSpec.de(CU, "DELTRE", "Eliminar tipo de recurso sin equipos")
                        .param("id", Tipo.ID)
                        .ejemplo("DELTRE[\"7\"]").build(),
                CommandSpec.de(CU, "LISREC", "Listar recursos")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISREC[\"operativo\"]").build(),
                CommandSpec.de(CU, "INSREC", "Registrar recurso")
                        .param("codigo", Tipo.CODIGO_RECURSO).param("id_tipo", Tipo.ID).param("marca", Tipo.texto(50))
                        .param("modelo", Tipo.texto(80)).opcional("placa", Tipo.texto(15)).opcional("caracteristicas", Tipo.texto(255))
                        .ejemplo("INSREC[\"CAM-06\",\"1\",\"Sony\",\"FX6\",\"\",\"Full frame 4K\"]").build(),
                CommandSpec.de(CU, "UPDREC", "Modificar recurso")
                        .param("id", Tipo.ID).opcional("marca", Tipo.texto(50)).opcional("modelo", Tipo.texto(80))
                        .opcional("placa", Tipo.texto(15)).opcional("caracteristicas", Tipo.texto(255)).opcional("observacion", Tipo.texto(255))
                        .ejemplo("UPDREC[\"5\",\"Canon\",\"XF605\",\"\",\"4K liviana\",\"\"]").build(),
                CommandSpec.de(CU, "DELREC", "Dar de baja un recurso")
                        .param("id", Tipo.ID).param("motivo", Tipo.texto(255))
                        .ejemplo("DELREC[\"4\",\"Visor irreparable\"]").build(),
                CommandSpec.de(CU, "INCREC", "Reportar daño o incidencia (pasa a mantenimiento)")
                        .param("codigo", Tipo.CODIGO_RECURSO).param("descripcion", Tipo.texto(255))
                        .ejemplo("INCREC[\"MOV-01\",\"Falla de frenos\"]").build(),
                CommandSpec.de(CU, "ALTREC", "Dar de alta tras la reparación")
                        .param("codigo", Tipo.CODIGO_RECURSO).opcional("descripcion", Tipo.texto(255))
                        .ejemplo("ALTREC[\"CAM-04\",\"Visor reemplazado\"]").build(),
                CommandSpec.de(CU, "LISMAN", "Historial de mantenimiento")
                        .param("codigo", Tipo.FILTRO)
                        .ejemplo("LISMAN[\"CAM-04\"]").build(),
                CommandSpec.de(CU, "LISPRE", "Listar préstamos")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISPRE[\"abiertos\"]").build(),
                CommandSpec.de(CU, "INSPRE", "Entregar recurso (préstamo)")
                        .param("codigo", Tipo.CODIGO_RECURSO).param("id_responsable", Tipo.ID).opcional("id_cobertura", Tipo.ID)
                        .opcional("observacion", Tipo.texto(255))
                        .ejemplo("INSPRE[\"CAM-01\",\"6\",\"3\",\"Con batería extra\"]").build(),
                CommandSpec.de(CU, "UPDPRE", "Corregir préstamo")
                        .param("id", Tipo.ID).opcional("id_responsable", Tipo.ID).opcional("observacion", Tipo.texto(255))
                        .ejemplo("UPDPRE[\"8\",\"5\",\"Cambio de responsable\"]").build(),
                CommandSpec.de(CU, "DEVPRE", "Registrar devolución")
                        .param("id", Tipo.ID).opcional("observacion", Tipo.texto(255))
                        .ejemplo("DEVPRE[\"8\",\"Sin novedad\"]").build(),
                CommandSpec.de(CU, "DELPRE", "Anular préstamo registrado por error")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPRE[\"8\"]").build());
    }
}
