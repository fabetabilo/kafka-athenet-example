# web-public

- **Representa en Athenet:** web nueva, marcador público
- **Infra real:** GitHub Pages

Muestra el marcador del partido en curso en tiempo real. Se conecta por STOMP a `ms-statistics`. Creada con `npm create vite@latest web-public -- --template react`, con CSS vanilla. Las líneas clave están explicadas en [GUIA.md](GUIA.md).

Se levanta con el `docker-compose.yml` de la raíz, en http://localhost:5173. Para desarrollo: `npm install` y `npm run dev`.
