package bo.edu.uagrm.ficct.canal11.negocio.cu07;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-07 Gestión de Analítica Periodística (Tabla 85 del Anexo A).
 */
public final class ComandosAnalitica {

    private static final String CU = "CU-07";

    private ComandosAnalitica() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                periodo("ANAPER", "Producción por periodista: notas, % aprobadas a la primera y tiempo medio de aprobación"),
                periodo("ANAEMI", "Precisión de las emisiones: duración real frente a la suma de la escaleta, e ítems de última hora"),
                periodo("ANACOB", "Cumplimiento de coberturas por tipo"),
                periodo("ANANOT", "Duración media por tipo de pieza"));
    }

    private static CommandSpec periodo(String codigo, String descripcion) {
        return CommandSpec.de(CU, codigo, descripcion)
                .param("fecha_desde", Tipo.FECHA).param("fecha_hasta", Tipo.FECHA)
                .ejemplo(codigo + "[\"2026-10-01\",\"2026-10-31\"]").build();
    }
}
