# ms-statistics

- **Representa en Athenet:** microservicio nuevo, consumidor del marcador
- **Infra real:** AWS EC2, con MongoDB en EC2

Consume el topic `marcador` en el grupo `estadisticas`, guarda el último marcador de cada encuentro en MongoDB (base `statistics`, colección `marcadores`) y lo envía por STOMP a `/topic/marcador`.

| Endpoint | Para qué |
|---|---|
| `GET /marcador/actual` | Partido en curso (204 si no hay) |
| `ws://localhost:8082/ws` | Conexión STOMP | Las líneas clave están explicadas en [GUIA.md](GUIA.md).

Se levanta con el `docker-compose.yml` de la raíz. Para correrlo fuera de Docker (con Kafka y MongoDB arriba): `mvn spring-boot:run`.
