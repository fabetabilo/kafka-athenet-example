# ms-statistics

- **Representa en Athenet:** microservicio nuevo, consumidor del marcador
- **Infra real:** AWS EC2, con MongoDB en EC2

Consume el topic `marcador` en el grupo `estadisticas` y guarda el último marcador de cada encuentro en MongoDB (base `statistics`, colección `marcadores`). Las líneas clave están explicadas en [GUIA.md](GUIA.md).

Se levanta con el `docker-compose.yml` de la raíz. Para correrlo fuera de Docker (con Kafka y MongoDB arriba): `mvn spring-boot:run`.
