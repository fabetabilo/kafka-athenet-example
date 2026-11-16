# ms-competitions

- **Representa en Athenet:** `ms-competitions`
- **Infra real:** AWS EC2, con PostgreSQL en RDS

Administra los encuentros y publica cada cambio de marcador en el topic `marcador`. Las líneas clave están explicadas en [GUIA.md](GUIA.md).

| Método | Endpoint | Efecto |
|---|---|---|
| GET | `/encuentros` | Lista los encuentros |
| POST | `/encuentros/{id}/iniciar` | `PROGRAMADO` → `EN_CURSO` y publica `INICIADO` |
| POST | `/encuentros/{id}/marcador` | Publica `PUNTO`. No escribe en la BD |
| POST | `/encuentros/{id}/finalizar` | Guarda el resultado, pasa a `FINALIZADO` y publica `FINALIZADO` |

`marcador` y `finalizar` reciben `{"puntosLocal": 1, "puntosVisita": 0}`.

Se levanta con el `docker-compose.yml` de la raíz. Para correrlo fuera de Docker (con Kafka y PostgreSQL arriba): `mvn spring-boot:run`.
