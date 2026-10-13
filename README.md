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

En desarrollo. El avance por rama está en [ROADMAP.md](ROADMAP.md) y los cambios en [CHANGELOG.md](CHANGELOG.md).

## Requisitos

| Herramienta | Para qué |
|---|---|
| JDK 17 o superior | Compilar y correr |
| PostgreSQL 11 o superior | Base de datos (en el servidor: `db_grupo14sa`) |
| Acceso SSH a `www.tecnoweb.org.bo` | Subir y correr la aplicación |

Maven no hace falta: el repositorio trae el Maven Wrapper (`./mvnw`).

## Estructura

```
canal11-mail/
├── CLAUDE.md            reglas del proyecto (git, código, seguridad)
├── ROADMAP.md           ramas en orden de prioridad
├── CHANGELOG.md         cambios por versión
├── docs/DECISIONES.md   decisiones técnicas y pendientes
├── db/                  script de la BD, datos de prueba, consultas y pruebas de integridad
└── src/                 código Java (Presentación, Negocio y Datos)
```

## Cómo se trabaja

El flujo de git y las reglas de código están en [CLAUDE.md](CLAUDE.md). En resumen:
- `main` recibe solo las entregas.
- Se trabaja en ramas `feat/`, `fix/`, `chore/` o `docs/` que salen de `develop`.
- Cada rama vuelve a `develop` con `git merge --no-ff`, después de que pasan las pruebas.
