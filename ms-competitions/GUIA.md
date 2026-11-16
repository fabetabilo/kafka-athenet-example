# Guía de ms-competitions

Líneas clave del servicio y qué concepto de Kafka muestra cada una.

## Topic y particiones

[`KafkaConfig.java`](src/main/java/cl/athenet/competitions/config/KafkaConfig.java)

- [L11](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L11): el topic se llama `marcador`.
- [L15](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L15): 3 particiones y 1 réplica. Spring crea el topic al arrancar, porque es un bean `NewTopic`. Tiene 1 réplica porque hay un solo broker; en la fase 9 sube a 3.

## Productor

[`MatchService.java`](src/main/java/cl/athenet/competitions/service/MatchService.java)

- [L22](src/main/java/cl/athenet/competitions/service/MatchService.java#L22): `KafkaTemplate<String, ScoreEvent>` es el productor. La clave es texto y el valor es el evento.
- [L61](src/main/java/cl/athenet/competitions/service/MatchService.java#L61): la clave es el `idEncuentro`. Kafka calcula un hash de ella para elegir la partición, así que el mismo encuentro siempre cae en la misma partición y sus eventos quedan en orden.
- [L62](src/main/java/cl/athenet/competitions/service/MatchService.java#L62): `send(topic, clave, evento)` publica. Es asíncrono: responde cuando Kafka confirma.
- [L65](src/main/java/cl/athenet/competitions/service/MatchService.java#L65) y [L66](src/main/java/cl/athenet/competitions/service/MatchService.java#L66): la partición y el offset que asignó Kafka. El offset es la posición del mensaje dentro de su partición.

## Relación con Athenet

- [L42](src/main/java/cl/athenet/competitions/service/MatchService.java#L42): cada clic del delegado publica `PUNTO` y no escribe en la BD. Antes no existía: solo había un POST al finalizar.
- [L35](src/main/java/cl/athenet/competitions/service/MatchService.java#L35): iniciar pasa el partido de `PROGRAMADO` a `EN_CURSO`.
- [L52](src/main/java/cl/athenet/competitions/service/MatchService.java#L52): al finalizar se guarda el resultado, como antes, y además se publica `FINALIZADO`.
- [`MatchController.java` L37](src/main/java/cl/athenet/competitions/controller/MatchController.java#L37): el endpoint de cada clic. Responde `202 Accepted` ([L40](src/main/java/cl/athenet/competitions/controller/MatchController.java#L40)).

## Evento

- [`ScoreEvent.java` L4](src/main/java/cl/athenet/competitions/event/ScoreEvent.java#L4): lo que viaja por Kafka. Lleva el marcador completo y también el `idEncuentro`, para que el consumidor sepa de qué partido es.

## Conexión y serialización

[`application.yml`](src/main/resources/application.yml)

- [L13](src/main/resources/application.yml#L13): broker `localhost:9092` cuando corre fuera de Docker. En Docker se reemplaza por `kafka:29092` ([`docker-compose.yml` L68](../docker-compose.yml#L68)).
- [L15](src/main/resources/application.yml#L15): la clave se serializa como texto.
- [L16](src/main/resources/application.yml#L16): el valor se serializa como JSON con `JacksonJsonSerializer`, que es el nombre en Spring Boot 4.
- [L19](src/main/resources/application.yml#L19): sin headers de tipo. El JSON no lleva el nombre de la clase Java, así que otro microservicio puede leerlo con su propia clase.

## Partidos programados

- [`db/init.sql` L10](db/init.sql#L10): los 3 partidos `PROGRAMADO`. PostgreSQL ejecuta este script solo cuando la base está vacía.
