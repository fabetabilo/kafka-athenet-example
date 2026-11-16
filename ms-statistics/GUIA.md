# Guía de ms-statistics

Líneas clave del consumidor y qué concepto de Kafka muestra cada una.

## Consumer group

[`ScoreListener.java`](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java)

- [L24](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L24): `@KafkaListener` suscribe este servicio al topic `marcador` como parte del grupo `estadisticas`. El grupo es la identidad del lector: Kafka guarda el avance por grupo, no por proceso.
- [L28](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L28): la partición y el offset del mensaje que se está leyendo. Para un mismo encuentro, la partición es siempre la misma y el offset sube de a uno.
- [L34](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L34): cuando el método termina sin error, Spring confirma el offset al broker. Esa confirmación es la **marca** del grupo: el próximo offset que le toca leer.

## Leer no borra

- [L32](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L32): el mensaje se guarda en MongoDB, pero sigue en el topic. Otro grupo podría leerlo desde el principio con su propia marca (fase 6).

## Dónde parte un grupo nuevo

[`application.yml`](src/main/resources/application.yml)

- [L8](src/main/resources/application.yml#L8): `earliest` aplica **solo si el grupo no tiene marca**. La primera vez, `estadisticas` lee todo el topic desde el offset 0. Desde ahí, al reiniciar, retoma desde su marca.

## Deserialización

- [L10](src/main/resources/application.yml#L10): el valor llega como JSON y se convierte a objeto con `JacksonJsonDeserializer`.
- [L13](src/main/resources/application.yml#L13) y [L14](src/main/resources/application.yml#L14): como el JSON no trae el nombre de la clase, se le indica cuál usar: la [`ScoreEvent` propia](src/main/java/cl/athenet/statistics/event/ScoreEvent.java#L4) de este servicio. Los dos microservicios comparten el formato del mensaje, no el código.

## Estado en MongoDB

- [`ScoreDocument.java` L9](src/main/java/cl/athenet/statistics/model/ScoreDocument.java#L9): un documento por encuentro. El `_id` lo genera MongoDB y `idEncuentro` va como campo aparte, porque mongo-express solo abre documentos con `_id` de tipo ObjectId.
- [`ScoreListener.java` L31](src/main/java/cl/athenet/statistics/consumer/ScoreListener.java#L31): si el encuentro ya tiene documento, se reemplaza y queda el último marcador. Por eso no importa si un mensaje se lee dos veces: el resultado es el mismo.
