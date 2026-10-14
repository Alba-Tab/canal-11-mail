package bo.edu.uagrm.ficct.canal11.negocio.cu01;

import at.favre.lib.crypto.bcrypt.BCrypt;
import bo.edu.uagrm.ficct.canal11.negocio.Probador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static bo.edu.uagrm.ficct.canal11.negocio.Probador.AREA_TECNICA;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.DIRECTOR;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.JEFE_PRENSA;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.PERIODISTA;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.PRESENTADOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-01: un caso de éxito y uno de error por comando, con los ejemplos del Anexo A. */
@Tag("bd")
class ComandosUsuariosTest {

    private Probador p;

    @BeforeEach
    void abrir() throws Exception {
        p = Probador.abrir();
    }

    @AfterEach
    void cerrar() throws Exception {
        p.close();
    }

    private void contiene(String texto, String... esperados) {
        for (String e : esperados) {
            assertTrue(texto.contains(e), "Falta «" + e + "» en:\n" + texto);
        }
    }

    // ------------------------------------------------------------------ LISUSU

    @Test
    void lisusuElDirectorVeATodos() {
        contiene(p.texto(DIRECTOR, "LISUSU[\"*\"]"), "15 registros.", "Carla Rojas Suárez", "Américo Claros");
        contiene(p.texto(DIRECTOR, "LISUSU[\"periodista\"]"), "4 registros.");
        contiene(p.texto(JEFE_PRENSA, "LISUSU"), "15 registros.");
    }

    @Test
    void lisusuUnPeriodistaSoloVeSusDatos() {
        String t = p.texto(PERIODISTA, "LISUSU[\"*\"]");
        contiene(t, "Sus datos", "Carla Rojas Suárez", "solo puede ver sus propios datos");
        assertFalse(t.contains("Américo"), t);
    }

    @Test
    void lisusuSinPermiso() {
        contiene(p.texto(AREA_TECNICA, "LISUSU[\"*\"]"), "Error: no tiene permiso para ejecutar LISUSU");
    }

    // ------------------------------------------------------------------ INSUSU

    @Test
    void insusuGuardaLaContrasenaConBcrypt() throws Exception {
        String t = p.texto(DIRECTOR, "INSUSU[\"Ana\",\"Paz Rivero\",\"9100001\",\"Ana.Paz@Gmail.com\",\"71234599\",\"clave123\",\"3\"]");
        contiene(t, "Éxito: usuario registrado correctamente", "Ana Paz Rivero", "ana.paz@gmail.com", "Periodista", "INSPER[\"");
        assertFalse(t.contains("clave123"));
        String hash = p.uno("SELECT password FROM usuario WHERE ci = '9100001'").orElseThrow().texto("password");
        assertTrue(hash.startsWith("$2y$10$"), hash);
        assertTrue(BCrypt.verifyer().verify("clave123".toCharArray(), hash).verified);
    }

    @Test
    void insusuCorreoOCiRepetidosYRolInexistente() {
        contiene(p.texto(DIRECTOR, "INSUSU[\"Carla\",\"Rojas\",\"9100002\",\"Carla.Rojas@canal11.test\",\"\",\"clave123\",\"3\"]"),
                "Error: el correo ya está registrado");
        contiene(p.texto(DIRECTOR, "INSUSU[\"Ana\",\"Paz\",\"3100003\",\"ana@gmail.com\",\"\",\"clave123\",\"3\"]"),
                "Error: ese CI ya está registrado");
        contiene(p.texto(DIRECTOR, "INSUSU[\"Ana\",\"Paz\",\"9100003\",\"ana@gmail.com\",\"\",\"clave123\",\"99\"]"),
                "Error: el rol indicado no existe");
        contiene(p.texto(DIRECTOR, "INSUSU[\"Ana\",\"Paz\",\"9100003\",\"ana@gmail.com\",\"\",\"123\",\"3\"]"),
                "Error: la contraseña debe tener al menos 6 caracteres");
    }

    // ------------------------------------------------------------------ UPDUSU y DELUSU

    @Test
    void updusuCambiaSoloLoQueLlega() throws Exception {
        contiene(p.texto(DIRECTOR, "UPDUSU[\"3\",\"\",\"\",\"\",\"70000000\",\"\"]"), "Éxito: usuario modificado correctamente", "70000000");
        assertEquals("Carla", p.uno("SELECT nombre FROM usuario WHERE id = 3").orElseThrow().texto("nombre"));
        contiene(p.texto(DIRECTOR, "UPDUSU[\"3\",\"Carla\",\"Rojas Suárez\",\"carla.rojas@gmail.com\",\"71234580\",\"3\"]"),
                "carla.rojas@gmail.com");
    }

