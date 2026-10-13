package bo.edu.uagrm.ficct.canal11;

import bo.edu.uagrm.ficct.canal11.config.Config;
import bo.edu.uagrm.ficct.canal11.config.Registro;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto de entrada de la aplicación de consola.
 *
 * <p>Uso: {@code java -Duser.timezone=America/La_Paz -jar canal11-mail.jar [config.properties]}</p>
 *
 * <p>Por ahora carga la configuración y la registra. El bucle que lee el buzón
 * ({@code MailVerificationThread}) se agrega en la rama {@code feat/bucle-principal}.</p>
 */
public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(String[] args) {
        Registro.iniciarConsola(ZoneId.of("America/La_Paz"));
        Path archivo = Path.of(args.length > 0 ? args[0] : Config.ARCHIVO_POR_DEFECTO);
        try {
            Config config = Config.cargar(archivo);
            Registro.iniciar(config.carpetaLogs(), config.zonaHoraria());
            LOG.info("Canal 11 vía correo: configuración cargada desde " + archivo.toAbsolutePath());
            LOG.info(config.resumen());
        } catch (Config.ConfigException e) {
            LOG.severe(e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "No se pudo iniciar la aplicación", e);
        }
    }
}
