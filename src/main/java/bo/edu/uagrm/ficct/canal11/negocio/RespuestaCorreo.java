package bo.edu.uagrm.ficct.canal11.negocio;

import java.util.Objects;

/**
 * Lo que la capa de Negocio devuelve para responder: el asunto y el cuerpo en HTML.
 *
 * <p>El asunto nunca repite un dato sensible (decisión D6): se arma con {@link Enmascarador}.</p>
 */
public record RespuestaCorreo(String asunto, String html) {
    public RespuestaCorreo {
        Objects.requireNonNull(asunto);
        Objects.requireNonNull(html);
    }
}
