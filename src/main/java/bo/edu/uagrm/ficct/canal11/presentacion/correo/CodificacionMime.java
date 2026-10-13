package bo.edu.uagrm.ficct.canal11.presentacion.correo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Codificaciones MIME que no trae Java: palabras codificadas para cabeceras (RFC 2047) y cuerpos
 * en base64 con líneas cortas (RFC 2045).
 *
 * <p>Una cabecera de correo solo admite ASCII. Un asunto como {@code Éxito: nota registrada} viaja
 * como {@code =?UTF-8?B?w4l4aXRvOiBub3RhIHJlZ2lzdHJhZGE=?=} y el cliente de correo lo muestra con
 * sus tildes.</p>
 */
public final class CodificacionMime {

    /** Una palabra codificada no puede pasar de 75 caracteres (RFC 2047, §2). */
    static final int MAX_PALABRA_CODIFICADA = 75;
    private static final String INICIO = "=?UTF-8?B?";
    private static final String FIN = "?=";
    /**
     * 42 bytes en base64 son 56 caracteres; con el inicio y el fin, 68. Así la primera línea,
     * {@code "Subject: " + palabra}, no pasa de los 78 caracteres recomendados (RFC 5322, §2.1.1).
     */
    private static final int MAX_BYTES_POR_PALABRA = 42;

    private CodificacionMime() {
    }

    /** {@code true} si el texto se puede poner tal cual en una cabecera (ASCII imprimible y espacios). */
    static boolean esAsciiImprimible(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valor listo para una cabecera. Si es ASCII imprimible queda igual; si no, se codifica en una o
     * más palabras {@code =?UTF-8?B?…?=}, sin partir nunca un carácter entre dos palabras.
     *
     * @return las palabras separadas por {@code "\r\n "} (cabecera plegada, RFC 5322 §2.2.3)
     */
    public static String codificarCabecera(String texto) {
        if (esAsciiImprimible(texto)) {
            return texto;
        }
        return String.join("\r\n ", palabrasCodificadas(texto));
    }

    static List<String> palabrasCodificadas(String texto) {
        List<String> palabras = new ArrayList<>();
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        texto.codePoints().forEach(cp -> {
            byte[] caracter = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
            if (actual.size() + caracter.length > MAX_BYTES_POR_PALABRA) {
                palabras.add(palabra(actual.toByteArray()));
                actual.reset();
            }
            actual.write(caracter, 0, caracter.length);
        });
        if (actual.size() > 0 || palabras.isEmpty()) {
            palabras.add(palabra(actual.toByteArray()));
        }
        return palabras;
    }

    private static String palabra(byte[] bytes) {
        return INICIO + Base64.getEncoder().encodeToString(bytes) + FIN;
    }

    /** Cuerpo en base64 con líneas de 76 caracteres separadas por {@code \r\n} (RFC 2045, §6.8). */
    public static String base64EnLineas(byte[] contenido) {
        return Base64.getMimeEncoder().encodeToString(contenido);
    }

    /** Quita saltos de línea y caracteres de control: evita que un dato agregue cabeceras (inyección). */
    static String sinSaltos(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            sb.append(Character.isISOControl(c) ? ' ' : c);
        }
        return sb.toString().strip();
    }
}
