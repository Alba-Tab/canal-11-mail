package bo.edu.uagrm.ficct.canal11.negocio;

import bo.edu.uagrm.ficct.canal11.datos.BaseDatosPrueba;
import bo.edu.uagrm.ficct.canal11.datos.DBConnection;
import bo.edu.uagrm.ficct.canal11.datos.Repositorio;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("bd")
class ProcesadorInicialTest {

    private final Repositorio repo = new Repositorio(10);

    private ProcesadorInicial procesador() throws Exception {
        return new ProcesadorInicial(BaseDatosPrueba.conexion(), repo);
    }

    @Test
    void ayudaParaUnUsuarioRegistrado() throws Exception {
        RespuestaCorreo r = procesador().procesar(new SolicitudCorreo("director@canal11.test", "AYUDA", ""));
        assertEquals("Re: AYUDA", r.asunto());
        assertTrue(r.html().contains("Hola, Américo Claros (Director)"), r.html());
    }

    @Test
    void elCorreoSeComparaSinMayusculas() throws Exception {
        RespuestaCorreo r = procesador().procesar(new SolicitudCorreo("Director@Canal11.TEST", "ayuda", ""));
        assertTrue(r.html().contains("Hola, Américo Claros"), r.html());
    }

    @Test
    void unRemitenteNoRegistrado() throws Exception {
        RespuestaCorreo ayuda = procesador().procesar(new SolicitudCorreo("desconocido@gmail.com", "AYUDA", ""));
        assertTrue(ayuda.html().contains(ProcesadorInicial.NO_REGISTRADO), ayuda.html());
        RespuestaCorreo comando = procesador().procesar(new SolicitudCorreo("desconocido@gmail.com", "LISCOB[\"*\"]", ""));
        assertTrue(comando.html().contains("Error: no tiene permiso para ejecutar LISCOB"), comando.html());
    }

    @Test
    void unUsuarioInactivoEsComoNoRegistrado() throws Exception {
        DBConnection db = BaseDatosPrueba.conexion();
        repo.ejecutar(db.obtener(), "UPDATE usuario SET estado = 'inactivo' WHERE correo = ?", "carla.rojas@canal11.test");
        try {
            RespuestaCorreo r = procesador().procesar(new SolicitudCorreo("carla.rojas@canal11.test", "AYUDA", ""));
            assertTrue(r.html().contains(ProcesadorInicial.NO_REGISTRADO), r.html());
        } finally {
            repo.ejecutar(db.obtener(), "UPDATE usuario SET estado = 'activo' WHERE correo = ?", "carla.rojas@canal11.test");
        }
    }

    @Test
    void confirmaUnComandoSinMostrarLaContrasena() throws Exception {
        RespuestaCorreo r = procesador().procesar(new SolicitudCorreo("director@canal11.test",
                "INSUSU[\"Ana\",\"Paz\",\"9\",\"ana@gmail.com\",\"7\",\"clave-de-ana\",\"3\"]", ""));
        assertTrue(r.asunto().contains("******"), r.asunto());
        assertFalse(r.asunto().contains("clave-de-ana"));
        assertFalse(r.html().contains("clave-de-ana"));
        assertTrue(r.html().contains("Comando INSUSU recibido"), r.html());
    }

    @Test
    void unAsuntoInvalidoRespondeElMensajeDeLaTabla77() throws Exception {
        RespuestaCorreo r = procesador().procesar(new SolicitudCorreo("director@canal11.test", "Hola, ¿cómo están?", ""));
        assertTrue(r.html().contains("Error: comando no reconocido"), r.html());
        RespuestaCorreo vacio = procesador().procesar(new SolicitudCorreo("director@canal11.test", "", ""));
        assertEquals("Re: (sin asunto)", vacio.asunto());
    }

    @Test
    void sinBaseDeDatosRespondeUnErrorClaro() {
        DBConnection caida = new DBConnection("jdbc:postgresql://127.0.0.1:1/no_existe", "x", "x", ZoneId.of("America/La_Paz"), 2);
        RespuestaCorreo r = new ProcesadorInicial(caida, repo).procesar(new SolicitudCorreo("director@canal11.test", "AYUDA", ""));
        assertTrue(r.html().contains("no puede acceder a la base de datos"), r.html());
    }
}
