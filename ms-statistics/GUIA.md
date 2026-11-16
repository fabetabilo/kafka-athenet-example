# Guía de ms-statistics

Líneas clave del consumidor y qué concepto de Kafka muestra cada una.

## Consumer group

[`ScoreListener.java`](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java)

- [L28](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L28): `@KafkaListener` suscribe este servicio al topic `marcador` como parte del grupo `estadisticas`. El grupo es la identidad del lector: Kafka guarda el avance por grupo, no por proceso.
- [L32](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L32): la partición y el offset del mensaje que se está leyendo. Para un mismo encuentro, la partición es siempre la misma y el offset sube de a uno.
- [L40](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L40): cuando el método termina sin error, Spring confirma el offset al broker. Esa confirmación es la **marca** del grupo: el próximo offset que le toca leer.

## Leer no borra

- [L36](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L36): el mensaje se guarda en MongoDB, pero sigue en el topic. Otro grupo podría leerlo desde el principio con su propia marca (fase 6).

## Dónde parte un grupo nuevo

[`application.yml`](src/main/resources/application.yml)

- [L11](src/main/resources/application.yml#L11): `earliest` aplica **solo si el grupo no tiene marca**. La primera vez, `estadisticas` lee todo el topic desde el offset 0. Desde ahí, al reiniciar, retoma desde su marca.

## Deserialización

- [L13](src/main/resources/application.yml#L13): el valor llega como JSON y se convierte a objeto con `JacksonJsonDeserializer`.
- [L16](src/main/resources/application.yml#L16) y [L17](src/main/resources/application.yml#L17): como el JSON no trae el nombre de la clase, se le indica cuál usar: la [`ScoreEvent` propia](src/main/java/cl/athenet/statistics/event/ScoreEvent.java#L4) de este servicio. Los dos microservicios comparten el formato del mensaje, no el código.

## Estado en MongoDB

- [`ScoreDocument.java` L9](src/main/java/cl/athenet/statistics/model/ScoreDocument.java#L9): un documento por encuentro. El `_id` lo genera MongoDB y `idEncuentro` va como campo aparte, porque mongo-express solo abre documentos con `_id` de tipo ObjectId.
- [`ScoreListener.java` L35](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L35): si el encuentro ya tiene documento, se reemplaza y queda el último marcador. Por eso no importa si un mensaje se lee dos veces: el resultado es el mismo.

## Tiempo real con STOMP

- [`ScoreListener.java` L39](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L39): después de guardar, el marcador se envía a `/topic/marcador`. Así, lo que llega por Kafka sale hacia los navegadores.
- [`WebSocketConfig.java` L16](src/main/java/cl/athenet/statistics/config/WebSocketConfig.java#L16): los navegadores se conectan a `ws://localhost:8082/ws`.
- [L22](src/main/java/cl/athenet/statistics/config/WebSocketConfig.java#L22): un broker STOMP en memoria reparte cada mensaje a todos los suscritos. No es Kafka: vive dentro de este servicio.
- [`ScoreController.java` L22](src/main/java/cl/athenet/statistics/controller/ScoreController.java#L22): `GET /marcador/actual` entrega el partido en curso desde MongoDB. Lo usa la página al cargar, porque por STOMP solo llegan los cambios posteriores.

Por esto, en la fase 6 se escala con hilos dentro de una instancia y no con varias instancias. Cada instancia tiene su propio broker STOMP y recibe solo algunas particiones, así que un navegador conectado a una instancia no vería los mensajes leídos por otra.

## Escalamiento

- [`ScoreListener.java` L28](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L28): `concurrency` define cuántos consumidores del grupo `estadisticas` corren en esta instancia. Se ajusta con `ESTADISTICAS_HILOS` en [`docker-compose.yml` L107](../docker-compose.yml#L107).
- Kafka reparte las particiones entre los consumidores del grupo, y cada partición la lee uno solo:
  - 1 hilo: lee las 3 particiones.
  - 3 hilos: una partición cada uno. Es el máximo útil, porque el topic tiene 3 particiones.
  - 4 hilos: uno queda sin partición, ocioso.
- Un partido es una clave, y por lo tanto una partición. Aunque haya 3 hilos, los eventos de un mismo partido los procesa siempre el mismo. Más hilos ayudan cuando hay varios partidos repartidos en distintas particiones.

## Segundo grupo

- [`AuditListener.java` L17](src/main/java/cl/athenet/statistics/consumer/AuditListener.java#L17): otro grupo, `auditoria`, sobre el mismo topic. Recibe **todos** los mensajes, igual que `estadisticas`, y lleva su propia marca. Al arrancar por primera vez lee el historial completo desde el offset 0.
- Agregar un grupo no hace más rápido a `estadisticas`. Escalar un grupo es sumarle consumidores, hasta el número de particiones.

## Consumer lag

- `lag = log-end-offset − current-offset`. El `log-end-offset` es el próximo offset libre de la partición; el `current-offset` es la marca que se confirma en [`ScoreListener.java` L40](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L40).
- Si este servicio se detiene, la marca no avanza y el lag crece con cada clic. Al volver, lee lo pendiente desde su marca y el lag vuelve a 0. `earliest` no interviene, porque el grupo ya tiene marca.
- `estadisticas` y `auditoria` viven en este mismo servicio, así que los dos grupos acumulan lag a la vez.
