package bo.edu.uagrm.ficct.canal11.negocio.catalogo;

import bo.edu.uagrm.ficct.canal11.datos.BaseDatosPrueba;
import bo.edu.uagrm.ficct.canal11.datos.Fila;
import bo.edu.uagrm.ficct.canal11.datos.Repositorio;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado;
import bo.edu.uagrm.ficct.canal11.negocio.CommandParser;
import bo.edu.uagrm.ficct.canal11.negocio.ErrorNegocio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoTest {

    private final Catalogo catalogo = Catalogo.completo();

    @Test
    void tiene112ComandosEnSusCasosDeUso() {
        assertEquals(112, catalogo.cantidad());
        Map<String, Long> porCu = catalogo.todos().stream()
                .collect(Collectors.groupingBy(CommandSpec::casoUso, TreeMap::new, Collectors.counting()));
        assertEquals(Map.of("CU-01", 15L, "CU-02", 13L, "CU-03", 16L, "CU-04", 15L, "CU-05", 18L,
                "CU-06", 26L, "CU-07", 4L, "CU-08", 5L), porCu);
    }

    /** Cada ejemplo del Anexo A se lee con su código y pasa la validación de su ficha. */
    @Test
    void cadaEjemploEsValido() throws ErrorNegocio {
        for (CommandSpec s : catalogo.todos()) {
            ComandoParseado p = CommandParser.analizar(s.ejemplo());
            ComandoParseado.Comando c = assertInstanceOf(ComandoParseado.Comando.class, p, s.codigo());
            assertEquals(s.codigo(), c.codigo());
            s.validar(c.parametros());
        }
    }

    @Test
    void validaLaCantidadDeParametros() {
        CommandSpec delusu = catalogo.buscar("DELUSU").orElseThrow();
        assertEquals("Error: se esperaba 1 parámetro (id) y no llegó ninguno",
                assertThrows(ErrorNegocio.class, () -> delusu.validar(List.of())).getMessage());
        CommandSpec disper = catalogo.buscar("DISPER").orElseThrow();
        assertEquals("Error: se esperaban entre 2 y 3 parámetros (fecha, hora, cargo) y llegaron 4",
                assertThrows(ErrorNegocio.class, () -> disper.validar(List.of("2026-10-12", "16:00", "camarografo", "x"))).getMessage());
        CommandSpec insrol = catalogo.buscar("INSROL").orElseThrow();
        assertEquals("Error: falta «nombre»: es obligatorio",
                assertThrows(ErrorNegocio.class, () -> insrol.validar(List.of("", "x"))).getMessage());
        assertEquals("INSROL[\"nombre\",\"descripcion\"]", insrol.formato());
    }

    @Test
    void losOmitiblesPuedenFaltar() throws ErrorNegocio {
        Parametros p = catalogo.buscar("DISPER").orElseThrow().validar(List.of("2026-10-12", "16:00"));
        assertTrue(!p.tiene("cargo"));
        Parametros vacios = catalogo.buscar("UPDROL").orElseThrow().validar(List.of("8", "", "Nueva descripción"));
        assertTrue(!vacios.tiene("nombre"));
        assertEquals("Nueva descripción", vacios.texto("descripcion"));
    }

    /** El catálogo y la tabla permiso tienen los mismos comandos, con la misma descripción y caso de uso. */
    @Test
    @Tag("bd")
    void coincideConLaTablaPermiso() throws Exception {
        List<Fila> permisos = new Repositorio(10).consultar(BaseDatosPrueba.conexion().obtener(),
                "SELECT comando, descripcion, caso_uso FROM permiso ORDER BY id");
        assertEquals(catalogo.cantidad(), permisos.size());
        int i = 0;
        for (CommandSpec s : catalogo.todos()) {
            Fila f = permisos.get(i++);
            assertEquals(s.codigo(), f.texto("comando"), "orden del Anexo A");
            assertEquals(s.descripcion(), f.texto("descripcion"), s.codigo());
            assertEquals(s.casoUso(), f.texto("caso_uso"), s.codigo());
        }
    }
}
