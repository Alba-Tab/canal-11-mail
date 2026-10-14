# Decisiones técnicas

Lo que rige el código. Los análisis completos están en `../../docs/revision_general_sistema.md` (secciones 4 a 7). Si una decisión cambia, se actualiza aquí en la misma rama que cambia el código.

## Tomadas

| # | Decisión | Por qué | Fecha |
|---|---|---|---|
| D1 | La app es **de consola** y **corre en el servidor tecnoweb** (por SSH); POP3, SMTP y la BD se usan por `localhost` | Desde fuera, Sendmail rechaza el envío a otros dominios (`550 5.7.1 Relaying denied`); por SSH no hay pantalla | 13/10/2026 |
| D2 | POP3 y SMTP con `java.net.Socket`, a partir de los clientes de la lección 3.0 del docente; MIME a mano | El docente lo pide como base del Proyecto 1 | 13/10/2026 |
| D3 | Un solo hilo lee y procesa. **Un mensaje por sesión POP:** `RETR 1` → `DELE 1` → `QUIT`, y se procesa solo si `QUIT` respondió `+OK` | POP3 borra recién al `QUIT`: si la app se cae, no se reejecutan comandos y un correo que hace fallar al programa no vuelve. Además, el hilo único evita choques en las asignaciones (RNF-19) | 12/10/2026 |
| D4 | Una cobertura `planificada` cuya hora de inicio ya llegó pasa sola a `en_curso`, en cada vuelta del bucle | Ningún comando ponía ese estado | 13/10/2026 |
| D5 | Contraseñas con **bcrypt `$2y$`**, costo 10, máximo 72 bytes (`at.favre.lib:bcrypt`) | Lo enseñado en clase; la seguridad se evalúa desde el Proyecto 1; compatible con PHP/Laravel del hito web | 13/10/2026 |
| D6 | La contraseña de INSUSU se **enmascara** (`******`) en la bitácora y en el log, y no se repite en la respuesta | RNF-20 | 12/10/2026 |
| D7 | Sin avisos a terceros: el sistema solo responde a quien escribió | Alcance del hito 1 | 13/10/2026 |
| D8 | Respuesta en HTML UTF-8 **codificado en base64**; asunto según RFC 2047; cabeceras `Date`, `Message-ID`, `In-Reply-To`, `References` y `Auto-Submitted: auto-replied` | SMTP limita las líneas a 998 caracteres; las tildes; el hilo de la conversación en Gmail; menos spam | 13/10/2026 |
| D9 | Robustez: `try/catch` por correo y en el bucle; timeouts; reconexión a la BD; correos de más de 1 MB se descartan con respuesta; no se responde a robots ni a la propia cuenta; una sola copia (`FileChannel.tryLock`) | Cualquiera puede escribir al buzón; la app no se puede caer | 13/10/2026 |
| D10 | Zona horaria `America/La_Paz` en la JVM, en la sesión de la BD y en un `Clock` inyectado | `CURRENT_DATE` y los choques de horario dependen de ella | 13/10/2026 |
| D11 | Catálogo declarativo de comandos (`CommandSpec`), CRUD genérico para los comandos simples y handlers propios para los que tienen reglas | Son 112 comandos; menos código y menos errores | 13/10/2026 |
| D12 | `CommandProcessor` abre y cierra la transacción; la bitácora se escribe en otra transacción, siempre | Si el comando falla, igual queda registrado (R2) | 13/10/2026 |
| D13 | Los errores de PostgreSQL pasan por `TraductorErrores` (nombre de la restricción → mensaje); nunca se reenvía el texto crudo | Mensajes claros (RNF-22) y no mostrar detalles internos | 13/10/2026 |
| D14 | **El Jefe de Prensa redacta notas** (antes H3). Tiene ficha de personal con el cargo nuevo `jefe_prensa` y el permiso ENVNOT. Su nota sigue el mismo flujo que las demás: INSNOT → ENVNOT → APRNOT u OBSNOT | `nota.autor_id` exige una ficha de personal; así no cambia la tabla `nota` y el historial de revisión queda igual para todos | 13/10/2026 |
| D15 | El asunto se lee con tolerancia: se quitan `Re:`, `RV:`, `Fwd:` (también repetidos y `RE[2]:`); las comillas tipográficas cuentan como rectas; el código puede ir en minúsculas; se aceptan espacios entre parámetros y parámetros sin comillas si no tienen comas (`DELUSU[3]`); el texto después de `]` se ignora. **Una comilla dentro de un parámetro es error de formato** (antes L8) | Los clientes de correo reales (iPhone, respuestas, asuntos partidos) y la regla de «aceptar de todo» | 13/10/2026 |
| D16 | Remitente no registrado o inactivo (antes M8): a AYUDA se le responde «No está registrado en el sistema. Contacte al Director del canal.»; a un comando, «Error: no tiene permiso para ejecutar CODIGO.» más la misma frase | Flujo alternativo A1 del CU-01; quien escribe sabe a quién acudir | 13/10/2026 |
| D17 | El asunto de la respuesta es «Re: » más el asunto del pedido con los datos sensibles enmascarados, y el `Message-ID` del pedido va en `In-Reply-To`: Gmail la muestra en el mismo hilo | Seguir la conversación sin filtrar contraseñas (D6) | 13/10/2026 |
| D18 | Un parámetro vacío `""` en un UPD significa «no cambiar» (`col = COALESCE(?, col)`). La cantidad de parámetros es estricta (sección 4.2.4): los opcionales se envían vacíos; solo los marcados como omitibles al final (por ejemplo, el `estado` de M4 o el `cargo` de DISPER) pueden no enviarse. Un LIS sin corchetes o con `[]` vale `["*"]` | Así un UPD cambia un solo dato sin reescribir los demás, y el formato sigue siendo el del documento | 14/10/2026 |
| D19 | Volver atrás (antes M4): UPDUSU, UPDPER y UPDPRG aceptan un `estado` omitible al final (`activo`/`inactivo`), y UPDITE acepta `programado` para devolver un ítem flotado | Desactivar no borra (se conserva el historial), así que tiene que poder deshacerse por correo | 14/10/2026 |
| D20 | El sistema se protege de quedar sin administración: siempre queda un Director activo; nadie se desactiva ni se quita el rol a sí mismo; los roles Director y Jefe de Prensa no se renombran ni se eliminan (el código los reconoce por su nombre); el permiso de INSPRM, INSRPM, DELRPM, LISPRM y LISRPM no se borra ni se le quita al Director. INSPRM solo acepta comandos del catálogo y con su caso de uso | Todo se administra por correo: si el Director pierde esos comandos, nadie puede devolvérselos | 14/10/2026 |
| D21 | INSCPE y UPDCPE (antes L6): la función corresponde al cargo; la de `conductor` la hace un conductor o un presentador, y el Jefe de Prensa puede ir como `periodista` (D14) | Que un editor no salga como camarógrafo por error | 14/10/2026 |
| D22 | Reprogramar (antes M7): antes de salir, UPDCOB cambia la misma cobertura y vuelve a validar al equipo asignado (si hay un choque, no cambia nada); si volvieron sin cumplirla, RETCOB `reprogramada` y DUPCOB crea la nueva fecha, enlazada con `cobertura_origen_id`. DUPCOB no copia el equipo: lo muestra para asignarlo con INSCPE, que valida los horarios | Las dos formas quedan claras y ninguna salta la validación de choques | 14/10/2026 |
| D23 | Retorno y cancelación: RETCOB lo registra el Jefe de Prensa o alguien asignado a la cobertura; `cumplida` exige que la cobertura ya haya empezado. RETCOB y DELCOB cierran las entregas abiertas de la cobertura. Una cobertura cumplida, cancelada o reprogramada ya no se modifica | El retorno lo informa quien estuvo; lo terminado queda como historial para los reportes | 14/10/2026 |

## Pendientes

Cada una tiene una propuesta. Si nadie del grupo objeta antes de la rama indicada, se implementa la propuesta.

| # | Tema | Propuesta | Se decide antes de |
|---|---|---|---|
| M3 | GUINOT sobre una nota que ya está en una escaleta | Se permite; la nota vuelve a revisión y LISESC la marca con una alerta | `feat/cu05-notas` |
| M6 | Con qué comando lee el Presentador el guion completo | `LISNOT["id"]` con un solo id devuelve la nota con su guion | `feat/cu05-notas` |
| L5 | LISNOT y LISMAT filtrando solo por estado | `LISNOT["","aprobada"]`; en LISMAT, un número es id de nota y un texto es estado | `feat/cu05-notas` |
| M10 | ¿LISESC avisa si una nota tiene material sin terminar? | Sí, como alerta del ítem (no impide guardar) | `feat/cu06-escaleta` |
