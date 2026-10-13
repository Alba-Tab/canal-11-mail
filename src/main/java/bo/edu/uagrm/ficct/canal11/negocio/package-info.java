/**
 * Capa de Negocio (módulo de control y servicio).
 *
 * <ul>
 *   <li>{@code CommandParser}: separa el código del comando y sus parámetros del asunto.</li>
 *   <li>{@code CommandProcessor}: verifica el permiso (R1), valida, ejecuta el comando en una transacción y registra la bitácora (R2).</li>
 *   <li>{@code HTMLResponseBuilder}: arma la respuesta en HTML (plantillas P1 a P10 de la revisión).</li>
 * </ul>
 */
package bo.edu.uagrm.ficct.canal11.negocio;
