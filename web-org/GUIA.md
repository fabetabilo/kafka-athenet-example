# Guía de web-org

Este componente no usa Kafka directamente. Es el origen de los eventos: cada acción del delegado termina como un mensaje en el topic `marcador`.

## Conexión con ms-competitions

- [`app.py` L8](app.py#L8): la URL a la que llama la página. En Athenet, `web-org` llama a un BFF, y el BFF llama a `ms-competitions`. La maqueta omite el BFF y apunta a `ms-competitions`. Es `localhost:8081` porque la llamada sale del navegador, no del contenedor.
- [`templates/index.html` L41](templates/index.html#L41): Flask inyecta esa URL en la página.

## Flujo del delegado

- [L51](templates/index.html#L51): solo se listan los partidos `PROGRAMADO`.
- [L58](templates/index.html#L58): **Iniciar**. `ms-competitions` pasa el partido a `EN_CURSO` y publica `INICIADO`.
- [L69](templates/index.html#L69): cada clic en + o - actualiza el marcador en pantalla.
- [L73](templates/index.html#L73): y además lo envía. **Este es el cambio respecto de Athenet:** antes el clic no salía del navegador; ahora cada uno termina como un evento `PUNTO` en Kafka.
- [L77](templates/index.html#L77): **Finalizar**. Es el único POST que ya existía en Athenet. Ahora, además de guardar el resultado, publica `FINALIZADO`.
- [L82](templates/index.html#L82): siempre se envía el marcador completo, no "+1". Así, el último mensaje de la partición siempre tiene el marcador correcto.
