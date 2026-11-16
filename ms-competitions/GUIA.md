# Guía de ms-competitions

Líneas clave del servicio y qué concepto de Kafka muestra cada una.

## Topic y particiones

[`KafkaConfig.java`](src/main/java/cl/athenet/competitions/config/KafkaConfig.java)

- [L14](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L14): el topic se llama `marcador`.
- [L19](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L19): 3 particiones y 3 réplicas. Spring crea el topic al arrancar, porque es un bean `NewTopic`.

## Retención

- [L21](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L21): la política es `delete`. Kafka borra mensajes por antigüedad, nunca porque alguien los leyó.
- [L23](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L23): los mensajes viven 7 días. Mientras sigan en el topic, cualquier grupo puede releerlos.
- La alternativa es `compact`: de cada clave deja solo el último valor. Para `marcador` dejaría solo el marcador final de cada `idEncuentro`, que es justo lo que guarda MongoDB, pero se perdería la secuencia de clics.

## Productor

[`MatchService.java`](src/main/java/cl/athenet/competitions/service/MatchService.java)

- [L22](src/main/java/cl/athenet/competitions/service/MatchService.java#L22): `KafkaTemplate<String, ScoreEvent>` es el productor. La clave es texto y el valor es el evento.
- [L61](src/main/java/cl/athenet/competitions/service/MatchService.java#L61): la clave es el `idEncuentro`. Kafka calcula un hash de ella para elegir la partición, así que el mismo encuentro siempre cae en la misma partición y sus eventos quedan en orden.
- [L62](src/main/java/cl/athenet/competitions/service/MatchService.java#L62): `send(topic, clave, evento)` publica. Es asíncrono: responde cuando Kafka confirma.
- [L70](src/main/java/cl/athenet/competitions/service/MatchService.java#L70) y [L71](src/main/java/cl/athenet/competitions/service/MatchService.java#L71): la partición y el offset que asignó Kafka. El offset es la posición del mensaje dentro de su partición.

## Relación con Athenet

- [L42](src/main/java/cl/athenet/competitions/service/MatchService.java#L42): cada clic del delegado publica `PUNTO` y no escribe en la BD. Antes no existía: solo había un POST al finalizar.
- [L35](src/main/java/cl/athenet/competitions/service/MatchService.java#L35): iniciar pasa el partido de `PROGRAMADO` a `EN_CURSO`.
- [L52](src/main/java/cl/athenet/competitions/service/MatchService.java#L52): al finalizar se guarda el resultado, como antes, y además se publica `FINALIZADO`.
- [`MatchController.java` L40](src/main/java/cl/athenet/competitions/controller/MatchController.java#L40): el endpoint de cada clic. Responde `202 Accepted` ([L43](src/main/java/cl/athenet/competitions/controller/MatchController.java#L43)).
- [`MatchController.java` L19](src/main/java/cl/athenet/competitions/controller/MatchController.java#L19): `@CrossOrigin` deja que el navegador llame a este servicio. En Athenet no hace falta, porque quien llama a `ms-competitions` es el BFF.

## Evento

- [`ScoreEvent.java` L4](src/main/java/cl/athenet/competitions/event/ScoreEvent.java#L4): lo que viaja por Kafka. Lleva el marcador completo y también el `idEncuentro`, para que el consumidor sepa de qué partido es.

## Conexión y serialización

[`application.yml`](src/main/resources/application.yml)

- [L13](src/main/resources/application.yml#L13): broker `localhost:9092` cuando corre fuera de Docker. En Docker se reemplaza por `kafka:29092` ([`docker-compose.yml` L124](../docker-compose.yml#L124)).
- [L17](src/main/resources/application.yml#L17): la clave se serializa como texto.
- [L18](src/main/resources/application.yml#L18): el valor se serializa como JSON con `JacksonJsonSerializer`, que es el nombre en Spring Boot 4.
- [L21](src/main/resources/application.yml#L21): sin headers de tipo. El JSON no lleva el nombre de la clase Java, así que otro microservicio puede leerlo con su propia clase.

## Partidos programados

- [`db/init.sql` L10](db/init.sql#L10): los 3 partidos `PROGRAMADO`. PostgreSQL ejecuta este script solo cuando la base está vacía.

## Replicación

- [`KafkaConfig.java` L19](src/main/java/cl/athenet/competitions/config/KafkaConfig.java#L19): cada partición tiene 3 copias en brokers distintos. Una es el **líder**, que recibe las escrituras y lecturas; las otras dos son seguidoras y copian al líder. Las réplicas que están al día forman el **ISR**.
- [`application.yml` L16](src/main/resources/application.yml#L16): `acks: all`. El broker confirma el mensaje cuando lo tienen todas las réplicas del ISR, no solo el líder.
- `min.insync.replicas=2`, en [`docker-compose.yml` L28](../docker-compose.yml#L28): si el ISR baja de 2, el broker rechaza la escritura en vez de aceptarla con una sola copia.
- [`MatchService.java` L65](src/main/java/cl/athenet/competitions/service/MatchService.java#L65): ese rechazo aparece como `[ERROR]` en el log.
- [`docker-compose.yml` L124](../docker-compose.yml#L124): el servicio conoce los 3 brokers. Si uno cae, se conecta por cualquiera de los otros.