    @Test
    void updusuErrores() {
        contiene(p.texto(DIRECTOR, "UPDUSU[\"99\",\"X\",\"\",\"\",\"\",\"\"]"), "Error: el registro no existe");
        contiene(p.texto(DIRECTOR, "UPDUSU[\"3\",\"\",\"\",\"\",\"\",\"\"]"), "Error: indique al menos un dato para modificar");
        contiene(p.texto(DIRECTOR, "UPDUSU[\"3\",\"\",\"\",\"director@canal11.test\",\"\",\"\"]"), "Error: el correo ya está registrado");
        contiene(p.texto(DIRECTOR, "UPDUSU[\"1\",\"\",\"\",\"\",\"\",\"\",\"inactivo\"]"), "Error: no puede desactivarse ni quitarse el rol a sí mismo");
    }

    @Test
    void delusuDesactivaYUpdusuReactiva() throws Exception {
        contiene(p.texto(DIRECTOR, "DELUSU[\"3\"]"), "Éxito: usuario desactivado correctamente", "inactivo");
        contiene(p.texto(PERIODISTA, "LISNOT[\"*\"]"), "Error: no tiene permiso para ejecutar LISNOT. No está registrado");
        contiene(p.texto(DIRECTOR, "DELUSU[\"3\"]"), "Error: el usuario ya está inactivo");
        contiene(p.texto(DIRECTOR, "UPDUSU[\"3\",\"\",\"\",\"\",\"\",\"\",\"activo\"]"), "Éxito: usuario modificado correctamente", "activo");
        assertEquals("activo", p.uno("SELECT estado FROM usuario WHERE id = 3").orElseThrow().texto("estado"));
    }

    @Test
    void delusuErrores() throws Exception {
        contiene(p.texto(DIRECTOR, "DELUSU[\"1\"]"), "Error: no puede desactivarse a sí mismo");
        contiene(p.texto(DIRECTOR, "DELUSU[\"99\"]"), "Error: el registro no existe");
        // si otro rol recibe DELUSU, igual no puede dejar al canal sin Director
        contiene(p.texto(DIRECTOR, "INSRPM[\"2\",\"DELUSU\"]"), "Éxito: permiso otorgado correctamente");
        contiene(p.texto(JEFE_PRENSA, "DELUSU[\"1\"]"), "Error: debe quedar al menos un Director activo");
    }

    // ------------------------------------------------------------------ roles

    @Test
    void lisrol() {
        contiene(p.texto(DIRECTOR, "LISROL[\"*\"]"), "7 registros.", "Jefe de Area Tecnica", "Usuarios activos");
        contiene(p.texto(DIRECTOR, "LISROL[\"área\"]"), "1 registro.", "Jefe de Area Tecnica");
        contiene(p.texto(PERIODISTA, "LISROL[\"*\"]"), "Error: no tiene permiso para ejecutar LISROL");
    }

    @Test
    void insrolYUpdrolYDelrol() throws Exception {
        contiene(p.texto(DIRECTOR, "INSROL[\"Productor\",\"Apoya el armado de la escaleta\"]"),
                "Éxito: rol registrado correctamente", "Productor", "INSRPM[\"");
        long id = p.numero("SELECT id FROM rol WHERE nombre = 'Productor'");
        contiene(p.texto(DIRECTOR, "UPDROL[\"" + id + "\",\"Productor\",\"Arma la escaleta con el Jefe de Prensa\"]"),
                "Éxito: rol modificado correctamente", "Arma la escaleta");
        contiene(p.texto(DIRECTOR, "DELROL[\"" + id + "\"]"), "Éxito: rol eliminado correctamente");
        assertEquals(0, p.numero("SELECT count(*) FROM rol WHERE nombre = 'Productor'"));
    }

    @Test
    void insrolRepetido() {
        contiene(p.texto(DIRECTOR, "INSROL[\"periodista\",\"\"]"), "Error: el rol periodista ya existe");
    }

    @Test
    void updrolNoRenombraLosRolesDelSistema() {
        contiene(p.texto(DIRECTOR, "UPDROL[\"1\",\"Gerente\",\"\"]"), "Error: el rol Director no se puede renombrar");
        contiene(p.texto(DIRECTOR, "UPDROL[\"3\",\"Camarografo\",\"\"]"), "Error: el rol Camarografo ya existe");
        contiene(p.texto(DIRECTOR, "UPDROL[\"99\",\"X\",\"\"]"), "Error: el registro no existe");
    }

    @Test
    void delrolConUsuarios() {
        contiene(p.texto(DIRECTOR, "DELROL[\"3\"]"), "Error: el rol tiene usuarios asignados");
        contiene(p.texto(DIRECTOR, "DELROL[\"2\"]"), "Error: el rol Jefe de Prensa no se puede eliminar");
    }

    // ------------------------------------------------------------------ permisos

    @Test
    void lisprm() {
        contiene(p.texto(DIRECTOR, "LISPRM[\"CU-05\"]"), "18 registros.", "APRNOT", "Jefe de Prensa");
        contiene(p.texto(DIRECTOR, "LISPRM[\"aprnot\"]"), "1 registro.");
        contiene(p.texto(DIRECTOR, "LISPRM[\"*\"]"), "112 registros.");
        contiene(p.texto(DIRECTOR, "LISPRM[\"CU-09\"]"), "Error: filtre por \"*\", un caso de uso");
    }

