package bo.edu.uagrm.ficct.canal11.negocio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HTMLResponseBuilderTest {

    @Test
    void escapaLosCaracteresDeHtml() {
        assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;ñ&quot;",
                HTMLResponseBuilder.escapar("<script>alert('x')</script> & \"ñ\""));
        assertEquals("", HTMLResponseBuilder.escapar(null));
    }

    @Test
    void unDatoDelUsuarioNoSeEjecuta() {
        String html = HTMLResponseBuilder.pagina("<b>título</b>",
                HTMLResponseBuilder.mensaje(HTMLResponseBuilder.Tipo.ERROR, "Error: <img src=x onerror=alert(1)>"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<b>título"));
        assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
    }

    @Test
    void laPaginaLlevaEncabezadoYRecordatorio() {
        String html = HTMLResponseBuilder.pagina("Ayuda", HTMLResponseBuilder.parrafo("Hola"));
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<meta charset=\"UTF-8\">"));
        assertTrue(html.contains("Canal 11"));
        assertTrue(html.contains("Envíe AYUDA"));
        assertTrue(html.contains("<p style=\"margin:0 0 12px 0;\">Hola</p>"));
    }

    @Test
    void listaYCodigo() {
        String lista = HTMLResponseBuilder.lista(List.of(HTMLResponseBuilder.codigo("LISCOB[\"*\"]")));
        assertTrue(lista.contains("<li"));
        assertTrue(lista.contains("LISCOB[&quot;*&quot;]"));
    }

    @Test
    void errorEsUnaPaginaConMensajeRojo() {
        String html = HTMLResponseBuilder.error("Error: el registro no existe");
        assertTrue(html.contains("#b00020"));
        assertTrue(html.contains("Error: el registro no existe"));
    }
}
