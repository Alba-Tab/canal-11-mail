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

## Configurar y correr

```bash
cp config.example.properties config.properties
```

Completar `config.properties` con las contraseñas del buzón y de la base de datos. No se versiona; en el servidor, darle permisos `600`.

```bash
java -Duser.timezone=America/La_Paz -jar target/canal11-mail.jar config.properties
```

El log va a la consola y a `logs/canal11.0.log`.

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
    ├── negocio/         CommandParser, CommandProcessor, HTMLResponseBuilder y catálogo de comandos
    └── datos/           DBConnection, repositorio y traductor de errores
```

## Cómo se trabaja

- `main` recibe solo las entregas.
- Se trabaja en ramas `feat/`, `fix/`, `chore/` o `docs/` que salen de `develop`.
- Cada rama vuelve a `develop` con `git merge --no-ff`, después de que pasan las pruebas.
