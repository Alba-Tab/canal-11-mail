# canal11-mail

Sistema de Gestión de Operaciones y Noticias del **Canal 11 (TVU-UAGRM)**, operado por correo electrónico.
Proyecto 1 de Tecnología Web (INF513) — Grupo 14 SA — Semestre II-2026.

## Qué hace

Un usuario del canal envía un correo a `grupo14sa@tecnoweb.org.bo` con un comando en el asunto:

```
LISCOB["2026-10-12"]
```

El sistema lee el buzón por POP3, verifica que el remitente esté registrado y que su rol tenga permiso para ese comando, ejecuta la operación en PostgreSQL y le responde por SMTP con el resultado en HTML. Si el asunto es `AYUDA`, responde con los comandos que puede usar ese rol.

Son 112 comandos más AYUDA, repartidos en 8 casos de uso: usuarios, personal y turnos, recursos técnicos, coberturas, notas (NRCS), escaleta del noticiero, analítica y reportes. El catálogo completo está en el Anexo A del documento oficial.

## Estado

En desarrollo.

## Requisitos

| Herramienta | Para qué |
|---|---|
| JDK 17 o superior | Compilar y correr |
| PostgreSQL 11 o superior | Base de datos (en el servidor: `db_grupo14sa`) |
| Acceso SSH a `www.tecnoweb.org.bo` | Subir y correr la aplicación |

Maven no hace falta: el repositorio trae el Maven Wrapper (`./mvnw`).

## Compilar y probar

```bash
./mvnw verify
```

Compila, corre las pruebas y deja un solo jar con todo adentro en `target/canal11-mail.jar`.

### Pruebas con base de datos

Las pruebas marcadas `@Tag("bd")` necesitan un PostgreSQL. Sin él se saltan solas. Para correrlas:

```bash
createdb db_grupo14sa_test
```

```bash
export CANAL11_BD_PRUEBA_URL=jdbc:postgresql://localhost:5432/db_grupo14sa_test CANAL11_BD_PRUEBA_USUARIO=postgres CANAL11_BD_PRUEBA_CONTRASENA=
```

```bash
./mvnw verify
```

La primera prueba carga `db/script_db_grupo14sa.sql` y `db/datos_prueba.sql`, y cada prueba deshace sus cambios. Como el script **borra todas las tablas**, solo se acepta una base con «test» en el nombre. También corren las 21 pruebas de `db/pruebas_integridad.sql`.

## Desplegar en el servidor

La aplicación corre en `www.tecnoweb.org.bo`. La guía paso a paso está en [docs/despliegue.md](docs/despliegue.md). En resumen:

```bash
scripts/subir.sh grupo14sa@www.tecnoweb.org.bo
```

Después, en el servidor, dentro de `~/canal11`: crear `config.properties`, correr `scripts/verificar-servidor.sh su.correo@gmail.com` y `scripts/cargar-bd.sh`, y arrancar con `scripts/run.sh`. Para operarla: `scripts/estado.sh` y `scripts/stop.sh`.

## Configurar y correr

```bash
cp config.example.properties config.properties
```

Completar `config.properties` con las contraseñas del buzón y de la base de datos. No se versiona; en el servidor, darle permisos `600`.

```bash
java -Duser.timezone=America/La_Paz -jar target/canal11-mail.jar config.properties
```

El log va a la consola y a `logs/canal11.0.log`.

Qué hace al arrancar:
- Revisa el buzón cada 30 s (`app.intervalo_segundos`). Saca un correo por sesión POP: `RETR` → `DELE` → `QUIT`, y lo procesa solo si el servidor confirmó el `QUIT`.
- Responde en HTML desde un segundo hilo.
- No responde a rebotes, a respuestas automáticas ni a sus propios correos.
- Los correos de más de 1 MB no se descargan: se responde que no se aceptan adjuntos.
- Pasa a `en_curso` las coberturas que ya empezaron.
- Si se cae el buzón o la base de datos, lo anota en el log y reintenta en la vuelta siguiente: no se detiene.

Corre **una sola copia**: si ya hay otra, la segunda avisa y termina (bloqueo en `canal11.lock`). Para detenerla, **Ctrl+C** o `kill`. Termina el correo en curso, envía las respuestas pendientes y escribe «Aplicación detenida» en el log.

Cada correo pasa por `CommandProcessor`:

1. `AYUDA` responde los comandos del rol del remitente, con un ejemplo de cada uno (sale de `rol_permiso`: si el Director otorga o quita un permiso, la ayuda cambia sola).
2. El comando tiene que existir en el catálogo (`negocio/catalogo` y un paquete `negocio/cuNN` por caso de uso) y el rol del remitente tiene que tener el permiso (R1). Un remitente no registrado o inactivo recibe «No está registrado en el sistema. Contacte al Director del canal.».
3. Se validan la cantidad y el tipo de los parámetros: fechas `AAAA-MM-DD`, horas `HH:MM`, duraciones `mm:ss`, estados sin tildes. Los opcionales se envían vacíos (`""`); en un UPD, un parámetro vacío deja el dato como está. Un LIS sin corchetes lista todo (`"*"`); un INS, UPD o DEL sin corchetes responde el formato con un ejemplo.
4. El comando se ejecuta en una transacción: si algo falla, no queda nada a medias. Los errores de la base se responden con un mensaje claro, nunca con el texto de PostgreSQL.
5. Todo correo queda en `bitacora_comando` (también los rechazados), con la contraseña de INSUSU enmascarada.

Los comandos que todavía no están habilitados responden un aviso y figuran en AYUDA como «(próximamente)». Hoy están habilitados los del CU-01 al CU-06 y el CU-08 (108 comandos); falta la analítica (CU-07). Los reportes reciben un período de hasta 366 días y llevan una fila de totales.

EXPESC responde con la escaleta como adjunto `.html` (correo `multipart/mixed`): se abre en el navegador y se puede imprimir.

## Estructura

```
canal11-mail/
├── docs/DECISIONES.md   decisiones técnicas y pendientes
├── db/                  script de la BD, datos de prueba, consultas y pruebas de integridad
├── pom.xml, mvnw       build con Maven Wrapper (Java 17)
└── src/main/java/bo/edu/uagrm/ficct/canal11/
    ├── App.java         arranque
    ├── config/          configuración y log
    ├── presentacion/    MailVerificationThread, SendEmailThread y clientes POP3/SMTP
    ├── negocio/         CommandParser, CommandProcessor, HTMLResponseBuilder y AYUDA
    │   ├── catalogo/    ficha de cada comando: parámetros con su tipo y ejemplo
    │   └── cu01 … cu08/ comandos de cada caso de uso con sus reglas
    └── datos/           DBConnection, repositorio, CRUD genérico, bitácora y traductor de errores
```

## Cómo se trabaja

- `main` recibe solo las entregas.
- Se trabaja en ramas `feat/`, `fix/`, `chore/` o `docs/` que salen de `develop`.
- Cada rama vuelve a `develop` con `git merge --no-ff`, después de que pasan las pruebas.
