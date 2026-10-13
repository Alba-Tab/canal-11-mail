package bo.edu.uagrm.ficct.canal11.presentacion;

import bo.edu.uagrm.ficct.canal11.presentacion.correo.CorreoSaliente;

/** Dónde deja {@link MailVerificationThread} cada respuesta para que se envíe. */
@FunctionalInterface
public interface EnvioRespuestas {

    /** Deja la respuesta para enviar. No bloquea al hilo que lee el buzón. */
    void encolar(CorreoSaliente respuesta);
}
