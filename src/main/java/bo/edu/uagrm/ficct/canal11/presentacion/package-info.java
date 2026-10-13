/**
 * Capa de Presentación (módulo de comunicación).
 *
 * <p>Es la «interfaz» del sistema: el correo electrónico. Según el diagrama de componentes del documento:</p>
 * <ul>
 *   <li>{@code MailVerificationThread}: hilo único que lee el buzón por POP3 y procesa los correos de a uno.</li>
 *   <li>{@code SendEmailThread}: envía las respuestas por SMTP desde una cola.</li>
 * </ul>
 */
package bo.edu.uagrm.ficct.canal11.presentacion;
