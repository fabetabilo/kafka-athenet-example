# Guía de la maqueta

Una sección por fase del [ROADMAP](ROADMAP.md): cómo levantarla, cómo probarla y qué mirar. El detalle línea por línea está en el `GUIA.md` de cada componente.

## Fase 1: infraestructura Kafka

```bash
docker compose up -d
```

Espera unos 30 segundos y abre http://localhost:8080.

**Qué mirar**

- En Kafka UI, el cluster `athenet` con 1 broker y sin topics de la aplicación.
- `docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list` responde vacío.

**En el código**

- [docker-compose.yml L8](docker-compose.yml#L8): el broker.
- [L16](docker-compose.yml#L16): ZooKeeper coordina el cluster. Kafka 4.x lo reemplaza por KRaft.
- [L19](docker-compose.yml#L19): dos direcciones. Los contenedores usan `kafka:29092` y tu PC usa `localhost:9092`.
- [L24](docker-compose.yml#L24): Kafka no crea topics solo. Se crean desde el código, con sus particiones.

**Athenet:** Kafka sería un servicio autogestionado en una EC2.

## Fase 2: ms-competitions y PostgreSQL

```bash
docker compose up -d --build
docker compose logs -f ms-competitions
```

**Probar**

```bash
curl http://localhost:8081/encuentros
curl -X POST http://localhost:8081/encuentros/1/iniciar
curl -X POST http://localhost:8081/encuentros/1/marcador -H "Content-Type: application/json" -d '{"puntosLocal":1,"puntosVisita":0}'
curl -X POST http://localhost:8081/encuentros/1/marcador -H "Content-Type: application/json" -d '{"puntosLocal":1,"puntosVisita":1}'
curl -X POST http://localhost:8081/encuentros/1/finalizar -H "Content-Type: application/json" -d '{"puntosLocal":2,"puntosVisita":1}'
```

Después repite con el encuentro 2.

**Qué mirar**

- En el log, una línea `[PUBLICA]` por llamada, con su partición y su offset.
- En Kafka UI → Topics → `marcador` → Messages: los 4 mensajes del encuentro 1 están en una misma partición, con offsets 0 a 3. El encuentro 2 puede caer en otra partición, donde su offset parte de 0.
- En PostgreSQL, el encuentro 1 aparece `FINALIZADO` con 2-1. Los clics intermedios no tocaron la tabla.

```bash
docker compose exec postgres psql -U athenet -d competitions -c "select * from encuentros;"
```

**En el código:** [ms-competitions/GUIA.md](ms-competitions/GUIA.md) (topic, particiones, clave, offset y serialización).

**Athenet:** antes había un solo POST al finalizar. Ahora cada clic en +/- también hace un POST, que publica en `marcador` sin escribir en la BD.

**Reiniciar desde cero:** `docker compose down -v` borra el topic y la base. Al volver a levantar, el script de los partidos programados corre de nuevo.

## Fase 3: web-org

```bash
docker compose up -d --build
docker compose logs -f ms-competitions
```

**Probar**

1. Abre http://localhost:5000, elige un partido programado y presiona **Iniciar**.
2. Presiona + y - varias veces.
3. Presiona **Finalizar partido**. Vuelve la lista sin ese partido.
4. Inicia otro partido.

**Qué mirar**

- Cada clic genera una línea `[PUBLICA]` en el log y un mensaje nuevo en Kafka UI → `marcador`, con el offset siguiente dentro de la misma partición.
- El segundo partido puede caer en otra partición, porque tiene otra clave.
- En la pestaña Network del navegador, cada clic es un `POST /encuentros/{id}/marcador`.

**En el código:** [web-org/GUIA.md](web-org/GUIA.md).

**Athenet:** la pantalla del delegado es la misma. La diferencia es que cada clic ahora sale del navegador.

## Fase 4: ms-statistics y MongoDB

```bash
docker compose up -d --build
docker compose logs -f ms-statistics
```

**Probar**

1. En http://localhost:5000, inicia un partido y haz algunos clics.
2. Detén el consumidor: `docker compose stop ms-statistics`.
3. Haz 3 clics más.
4. Levántalo de nuevo: `docker compose start ms-statistics`.

**Qué mirar**

- Apenas arranca, `ms-statistics` lee todos los mensajes que ya existían en el topic, incluidos los de fases anteriores. Cada uno deja una línea `[LEE]` con su partición y su offset.
- Al volver a levantarlo (paso 4), lee solo los 3 clics que se hicieron mientras estaba detenido. No repite los anteriores.
- En mongo-express (http://localhost:8083) → `statistics` → `marcadores`: un documento por encuentro con el último marcador.
- La marca del grupo. `CURRENT-OFFSET` es el próximo mensaje que le toca leer:

```bash
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group estadisticas
```

También se ve en Kafka UI → Consumers → `estadisticas`.

**En el código:** [ms-statistics/GUIA.md](ms-statistics/GUIA.md) (consumer group, marca, `earliest` y deserialización).

**Athenet:** `ms-competitions` no sabe que existe `ms-statistics`. Kafka los desacopla: el productor publica y cada consumidor lee a su ritmo.

## Fase 5: tiempo real con STOMP y web-public

```bash
docker compose up -d --build
```

**Probar**

1. Abre http://localhost:5173 (marcador público) y http://localhost:5000 (delegado) lado a lado.
2. En el panel del delegado, inicia un partido. El marcador público lo muestra sin recargar.
3. Haz clics en +/-. Cada uno se refleja en el marcador público.
4. Recarga el marcador público: vuelve con el marcador actual.
5. Finaliza el partido. El marcador público muestra "Finalizado".

**Qué mirar**

- El recorrido de un clic: POST a `ms-competitions`, mensaje en Kafka (`[PUBLICA]`), lectura en `ms-statistics` (`[LEE]`), documento en MongoDB y envío por STOMP al navegador.
- En el navegador, DevTools → Network → WS → `ws`: los frames STOMP que llegan con cada clic.

**En el código:** [ms-statistics/GUIA.md](ms-statistics/GUIA.md#tiempo-real-con-stomp) y [web-public/GUIA.md](web-public/GUIA.md).

**Athenet:** el delegado ya no es el único que ve el marcador mientras se juega. `web-public` no sabe que existe Kafka, y `ms-competitions` no sabe que existe `web-public`.

## Fase 6: escalamiento

```bash
docker compose up -d --build
docker compose logs -f ms-statistics
```

**Probar**

1. Con `ESTADISTICAS_HILOS: 1` en `docker-compose.yml`, revisa los miembros del grupo:

```bash
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group estadisticas --members
```

2. Cambia a `ESTADISTICAS_HILOS: 3`, aplica con `docker compose up -d ms-statistics` y repite el comando.
3. Cambia a `4`, aplica y repite.
4. Con 3 hilos, juega un partido y luego otro.
5. Revisa el segundo grupo: `--describe --group auditoria`.

**Qué mirar**

| Hilos | Miembros | Particiones por miembro |
|---|---|---|
| 1 | 1 | 3 |
| 3 | 3 | 1 cada uno |
| 4 | 4 | uno queda con 0 |

- En el log, el nombre del hilo aparece entre corchetes (`...-0-C-1`, `...-1-C-1`). Todos los `[LEE]` de un partido salen del mismo hilo. Otro partido puede salir de otro hilo, si cayó en otra partición.
- Cada clic deja una línea `[LEE]` y una `[AUDITORIA]`: los dos grupos leen el mismo mensaje.
- En Kafka UI → Consumers aparecen `estadisticas` y `auditoria`, cada uno con sus propios offsets.

**En el código:** [ms-statistics/GUIA.md](ms-statistics/GUIA.md#escalamiento).

**Athenet:** con varias instancias de `ms-statistics` (por ejemplo, varias EC2), cada una recibiría solo algunas particiones. Un navegador conectado por STOMP a una instancia no vería los partidos leídos por otra. Para escalar horizontalmente habría que separar la lectura de Kafka del envío por WebSocket.
