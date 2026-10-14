package bo.edu.uagrm.ficct.canal11.negocio.cu04;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-04 Gestión de Planificar Coberturas, Validar Conflictos de Operación y Liberar Recursos
 * (Tabla 82 del Anexo A).
 */
public final class ComandosCoberturas {

    private static final String CU = "CU-04";
    static final Tipo TIPO = Tipo.opcion("conferencia", "hecho_noticioso", "deportes", "acto_institucional", "entrevista");
    static final Tipo PRIORIDAD = Tipo.opcion("alta", "media", "baja");
    static final Tipo FUNCION = Tipo.opcion("periodista", "camarografo", "conductor");

    private ComandosCoberturas() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISCOB", "Agenda de coberturas")
                        .param("fecha", Tipo.FILTRO)
                        .ejemplo("LISCOB[\"2026-10-12\"]").build(),
                CommandSpec.de(CU, "INSCOB", "Planificar cobertura (si la hora de fin es menor, termina al día siguiente)")
                        .param("titulo", Tipo.texto(200)).opcional("tema", Tipo.texto(255)).param("tipo", TIPO)
                        .param("fecha", Tipo.FECHA).param("hora_inicio", Tipo.HORA).param("hora_fin", Tipo.HORA)
                        .param("lugar", Tipo.texto(200)).opcional("prioridad", PRIORIDAD).opcional("fuente", Tipo.texto(200))
                        .opcional("indicaciones", Tipo.TEXTO_LARGO_LIBRE)
                        .ejemplo("INSCOB[\"Clásico cruceño\",\"Conferencia previa\",\"deportes\",\"2026-10-12\",\"15:00\",\"16:30\","
                                + "\"Estadio Tahuichi\",\"media\",\"Prensa Club Blooming\",\"Conferencia de técnicos\"]").build(),
                CommandSpec.de(CU, "UPDCOB", "Modificar o reprogramar cobertura (revalida conflictos)")
                        .param("id", Tipo.ID).opcional("titulo", Tipo.texto(200)).opcional("fecha", Tipo.FECHA)
                        .opcional("hora_inicio", Tipo.HORA).opcional("hora_fin", Tipo.HORA).opcional("lugar", Tipo.texto(200))
                        .opcional("prioridad", PRIORIDAD)
                        .ejemplo("UPDCOB[\"4\",\"Clásico cruceño\",\"2026-10-13\",\"15:00\",\"16:30\",\"Estadio Tahuichi\",\"media\"]").build(),
                CommandSpec.de(CU, "DELCOB", "Cancelar cobertura (libera recursos)")
                        .param("id", Tipo.ID).opcional("motivo", Tipo.texto(1000))
                        .ejemplo("DELCOB[\"11\",\"Sesión suspendida\"]").build(),
                CommandSpec.de(CU, "DUPCOB", "Duplicar cobertura recurrente o reprogramada (conserva horas y duración)")
                        .param("id", Tipo.ID).param("nueva_fecha", Tipo.FECHA)
                        .ejemplo("DUPCOB[\"7\",\"2026-10-20\"]").build(),
                CommandSpec.de(CU, "LISCPE", "Personal asignado a una cobertura")
                        .param("id_cobertura", Tipo.ID_O_TODOS)
                        .ejemplo("LISCPE[\"2\"]").build(),
                CommandSpec.de(CU, "INSCPE", "Asignar personal (valida turno, ausencias y choques)")
                        .param("id_cobertura", Tipo.ID).param("id_personal", Tipo.ID).param("funcion", FUNCION)
                        .ejemplo("INSCPE[\"3\",\"1\",\"periodista\"]").build(),
                CommandSpec.de(CU, "UPDCPE", "Cambiar la función asignada")
                        .param("id", Tipo.ID).param("funcion", FUNCION)
                        .ejemplo("UPDCPE[\"5\",\"conductor\"]").build(),
                CommandSpec.de(CU, "DELCPE", "Quitar personal de la cobertura")
                        .param("id", Tipo.ID)
                        .ejemplo("DELCPE[\"5\"]").build(),
                CommandSpec.de(CU, "LISCRE", "Recursos reservados para una cobertura")
                        .param("id_cobertura", Tipo.ID_O_TODOS)
                        .ejemplo("LISCRE[\"2\"]").build(),
                CommandSpec.de(CU, "INSCRE", "Reservar recurso (valida estado y choques)")
                        .param("id_cobertura", Tipo.ID).param("codigo", Tipo.CODIGO_RECURSO)
                        .ejemplo("INSCRE[\"6\",\"MOV-02\"]").build(),
                CommandSpec.de(CU, "UPDCRE", "Cambiar el recurso reservado")
                        .param("id", Tipo.ID).param("codigo", Tipo.CODIGO_RECURSO)
                        .ejemplo("UPDCRE[\"10\",\"CAM-05\"]").build(),
                CommandSpec.de(CU, "DELCRE", "Liberar un recurso de la cobertura")
                        .param("id", Tipo.ID)
                        .ejemplo("DELCRE[\"10\"]").build(),
                CommandSpec.de(CU, "HOJCOB", "Hoja de asignación de la cobertura")
                        .param("id_cobertura", Tipo.ID)
                        .ejemplo("HOJCOB[\"2\"]").build(),
                CommandSpec.de(CU, "RETCOB", "Registrar retorno y liberar personal y recursos")
                        .param("id_cobertura", Tipo.ID).param("resultado", Tipo.opcion("cumplida", "cancelada", "reprogramada"))
                        .opcional("parte", Tipo.TEXTO_LARGO_LIBRE).opcional("incidencias", Tipo.TEXTO_LARGO_LIBRE)
                        .ejemplo("RETCOB[\"2\",\"cumplida\",\"Imágenes del bloqueo y 3 testimonios\",\"Ninguna\"]").build());
    }
}
