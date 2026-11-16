# web-org

- **Representa en Athenet:** la web Angular `web-org`, en la parte del panel del delegado
- **Infra real:** GitHub Pages

Panel donde el delegado inicia un partido programado, asigna puntos y lo finaliza. Flask solo sirve la página. En Athenet, las llamadas pasan por un BFF antes de llegar a `ms-competitions`; la maqueta omite el BFF. Las líneas clave están explicadas en [GUIA.md](GUIA.md).

Se levanta con el `docker-compose.yml` de la raíz, en http://localhost:5000.
