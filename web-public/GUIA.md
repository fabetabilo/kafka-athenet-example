# Guía de web-public

Este componente no usa Kafka. Recibe por STOMP lo que `ms-statistics` leyó de Kafka.

## Estado inicial y cambios

- [`App.jsx` L14](src/App.jsx#L14): al cargar, pide el partido en curso a `ms-statistics`, que lo lee desde MongoDB. Sin esto, al recargar la página el marcador quedaría vacío hasta el próximo clic.
- [L19](src/App.jsx#L19): se conecta por WebSocket a `ws://localhost:8082/ws`.
- [L23](src/App.jsx#L23): se suscribe a `/topic/marcador`. Cada mensaje trae el marcador completo y reemplaza el anterior.
- [L38](src/App.jsx#L38): `FINALIZADO` cambia la etiqueta de "En vivo" a "Finalizado".

## URLs

- [L5](src/App.jsx#L5) y [L6](src/App.jsx#L6): apuntan a `localhost:8082` porque la conexión sale del navegador, no del contenedor.

## Despliegue

- [`Dockerfile` L9](Dockerfile#L9): se compila a archivos estáticos y los sirve nginx. En Athenet, el mismo `dist/` se publicaría en GitHub Pages.
