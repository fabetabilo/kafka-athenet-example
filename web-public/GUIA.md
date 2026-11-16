# Guía de web-public

Este componente no usa Kafka. Recibe por STOMP lo que `ms-statistics` leyó de Kafka.

## Conexión, estado y cambios

- [`App.jsx` L14](src/App.jsx#L14): se conecta por WebSocket a `ws://localhost:8082/ws`. Si la conexión se cae, `@stomp/stompjs` reintenta solo.
- [L18](src/App.jsx#L18): se suscribe a `/topic/marcador`. Cada mensaje trae el marcador completo y reemplaza el anterior.
- [L20](src/App.jsx#L20): en cada conexión o reconexión pide el partido en curso a `ms-statistics`, que lo lee desde MongoDB. Por STOMP solo llegan los cambios, así que sin esto la página quedaría vacía al recargar y desactualizada al reconectar (fase 7).
- [L37](src/App.jsx#L37): `FINALIZADO` cambia la etiqueta de "En vivo" a "Finalizado".

## URLs

- [L5](src/App.jsx#L5) y [L6](src/App.jsx#L6): apuntan a `localhost:8082` porque la conexión sale del navegador, no del contenedor.

## Despliegue

- [`Dockerfile` L9](Dockerfile#L9): se compila a archivos estáticos y los sirve nginx. En Athenet, el mismo `dist/` se publicaría en GitHub Pages.
