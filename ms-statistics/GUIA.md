# Guía de ms-statistics

Líneas clave del consumidor y qué concepto de Kafka muestra cada una.

## Consumer group

[`ScoreListener.java`](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java)

- [L27](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L27): `@KafkaListener` suscribe este servicio al topic `marcador` como parte del grupo `estadisticas`. El grupo es la identidad del lector: Kafka guarda el avance por grupo, no por proceso.
- [L31](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L31): la partición y el offset del mensaje que se está leyendo. Para un mismo encuentro, la partición es siempre la misma y el offset sube de a uno.
- [L39](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L39): cuando el método termina sin error, Spring confirma el offset al broker. Esa confirmación es la **marca** del grupo: el próximo offset que le toca leer.

## Leer no borra

- [L35](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L35): el mensaje se guarda en MongoDB, pero sigue en el topic. Otro grupo podría leerlo desde el principio con su propia marca (fase 6).

## Dónde parte un grupo nuevo

[`application.yml`](src/main/resources/application.yml)

- [L11](src/main/resources/application.yml#L11): `earliest` aplica **solo si el grupo no tiene marca**. La primera vez, `estadisticas` lee todo el topic desde el offset 0. Desde ahí, al reiniciar, retoma desde su marca.

## Deserialización

- [L13](src/main/resources/application.yml#L13): el valor llega como JSON y se convierte a objeto con `JacksonJsonDeserializer`.
- [L16](src/main/resources/application.yml#L16) y [L17](src/main/resources/application.yml#L17): como el JSON no trae el nombre de la clase, se le indica cuál usar: la [`ScoreEvent` propia](src/main/java/cl/athenet/statistics/event/ScoreEvent.java#L4) de este servicio. Los dos microservicios comparten el formato del mensaje, no el código.

## Estado en MongoDB

- [`ScoreDocument.java` L9](src/main/java/cl/athenet/statistics/model/ScoreDocument.java#L9): un documento por encuentro. El `_id` lo genera MongoDB y `idEncuentro` va como campo aparte, porque mongo-express solo abre documentos con `_id` de tipo ObjectId.
- [`ScoreListener.java` L34](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L34): si el encuentro ya tiene documento, se reemplaza y queda el último marcador. Por eso no importa si un mensaje se lee dos veces: el resultado es el mismo.

## Tiempo real con STOMP

- [`ScoreListener.java` L38](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L38): después de guardar, el marcador se envía a `/topic/marcador`. Así, lo que llega por Kafka sale hacia los navegadores.
- [`WebSocketConfig.java` L16](src/main/java/cl/athenet/statistics/config/WebSocketConfig.java#L16): los navegadores se conectan a `ws://localhost:8082/ws`.
- [L22](src/main/java/cl/athenet/statistics/config/WebSocketConfig.java#L22): un broker STOMP en memoria reparte cada mensaje a todos los suscritos. No es Kafka: vive dentro de este servicio.
- [`ScoreController.java` L22](src/main/java/cl/athenet/statistics/controller/ScoreController.java#L22): `GET /marcador/actual` entrega el partido en curso desde MongoDB. Lo usa la página al cargar, porque por STOMP solo llegan los cambios posteriores.

Por esto, en la fase 6 se escala con hilos y no con varias instancias. Cada instancia tiene su propio broker STOMP y recibe solo algunas particiones, así que un navegador conectado a una instancia no vería los mensajes leídos por otra.
