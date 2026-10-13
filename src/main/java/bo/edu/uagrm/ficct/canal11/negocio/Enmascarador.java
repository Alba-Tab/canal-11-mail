package bo.edu.uagrm.ficct.canal11.negocio;

import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Comando;
import bo.edu.uagrm.ficct.canal11.negocio.ComandoParseado.Invalido;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Oculta los parámetros sensibles de un comando (decisión D6) antes de escribirlo en el log, en la
 * bitácora o en el asunto de la respuesta.
 *
 * <p>Hoy el único es la contraseña de INSUSU (sexto parámetro). Si el asunto de un comando sensible
 * viene mal formado, se ocultan todos sus parámetros: así nunca se filtra una contraseña a medio
 * escribir.</p>
 */
public final class Enmascarador {

    public static final String MASCARA = "******";

    /** Código del comando y posiciones (desde 0) de sus parámetros sensibles. */
    private static final Map<String, Set<Integer>> SENSIBLES = Map.of(
            "INSUSU", Set.of(5));

    private Enmascarador() {
    }

    /** {@code true} si el comando tiene parámetros que no se pueden mostrar. */
    public static boolean esSensible(String codigo) {
        return codigo != null && SENSIBLES.containsKey(codigo);
    }

    /** El asunto listo para mostrar o registrar: igual que llegó, salvo los parámetros sensibles. */
    public static String asunto(String asunto) {
        if (asunto == null) {
            return "";
        }
        ComandoParseado p = CommandParser.analizar(asunto);
        if (p instanceof Comando c && esSensible(c.codigo())) {
            if (!c.conCorchetes()) {
                return c.codigo();
            }
            return c.codigo() + parametros(c).stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",", "[", "]"));
        }
        if (p instanceof Invalido i && esSensible(i.codigo())) {
            return i.codigo() + "[" + MASCARA + "]";
        }
        return asunto.strip();
    }

    /** Los parámetros del comando con los sensibles reemplazados por {@link #MASCARA}. */
    public static List<String> parametros(Comando c) {
        Set<Integer> ocultos = SENSIBLES.getOrDefault(c.codigo(), Set.of());
        List<String> resultado = new ArrayList<>(c.parametros());
        for (int i : ocultos) {
            if (i < resultado.size()) {
                resultado.set(i, MASCARA);
            }
        }
        return resultado;
    }
}
