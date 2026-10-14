package bo.edu.uagrm.ficct.canal11.negocio.cu02;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-02 Gestión de Personal de Prensa, Turnos y Disponibilidad (Tabla 80 del Anexo A).
 */
public final class ComandosPersonal {

    private static final String CU = "CU-02";
    static final Tipo CARGO = Tipo.opcion("periodista", "camarografo", "editor", "presentador", "conductor", "jefe_prensa");
    static final Tipo CONDICION = Tipo.opcion("planta", "contrato", "practicante");
    static final Tipo TIPO_AUSENCIA = Tipo.opcion("permiso", "vacacion", "baja_medica", "comision");

    private ComandosPersonal() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISPER", "Listar personal")
                        .param("cargo", Tipo.FILTRO)
                        .ejemplo("LISPER[\"camarografo\"]").build(),
                CommandSpec.de(CU, "INSPER", "Registrar ficha del personal")
                        .param("id_usuario", Tipo.ID).param("cargo", CARGO).param("condicion_laboral", CONDICION)
                        .param("id_turno", Tipo.ID).opcional("unidad_academica", Tipo.texto(150)).opcional("fecha_ingreso", Tipo.FECHA)
                        .ejemplo("INSPER[\"3\",\"periodista\",\"planta\",\"1\",\"\",\"2019-02-01\"]").build(),
                CommandSpec.de(CU, "UPDPER", "Modificar ficha del personal")
                        .param("id", Tipo.ID).opcional("cargo", CARGO).opcional("condicion_laboral", CONDICION)
                        .opcional("id_turno", Tipo.ID).opcional("unidad_academica", Tipo.texto(150))
                        .omitible("estado", Tipo.opcion("activo", "inactivo"))
                        .ejemplo("UPDPER[\"1\",\"periodista\",\"planta\",\"2\",\"\"]").build(),
                CommandSpec.de(CU, "DELPER", "Desactivar personal")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPER[\"4\"]").build(),
                CommandSpec.de(CU, "LISTUR", "Listar turnos")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISTUR[\"*\"]").build(),
                CommandSpec.de(CU, "INSTUR", "Registrar turno")
                        .param("nombre", Tipo.texto(50)).param("hora_inicio", Tipo.HORA).param("hora_fin", Tipo.HORA)
                        .opcional("dias", Tipo.DIAS).opcional("descripcion", Tipo.texto(150))
                        .ejemplo("INSTUR[\"Parcial 15-19\",\"15:00\",\"19:00\",\"1,2,3,4,5\",\"Practicantes\"]").build(),
                CommandSpec.de(CU, "UPDTUR", "Modificar turno")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(50)).opcional("hora_inicio", Tipo.HORA)
                        .opcional("hora_fin", Tipo.HORA).opcional("dias", Tipo.DIAS).opcional("descripcion", Tipo.texto(150))
                        .ejemplo("UPDTUR[\"6\",\"Parcial 14-18\",\"14:00\",\"18:00\",\"1,2,3,4,5\",\"Practicantes\"]").build(),
                CommandSpec.de(CU, "DELTUR", "Eliminar turno sin personal")
                        .param("id", Tipo.ID)
                        .ejemplo("DELTUR[\"7\"]").build(),
                CommandSpec.de(CU, "LISAUS", "Listar ausencias")
                        .param("id_personal", Tipo.ID_O_TODOS)
                        .ejemplo("LISAUS[\"6\"]").build(),
                CommandSpec.de(CU, "INSAUS", "Registrar permiso, vacación o baja (días completos o con hora)")
                        .param("id_personal", Tipo.ID).param("tipo", TIPO_AUSENCIA).param("inicio", Tipo.MOMENTO)
                        .param("fin", Tipo.MOMENTO).opcional("motivo", Tipo.texto(255))
                        .ejemplo("INSAUS[\"6\",\"permiso\",\"2026-10-14 09:00\",\"2026-10-14 11:00\",\"Trámite personal\"]").build(),
                CommandSpec.de(CU, "UPDAUS", "Modificar ausencia")
                        .param("id", Tipo.ID).opcional("tipo", TIPO_AUSENCIA).opcional("inicio", Tipo.MOMENTO)
                        .opcional("fin", Tipo.MOMENTO).opcional("motivo", Tipo.texto(255))
                        .ejemplo("UPDAUS[\"1\",\"vacacion\",\"2026-10-14\",\"2026-10-15\",\"Vacación anual\"]").build(),
                CommandSpec.de(CU, "DELAUS", "Anular ausencia")
                        .param("id", Tipo.ID)
                        .ejemplo("DELAUS[\"1\"]").build(),
                CommandSpec.de(CU, "DISPER", "Personal disponible en una fecha y hora")
                        .param("fecha", Tipo.FECHA).param("hora", Tipo.HORA).omitible("cargo", CARGO)
                        .ejemplo("DISPER[\"2026-10-12\",\"16:00\",\"camarografo\"]").build());
    }
}
