package bo.edu.uagrm.ficct.canal11.negocio.cu06;

import bo.edu.uagrm.ficct.canal11.negocio.Probador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import static bo.edu.uagrm.ficct.canal11.negocio.Probador.DIRECTOR;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.JEFE_PRENSA;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.PERIODISTA;
import static bo.edu.uagrm.ficct.canal11.negocio.Probador.PRESENTADOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU-06 (programas, grilla, plantillas y cortes): un caso de éxito y uno de error por comando. */
@Tag("bd")
class ComandosProgramacionTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/La_Paz"));
    private static final LocalDate LUNES = HOY.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(3);

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

    private long nuevoPrograma() throws Exception {
        contiene(p.texto(JEFE_PRENSA, "INSPRG[\"Noticiero Mediodía\",\"noticiero\",\"Edición de las 13:00\"]"),
                "Éxito: programa registrado correctamente", "INSGRI[\"");
        return p.numero("SELECT id FROM programa WHERE nombre = 'Noticiero Mediodía'");
    }

    // ------------------------------------------------------------------ programas

    @Test
    void lisprg() {
        contiene(p.texto(PERIODISTA, "LISPRG[\"noticiero\"]"), "2 registros.", "Noticiero Central", "Noticiero Matinal");
        contiene(p.texto(PERIODISTA, "LISPRG"), "4 registros.");
        contiene(p.texto(PERIODISTA, "LISPRG[\"noticiero\",\"x\"]"), "Error: se esperaba 1 parámetro (tipo) y llegaron 2");
    }

    @Test
    void insprgUpdprgYDelprg() throws Exception {
        long id = nuevoPrograma();
        contiene(p.texto(JEFE_PRENSA, "UPDPRG[\"" + id + "\",\"Noticiero Mediodía\",\"noticiero\",\"Edición de las 13:00 a 13:30\"]"),
                "Éxito: programa modificado correctamente", "13:00 a 13:30");
        contiene(p.texto(JEFE_PRENSA, "DELPRG[\"" + id + "\"]"), "Éxito: programa desactivado correctamente", "inactivo");
        contiene(p.texto(JEFE_PRENSA, "DELPRG[\"" + id + "\"]"), "Error: el programa ya está inactivo");
        contiene(p.texto(JEFE_PRENSA, "UPDPRG[\"" + id + "\",\"\",\"\",\"\",\"activo\"]"), "activo");
    }

    @Test
    void programasErrores() {
        contiene(p.texto(JEFE_PRENSA, "INSPRG[\"Noticiero Central\",\"noticiero\",\"\"]"), "Error: ya existe un programa con ese nombre");
        contiene(p.texto(JEFE_PRENSA, "INSPRG[\"X\",\"telenovela\",\"\"]"), "Error: «tipo» debe ser noticiero, especial u otro");
        contiene(p.texto(JEFE_PRENSA, "UPDPRG[\"99\",\"X\",\"\",\"\"]"), "Error: el registro no existe");
        contiene(p.texto(PERIODISTA, "INSPRG[\"X\",\"otro\",\"\"]"), "Error: no tiene permiso para ejecutar INSPRG");
    }

    // ------------------------------------------------------------------ grilla (R16, R17)

    @Test
    void lisgri() {
        contiene(p.texto(PERIODISTA, "LISGRI[\"" + LUNES + "\",\"" + LUNES.plusDays(6) + "\"]"), "17 registros.", "Película de la noche", "sin escaleta");
        contiene(p.texto(PERIODISTA, "LISGRI"), "borrador (escaleta 1)");
        contiene(p.texto(PERIODISTA, "LISGRI[\"" + LUNES + "\"]"), "3 registros.");
        contiene(p.texto(PERIODISTA, "LISGRI[\"" + LUNES + "\",\"" + HOY + "\"]"), "Error: período inválido");
        contiene(p.texto(PERIODISTA, "LISGRI[\"" + HOY + "\",\"" + HOY.plusDays(40) + "\"]"), "Error: pida hasta 31 días");
    }

    @Test
    void insgriValidaLosChoques() throws Exception {
        long id = nuevoPrograma();
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"" + id + "\",\"1\",\"13:00\",\"13:30\",\"" + LUNES + "\",\"\"]"),
                "Éxito: horario agregado correctamente", "Lunes", "13:00–13:30");
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"" + id + "\",\"1\",\"20:30\",\"21:00\",\"" + LUNES + "\",\"\"]"),
                "Error: choca con Noticiero Central 20:00–21:00");
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"" + id + "\",\"6\",\"23:30\",\"24:00\",\"\",\"\"]"), "23:30–24:00");
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"" + id + "\",\"6\",\"23:30\",\"00:30\",\"\",\"\"]"), "Error: la hora de fin debe ser posterior a la de inicio");
        // con la vigencia terminada antes, ya no choca
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"" + id + "\",\"2\",\"06:00\",\"07:00\",\"" + HOY.minusDays(60) + "\",\"" + HOY.minusDays(40) + "\"]"),
                "Éxito: horario agregado correctamente");
    }

    @Test
    void insgriErrores() {
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"99\",\"1\",\"13:00\",\"13:30\",\"\",\"\"]"), "Error: el programa indicado no existe");
        contiene(p.texto(JEFE_PRENSA, "INSGRI[\"1\",\"8\",\"13:00\",\"13:30\",\"\",\"\"]"), "Error: «dia_semana» debe ser un número de 1 a 7");
    }

    @Test
    void updgri() {
        LocalDate fin = HOY.plusMonths(2);
        contiene(p.texto(JEFE_PRENSA, "UPDGRI[\"8\",\"06:30\",\"07:30\",\"" + fin + "\"]"), "Éxito: horario modificado correctamente", fin.toString());
        contiene(p.texto(JEFE_PRENSA, "UPDGRI[\"8\",\"20:30\",\"21:30\",\"\"]"), "Error: choca con Noticiero Central 20:00–21:00");
        contiene(p.texto(JEFE_PRENSA, "UPDGRI[\"99\",\"20:30\",\"21:30\",\"\"]"), "Error: el registro no existe");
    }

    @Test
    void delgri() throws Exception {
        contiene(p.texto(JEFE_PRENSA, "DELGRI[\"13\"]"), "Éxito: horario quitado de la grilla correctamente");
        assertEquals(0, p.numero("SELECT count(*) FROM grilla WHERE id = 13"));
        int conEscaleta = HOY.getDayOfWeek().getValue();  // el Central de hoy tiene la escaleta 1
        contiene(p.texto(JEFE_PRENSA, "DELGRI[\"" + conEscaleta + "\"]"), "queda inactivo para conservar su historial");
        contiene(p.texto(JEFE_PRENSA, "DELGRI[\"99\"]"), "Error: el registro no existe");
    }

    // ------------------------------------------------------------------ plantillas (R18, R19)

    @Test
    void lispla() {
        contiene(p.texto(PRESENTADOR, "LISPLA[\"1\"]"), "Noticiero Central", "Bloques", "Cortes comerciales", "Tiempo neto", "51:30", "ok");
        contiene(p.texto(DIRECTOR, "LISPLA[\"*\"]"), "3 registros.");
        contiene(p.texto(DIRECTOR, "LISPLA[\"99\"]"), "Error: el registro no existe");
    }

    @Test
    void insplaYCortesHastaQueEsteLista() throws Exception {
        long prg = nuevoPrograma();
        contiene(p.texto(JEFE_PRENSA, "INSPLA[\"" + prg + "\",\"Noticiero Mediodía\",\"30\",\"2\",\"155\"]"),
                "Éxito: plantilla registrada correctamente", "30:00", "todavía no está lista: debe tener 1 cortes");
        long pla = p.numero("SELECT id FROM plantilla WHERE nombre = 'Noticiero Mediodía'");
        contiene(p.texto(JEFE_PRENSA, "INSCOR[\"" + pla + "\",\"2\",\"15\",\"02:00\"]"), "Error: la plantilla tiene 2 bloque(s): sus cortes van del 1 al 1");
        contiene(p.texto(JEFE_PRENSA, "INSCOR[\"" + pla + "\",\"1\",\"29\",\"02:00\"]"), "Error: el corte termina después del final del programa (30:00)");
        contiene(p.texto(JEFE_PRENSA, "INSCOR[\"" + pla + "\",\"1\",\"15\",\"02:00\"]"),
                "Éxito: corte agregado correctamente", "cierra el bloque A", "está lista para generar escaletas");
    }

    @Test
    void insplaErrores() {
        contiene(p.texto(JEFE_PRENSA, "INSPLA[\"4\",\"Película\",\"120\",\"1\",\"\"]"), "Error: solo los noticieros y los especiales tienen plantilla");
        contiene(p.texto(JEFE_PRENSA, "INSPLA[\"1\",\"Noticiero Central\",\"60\",\"4\",\"\"]"), "Error: ya existe una plantilla con ese nombre");
        contiene(p.texto(JEFE_PRENSA, "INSPLA[\"1\",\"Otra\",\"60\",\"11\",\"\"]"), "Error: «cantidad_bloques» debe ser un número de 1 a 10");
    }

    @Test
    void updpla() {
        contiene(p.texto(JEFE_PRENSA, "UPDPLA[\"2\",\"Noticiero Matinal\",\"60\",\"3\",\"165\"]"), "Éxito: plantilla modificada correctamente", "ok");
        contiene(p.texto(JEFE_PRENSA, "UPDPLA[\"2\",\"\",\"\",\"4\",\"\"]"), "todavía no está lista: debe tener 3 cortes");
        contiene(p.texto(JEFE_PRENSA, "UPDPLA[\"1\",\"\",\"\",\"5\",\"\"]"), "Error: la plantilla tiene escaletas en borrador", "(R19)");
        contiene(p.texto(JEFE_PRENSA, "UPDPLA[\"1\",\"Noticiero Central\",\"\",\"\",\"140\"]"), "Éxito: plantilla modificada correctamente");
    }

    @Test
    void delpla() {
        contiene(p.texto(JEFE_PRENSA, "DELPLA[\"3\"]"), "Éxito: plantilla desactivada correctamente", "inactiva");
        contiene(p.texto(JEFE_PRENSA, "DELPLA[\"3\"]"), "Error: la plantilla ya está inactiva");
    }

    // ------------------------------------------------------------------ cortes

    @Test
    void liscor() {
        contiene(p.texto(JEFE_PRENSA, "LISCOR[\"1\"]"), "3 registros.", "cierra el bloque A", "minuto 12", "03:00");
        contiene(p.texto(JEFE_PRENSA, "LISCOR[\"99\"]"), "Error: el registro no existe");
        contiene(p.texto(DIRECTOR, "LISCOR[\"1\"]"), "Error: no tiene permiso para ejecutar LISCOR");
    }

    @Test
    void updcorYDelcor() {
        contiene(p.texto(JEFE_PRENSA, "UPDCOR[\"4\",\"21\",\"\"]"), "Éxito: corte modificado correctamente", "minuto 21");
        contiene(p.texto(JEFE_PRENSA, "DELCOR[\"5\"]"), "Éxito: corte eliminado correctamente", "debe tener 2 cortes");
        contiene(p.texto(JEFE_PRENSA, "DELCOR[\"99\"]"), "Error: el registro no existe");
    }

    @Test
    void losCortesDeUnaPlantillaConEscaletasEnBorradorNoCambian() {
        contiene(p.texto(JEFE_PRENSA, "UPDCOR[\"2\",\"29\",\"03:00\"]"), "Error: la plantilla tiene escaletas en borrador");
        contiene(p.texto(JEFE_PRENSA, "INSCOR[\"1\",\"3\",\"50\",\"01:00\"]"), "Error: la plantilla tiene escaletas en borrador");
        contiene(p.texto(JEFE_PRENSA, "DELCOR[\"1\"]"), "Error: la plantilla tiene escaletas en borrador");
    }
}
