package bo.edu.uagrm.ficct.canal11.negocio;

import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Comando;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnmascaradorTest {

    private static final String INSUSU =
            "INSUSU[\"Carla\",\"Rojas Suárez\",\"3100003\",\"carla.rojas@gmail.com\",\"71234580\",\"clave123\",\"3\"]";

    @Test
    void ocultaLaContrasenaDeInsusu() {
        String asunto = Enmascarador.asunto("Re: " + INSUSU);
        assertEquals("INSUSU[\"Carla\",\"Rojas Suárez\",\"3100003\",\"carla.rojas@gmail.com\",\"71234580\",\"******\",\"3\"]", asunto);
        assertFalse(asunto.contains("clave123"));
    }

    @Test
    void unInsusuMalFormadoOcultaTodo() {
        String roto = INSUSU.substring(0, INSUSU.length() - 1); // sin el corchete de cierre
        assertEquals("INSUSU[******]", Enmascarador.asunto(roto));
    }

    @Test
    void sinCorchetesQuedaSoloElCodigo() {
        assertEquals("INSUSU", Enmascarador.asunto("insusu clave123"));
    }

    @Test
    void losDemasComandosQuedanIgual() {
        assertEquals("LISCOB[\"2026-10-12\"]", Enmascarador.asunto("  LISCOB[\"2026-10-12\"]  "));
        assertEquals("Hola", Enmascarador.asunto("Hola"));
        assertEquals("", Enmascarador.asunto(null));
        assertFalse(Enmascarador.esSensible("UPDUSU"));
        assertTrue(Enmascarador.esSensible("INSUSU"));
    }

    @Test
    void enmascaraLaListaDeParametros() {
        Comando c = (Comando) CommandParser.analizar(INSUSU);
        assertEquals(List.of("Carla", "Rojas Suárez", "3100003", "carla.rojas@gmail.com", "71234580", "******", "3"),
                Enmascarador.parametros(c));
        Comando corto = (Comando) CommandParser.analizar("INSUSU[\"Carla\"]");
        assertEquals(List.of("Carla"), Enmascarador.parametros(corto));
    }
}
