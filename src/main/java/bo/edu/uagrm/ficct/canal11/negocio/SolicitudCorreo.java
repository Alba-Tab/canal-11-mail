package bo.edu.uagrm.ficct.canal11.negocio;

import java.util.Objects;

/**
 * Lo que la capa de Negocio recibe de un correo: quién lo envió, el asunto y el cuerpo, ya
 * decodificados. La capa de Presentación se encarga del MIME y de los sockets.
 *
 * @param remitente dirección del remitente, en minúsculas
 * @param asunto    asunto decodificado (el comando)
 * @param cuerpo    texto del correo sin firma ni citas (el guion de INSNOT y GUINOT)
 */
public record SolicitudCorreo(String remitente, String asunto, String cuerpo) {
    public SolicitudCorreo {
        Objects.requireNonNull(remitente);
        asunto = asunto == null ? "" : asunto;
        cuerpo = cuerpo == null ? "" : cuerpo;
    }
}
