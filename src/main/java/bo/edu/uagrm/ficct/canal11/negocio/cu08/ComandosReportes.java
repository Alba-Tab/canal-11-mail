package bo.edu.uagrm.ficct.canal11.negocio.cu08;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-08 Reportes y Estadísticas (Tabla 86 del Anexo A).
 */
public final class ComandosReportes {

    private static final String CU = "CU-08";

    private ComandosReportes() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                periodo("REPCOB", "Coberturas por periodista"),
                periodo("REPREC", "Uso de equipos y unidades móviles (horas)"),
                periodo("REPMAN", "Incidencias de mantenimiento del período, con su cierre y días fuera de servicio"),
                periodo("REPNOT", "Notas emitidas, observadas y flotadas"),
                CommandSpec.de(CU, "LISBIT", "Bitácora de comandos recibidos (RNF-24)")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISBIT[\"2026-10-12\"]").build());
    }

    private static CommandSpec periodo(String codigo, String descripcion) {
        return CommandSpec.de(CU, codigo, descripcion)
                .param("fecha_desde", Tipo.FECHA).param("fecha_hasta", Tipo.FECHA)
                .ejemplo(codigo + "[\"2026-10-01\",\"2026-10-31\"]").build();
    }
}
