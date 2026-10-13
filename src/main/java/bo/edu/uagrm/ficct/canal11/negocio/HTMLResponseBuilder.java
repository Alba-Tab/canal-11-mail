package bo.edu.uagrm.ficct.canal11.negocio;

import java.util.List;

/**
 * Constructor de respuestas en HTML («HTMLResponseBuilder» del documento).
 *
 * <p>Los estilos van en línea, porque Gmail y Outlook ignoran buena parte de las hojas de estilo.
 * Todo texto que viene de un usuario o de la base pasa por {@link #escapar(String)}: un dato con
 * {@code <script>} se muestra como texto y no se ejecuta.</p>
 *
 * <p>En esta versión arma la página y los mensajes (plantillas P3 y P5 de la revisión). Las tablas
 * de los listados y reportes (P2, P8, P9) se agregan con el procesador de comandos.</p>
 */
public final class HTMLResponseBuilder {

    /** Tipo de mensaje: define el color del recuadro. */
    public enum Tipo {
        EXITO("#1e7e34", "#e8f5e9"),
        ERROR("#b00020", "#fdecea"),
        ADVERTENCIA("#8a6d00", "#fff8e1"),
        INFORMACION("#0b5394", "#e8f0fe");

        private final String borde;
        private final String fondo;

        Tipo(String borde, String fondo) {
            this.borde = borde;
            this.fondo = fondo;
        }
    }

    static final String RECORDATORIO_FORMATO =
            "Formato: CODIGO[\"param1\",\"param2\",...] · fechas AAAA-MM-DD · horas HH:MM · duraciones mm:ss. "
                    + "Envíe AYUDA para ver los comandos que puede usar.";

    private HTMLResponseBuilder() {
    }

    /** Escapa {@code & < > " '} para mostrar un texto tal cual dentro del HTML. */
    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length() + 16);
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Página completa: encabezado del canal, el contenido y el recordatorio del formato. */
    public static String pagina(String titulo, String contenidoHtml) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>" + escapar(titulo) + "</title></head>"
                + "<body style=\"margin:0;padding:16px;background:#f4f5f7;font-family:Arial,Helvetica,sans-serif;color:#1f2328;\">"
                + "<div style=\"max-width:760px;margin:0 auto;background:#ffffff;border:1px solid #d0d7de;border-radius:8px;overflow:hidden;\">"
                + "<div style=\"background:#0b2e59;color:#ffffff;padding:12px 16px;\">"
                + "<div style=\"font-size:12px;opacity:.85;\">Canal 11 · Televisión Universitaria UAGRM</div>"
                + "<div style=\"font-size:18px;font-weight:bold;\">" + escapar(titulo) + "</div></div>"
                + "<div style=\"padding:16px;font-size:14px;line-height:1.5;\">" + contenidoHtml + "</div>"
                + "<div style=\"padding:10px 16px;border-top:1px solid #d0d7de;font-size:12px;color:#57606a;\">"
                + escapar(RECORDATORIO_FORMATO) + "</div>"
                + "</div></body></html>";
    }

    /** Recuadro con un mensaje («Éxito: …», «Error: …»). */
    public static String mensaje(Tipo tipo, String texto) {
        return "<div style=\"border-left:4px solid " + tipo.borde + ";background:" + tipo.fondo
                + ";padding:10px 12px;margin:0 0 12px 0;\">" + escapar(texto) + "</div>";
    }

    public static String parrafo(String texto) {
        return "<p style=\"margin:0 0 12px 0;\">" + escapar(texto) + "</p>";
    }

    /** Texto con formato de código (comandos y ejemplos). */
    public static String codigo(String texto) {
        return "<code style=\"background:#f6f8fa;border:1px solid #d0d7de;border-radius:4px;padding:1px 4px;"
                + "font-family:Consolas,Menlo,monospace;font-size:13px;\">" + escapar(texto) + "</code>";
    }

    /** Lista con viñetas; cada elemento ya es HTML (armado con estos métodos). */
    public static String lista(List<String> elementosHtml) {
        StringBuilder sb = new StringBuilder("<ul style=\"margin:0 0 12px 0;padding-left:20px;\">");
        for (String e : elementosHtml) {
            sb.append("<li style=\"margin:2px 0;\">").append(e).append("</li>");
        }
        return sb.append("</ul>").toString();
    }

    /** Respuesta de error completa: página con el mensaje en rojo. */
    public static String error(String mensaje) {
        return pagina("Error", mensaje(Tipo.ERROR, mensaje));
    }
}