    @Test
    void delprmEInsprmRestauranUnPermiso() throws Exception {
        long id = p.numero("SELECT id FROM permiso WHERE comando = 'EXPESC'");
        contiene(p.texto(DIRECTOR, "DELPRM[\"" + id + "\"]"), "Éxito: permiso eliminado correctamente", "nadie puede ejecutar EXPESC");
        contiene(p.texto(PRESENTADOR, "EXPESC[\"1\"]"), "Error: no tiene permiso para ejecutar EXPESC");
        contiene(p.texto(DIRECTOR, "INSPRM[\"EXPESC\",\"Exportar escaleta\",\"CU-06\"]"), "Éxito: permiso registrado correctamente");
        contiene(p.texto(DIRECTOR, "INSRPM[\"6\",\"EXPESC\"]"), "Éxito: permiso otorgado correctamente");
    }

    @Test
    void insprmErrores() {
        contiene(p.texto(DIRECTOR, "INSPRM[\"ZZZZZZ\",\"x\",\"CU-01\"]"), "Error: ZZZZZZ no es un comando del sistema");
        contiene(p.texto(DIRECTOR, "INSPRM[\"EXPESC\",\"x\",\"CU-06\"]"), "Error: ese comando ya está registrado como permiso");
        contiene(p.texto(DIRECTOR, "INSPRM[\"EXPESC\",\"x\",\"CU-02\"]"), "Error: EXPESC es del CU-06, no del CU-02");
    }

    @Test
    void updprm() throws Exception {
        long id = p.numero("SELECT id FROM permiso WHERE comando = 'EXPESC'");
        contiene(p.texto(DIRECTOR, "UPDPRM[\"" + id + "\",\"Exportar escaleta en HTML\",\"CU-06\"]"),
                "Éxito: permiso modificado correctamente", "Exportar escaleta en HTML");
        contiene(p.texto(DIRECTOR, "UPDPRM[\"40\",\"x\",\"CU-06\"]"), "Error: LISPRE es del CU-03, no del CU-06");
        contiene(p.texto(DIRECTOR, "UPDPRM[\"999\",\"x\",\"\"]"), "Error: el registro no existe");
    }

    @Test
    void delprmNoBorraLaAdministracion() throws Exception {
        long id = p.numero("SELECT id FROM permiso WHERE comando = 'INSRPM'");
        contiene(p.texto(DIRECTOR, "DELPRM[\"" + id + "\"]"), "Error: el permiso de INSRPM no se puede eliminar");
        contiene(p.texto(DIRECTOR, "DELPRM[\"999\"]"), "Error: el registro no existe");
    }

    @Test
    void lisrpm() {
        contiene(p.texto(DIRECTOR, "LISRPM[\"6\"]"), "Permisos del rol Presentador", "EXPESC");
        contiene(p.texto(DIRECTOR, "LISRPM[\"*\"]"), "7 registros.", "Jefe de Area Tecnica");
        contiene(p.texto(DIRECTOR, "LISRPM[\"99\"]"), "Error: el rol indicado no existe");
    }

    @Test
    void insrpmOtorgaYApareceEnAyuda() {
        assertFalse(p.texto(PERIODISTA, "AYUDA").contains("EXPESC"));
        contiene(p.texto(DIRECTOR, "INSRPM[\"3\",\"EXPESC\"]"), "Éxito: permiso otorgado correctamente", "Periodista ya puede usar EXPESC");
        contiene(p.texto(PERIODISTA, "AYUDA"), "EXPESC[\"1\"]");
    }

    @Test
    void insrpmErrores() {
        contiene(p.texto(DIRECTOR, "INSRPM[\"6\",\"EXPESC\"]"), "Error: el rol Presentador ya tiene el permiso de EXPESC");
        contiene(p.texto(DIRECTOR, "INSRPM[\"6\",\"ZZZZZZ\"]"), "Error: el comando ZZZZZZ no está registrado como permiso");
        contiene(p.texto(DIRECTOR, "INSRPM[\"99\",\"EXPESC\"]"), "Error: el rol indicado no existe");
    }

    @Test
    void delrpm() {
        contiene(p.texto(DIRECTOR, "DELRPM[\"6\",\"EXPESC\"]"), "Éxito: permiso quitado correctamente");
        contiene(p.texto(PRESENTADOR, "EXPESC[\"1\"]"), "Error: no tiene permiso para ejecutar EXPESC");
        contiene(p.texto(DIRECTOR, "DELRPM[\"6\",\"EXPESC\"]"), "Error: el rol Presentador no tiene el permiso de EXPESC");
        contiene(p.texto(DIRECTOR, "DELRPM[\"1\",\"INSRPM\"]"), "Error: al Director no se le puede quitar INSRPM");
    }
}
