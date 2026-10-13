package bo.edu.uagrm.ficct.canal11.negocio;

/**
 * Lo que la capa de Presentación le pide a la de Negocio por cada correo: procesarlo y devolver la
 * respuesta.
 *
 * <p>En la rama {@code feat/bucle-principal} lo implementa {@link ProcesadorInicial}; después lo
 * reemplaza {@code CommandProcessor}, con el catálogo completo de comandos.</p>
 */
@FunctionalInterface
public interface ProcesadorCorreo {

    /**
     * Procesa un correo y arma su respuesta. Si lanza una excepción, quien llama responde un error
     * general y sigue con el correo siguiente.
     */
    RespuestaCorreo procesar(SolicitudCorreo solicitud) throws Exception;
}
