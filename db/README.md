# Base de datos `db_grupo14sa`

Scripts de PostgreSQL del sistema. **Son la fuente de verdad**: el script es el mismo de la sección 3.2.4.2 del documento oficial. El diseño completo (modelo conceptual, dominios y reglas R1–R26) está en `../../docs/base_de_datos/bd_formal_v2.md`.

## Archivos

| Archivo | Contenido |
|---|---|
| `script_db_grupo14sa.sql` | Borra y crea las 26 tablas, sus restricciones con nombre (`ck_*`, `uq_*`, `ux_*`), los índices y las 8 vistas de cálculo |
| `datos_prueba.sql` | Datos de la demo: 7 roles, 15 usuarios, 112 permisos y 205 asignaciones rol–permiso, personal, recursos, coberturas, notas y escaletas. Las fechas son relativas a `CURRENT_DATE`, así que la demo funciona cualquier día |
| `consultas_negocio.sql` | Las consultas que usa la capa de Negocio, probadas con variables de psql (`:variable` = `?` del `PreparedStatement`). Las que modifican datos van dentro de `BEGIN … ROLLBACK` |
| `pruebas_integridad.sql` | 21 pruebas automáticas de las restricciones (OK / FALLA). No dejan cambios: todo se deshace al final |

## Orden de ejecución

En una base vacía (en tecnoweb la base `db_grupo14sa` ya viene creada):

```bash
psql -h localhost -U grupo14sa -d db_grupo14sa -f script_db_grupo14sa.sql
```

```bash
psql -h localhost -U grupo14sa -d db_grupo14sa -f datos_prueba.sql
```

```bash
psql -h localhost -U grupo14sa -d db_grupo14sa -f pruebas_integridad.sql
```

Las 21 líneas deben decir `OK`. `consultas_negocio.sql` es opcional: muestra las consultas funcionando sobre los datos de prueba.

⚠️ `script_db_grupo14sa.sql` **borra todas las tablas** antes de crearlas. No correrlo sobre una base con datos reales.

## Antes de cargar los datos en el servidor

- Cambiar los correos `@canal11.test` de `datos_prueba.sql` por los correos reales del grupo: el sistema identifica a cada usuario por su remitente. Como mínimo, el del Director (usuario 1) y el de un usuario por rol para la demo.
- La contraseña de todas las cuentas de prueba es `canal11`, guardada con bcrypt (`$2y$`, costo 10). Solo sirve para el sistema web del siguiente hito: por correo, al usuario lo identifica su remitente.

## Si se cambia un script

1. Correr el script, los datos y las pruebas en una base vacía; las 21 deben seguir en `OK`.
2. Si cambió una tabla o una restricción, anotar el cambio para el documento en `../../docs/` (el Word no se toca) y actualizar los datos de `../../docs/diagramas/EA_generar_BD_Canal11.js`, que se generan desde el esquema real.
