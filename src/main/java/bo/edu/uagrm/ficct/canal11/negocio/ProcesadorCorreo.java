package bo.edu.uagrm.ficct.canal11.negocio;

/**
 * Lo que la capa de Presentación le pide a la de Negocio por cada correo: procesarlo y devolver la
 * respuesta.
 *
 * <p>Lo implementa {@link CommandProcessor}; las pruebas de la capa de Presentación usan uno falso.</p>
 */
@FunctionalInterface
public interface ProcesadorCorreo {

    /**
     * Procesa un correo y arma su respuesta. Si lanza una excepción, quien llama responde un error
     * general y sigue con el correo siguiente.
     */
    RespuestaCorreo procesar(SolicitudCorreo solicitud) throws Exception;
}
