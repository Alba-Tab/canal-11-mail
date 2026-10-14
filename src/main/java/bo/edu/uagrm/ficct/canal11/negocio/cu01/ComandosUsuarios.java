package bo.edu.uagrm.ficct.canal11.negocio.cu01;

import bo.edu.uagrm.ficct.canal11.negocio.catalogo.CommandSpec;
import bo.edu.uagrm.ficct.canal11.negocio.catalogo.Tipo;

import java.util.List;

/**
 * CU-01 Gestión de Usuarios: usuarios, roles y permisos (Tabla 79 del Anexo A).
 */
public final class ComandosUsuarios {

    private static final String CU = "CU-01";
    private static final Tipo ESTADO = Tipo.opcion("activo", "inactivo");

    private ComandosUsuarios() {
    }

    public static List<CommandSpec> comandos() {
        return List.of(
                CommandSpec.de(CU, "LISUSU", "Listar usuarios (Periodista, Camarógrafo, Editor y Presentador solo reciben sus propios datos)")
                        .param("rol", Tipo.FILTRO)
                        .ejemplo("LISUSU[\"*\"]").build(),
                CommandSpec.de(CU, "INSUSU", "Registrar usuario")
                        .param("nombre", Tipo.texto(100)).param("apellido", Tipo.texto(100)).param("ci", Tipo.texto(20))
                        .param("correo", Tipo.CORREO).opcional("telefono", Tipo.texto(20)).param("password", Tipo.CONTRASENA)
                        .param("id_rol", Tipo.ID)
                        .ejemplo("INSUSU[\"Carla\",\"Rojas Suárez\",\"3100003\",\"carla.rojas@gmail.com\",\"71234580\",\"clave123\",\"3\"]").build(),
                CommandSpec.de(CU, "UPDUSU", "Modificar usuario")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(100)).opcional("apellido", Tipo.texto(100))
                        .opcional("correo", Tipo.CORREO).opcional("telefono", Tipo.texto(20)).opcional("id_rol", Tipo.ID)
                        .omitible("estado", ESTADO)
                        .ejemplo("UPDUSU[\"3\",\"Carla\",\"Rojas Suárez\",\"carla.rojas@gmail.com\",\"71234580\",\"3\"]").build(),
                CommandSpec.de(CU, "DELUSU", "Desactivar usuario")
                        .param("id", Tipo.ID)
                        .ejemplo("DELUSU[\"3\"]").build(),
                CommandSpec.de(CU, "LISROL", "Listar roles")
                        .param("filtro", Tipo.FILTRO)
                        .ejemplo("LISROL[\"*\"]").build(),
                CommandSpec.de(CU, "INSROL", "Registrar rol")
                        .param("nombre", Tipo.texto(50)).opcional("descripcion", Tipo.texto(150))
                        .ejemplo("INSROL[\"Productor\",\"Apoya el armado de la escaleta\"]").build(),
                CommandSpec.de(CU, "UPDROL", "Modificar rol")
                        .param("id", Tipo.ID).opcional("nombre", Tipo.texto(50)).opcional("descripcion", Tipo.texto(150))
                        .ejemplo("UPDROL[\"8\",\"Productor\",\"Arma la escaleta con el Jefe de Prensa\"]").build(),
                CommandSpec.de(CU, "DELROL", "Eliminar rol sin usuarios")
                        .param("id", Tipo.ID)
                        .ejemplo("DELROL[\"8\"]").build(),
                CommandSpec.de(CU, "LISPRM", "Listar comandos con permiso")
                        .param("caso_uso", Tipo.FILTRO)
                        .ejemplo("LISPRM[\"CU-05\"]").build(),
                CommandSpec.de(CU, "INSPRM", "Registrar un comando como permiso")
                        .param("comando", Tipo.COMANDO).param("descripcion", Tipo.texto(150)).param("caso_uso", Tipo.CASO_USO)
                        .ejemplo("INSPRM[\"EXPESC\",\"Exportar escaleta\",\"CU-06\"]").build(),
                CommandSpec.de(CU, "UPDPRM", "Modificar permiso")
                        .param("id", Tipo.ID).opcional("descripcion", Tipo.texto(150)).opcional("caso_uso", Tipo.CASO_USO)
                        .ejemplo("UPDPRM[\"40\",\"Exportar escaleta en HTML\",\"CU-06\"]").build(),
                CommandSpec.de(CU, "DELPRM", "Eliminar permiso")
                        .param("id", Tipo.ID)
                        .ejemplo("DELPRM[\"40\"]").build(),
                CommandSpec.de(CU, "LISRPM", "Listar los permisos de un rol")
                        .param("id_rol", Tipo.ID_O_TODOS)
                        .ejemplo("LISRPM[\"6\"]").build(),
                CommandSpec.de(CU, "INSRPM", "Otorgar un comando a un rol")
                        .param("id_rol", Tipo.ID).param("comando", Tipo.COMANDO)
                        .ejemplo("INSRPM[\"6\",\"EXPESC\"]").build(),
                CommandSpec.de(CU, "DELRPM", "Quitar un comando a un rol")
                        .param("id_rol", Tipo.ID).param("comando", Tipo.COMANDO)
                        .ejemplo("DELRPM[\"6\",\"EXPESC\"]").build());
    }
}
