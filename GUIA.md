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

## Fase 7: consumer lag

```bash
docker compose up -d --build
```

**Probar**

1. Inicia un partido en http://localhost:5000 y deja http://localhost:5173 abierto.
2. Revisa el lag:

```bash
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group estadisticas
```

3. Detén el consumidor: `docker compose stop ms-statistics`.
4. Haz 5 clics en el panel del delegado y repite el comando del paso 2.
5. Levántalo: `docker compose start ms-statistics`. Repite el comando.

**Qué mirar**

| Momento | LAG de la partición del partido | web-public |
|---|---|---|
| Consumidor arriba | 0 | Al día |
| Consumidor detenido, después de 5 clics | 5; `CURRENT-OFFSET` no se mueve | "Desconectado", con el marcador congelado |
| Consumidor de nuevo arriba | Vuelve a 0 | Se reconecta y muestra el marcador actual |

- Mientras el consumidor está detenido, `ms-competitions` sigue publicando sin errores. Kafka guarda los mensajes y el productor no depende del consumidor.
- Al volver, el log muestra los 5 `[LEE]` pendientes seguidos.
- En Kafka UI → Consumers → `estadisticas` se ve el mismo lag. `auditoria` también acumula, porque vive en el mismo servicio.

**En el código:** [ms-statistics/GUIA.md](ms-statistics/GUIA.md#consumer-lag) y [web-public/GUIA.md](web-public/GUIA.md).

**Athenet:** si `ms-statistics` se cae en pleno partido, el delegado puede seguir anotando puntos y no se pierde ninguno. El marcador público se atrasa, y al volver el servicio se pone al día solo. En producción, el lag es la métrica que avisa de ese atraso.

## Fase 8: retención

El topic cambia de configuración, así que hay que recrearlo:

```bash
docker compose down -v
docker compose up -d --build
```

Juega un partido completo antes de seguir.

**Probar 1: leer no borra**

```bash
docker compose exec kafka kafka-configs --bootstrap-server localhost:9092 --describe --entity-type topics --entity-name marcador
```

Muestra `cleanup.policy=delete` y `retention.ms=604800000` (7 días). En Kafka UI, los mensajes del partido siguen en el topic aunque `estadisticas` y `auditoria` ya los leyeron.

**Probar 2: reconstruir MongoDB desde Kafka**

1. Detén el consumidor: `docker compose stop ms-statistics`.
2. Borra la colección:

```bash
docker compose exec mongo mongosh statistics --eval "db.marcadores.drop()"
```

3. Mueve la marca del grupo al principio:

```bash
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group estadisticas --topic marcador --reset-offsets --to-earliest --execute
```

4. Levántalo: `docker compose start ms-statistics`.

El log muestra todos los `[LEE]` desde el offset 0, y en mongo-express vuelve a aparecer el documento con el marcador final. MongoDB se reconstruyó solo con lo que había en Kafka.

**Probar 3: ver la retención borrar**

1. Baja la retención a 1 minuto y cierra un segmento cada 10 segundos:

```bash
docker compose exec kafka kafka-configs --bootstrap-server localhost:9092 --alter --entity-type topics --entity-name marcador --add-config retention.ms=60000,segment.ms=10000
```

2. Espera 15 segundos, inicia otro partido y haz un clic, para que se abra un segmento nuevo.
3. Espera unos 2 minutos y mira Kafka UI → `marcador` → Messages.
4. Restaura la configuración:

```bash
docker compose exec kafka kafka-configs --bootstrap-server localhost:9092 --alter --entity-type topics --entity-name marcador --delete-config retention.ms,segment.ms
```

**Qué mirar**

- Los mensajes viejos desaparecen aunque nadie los haya pedido borrar. Los offsets no vuelven a 0: la partición ahora empieza en un offset mayor.
- Si repites la reconstrucción (Probar 2), solo se recupera lo que sobrevivió. La retención define hasta dónde se puede releer.

**En el código:** [ms-competitions/GUIA.md](ms-competitions/GUIA.md#retención).

**Athenet:** con 7 días de retención, si MongoDB se pierde, el estado de los partidos de la última semana se puede reconstruir desde Kafka sin tocar `ms-competitions`.

## Fase 9: replicación

Ahora hay 3 brokers y el topic se crea con 3 réplicas, así que hay que partir de cero:

```bash
docker compose down -v
docker compose up -d --build
```

**Probar**

1. Inicia un partido y haz un clic. Anota su partición en el log de `ms-competitions` (`[PUBLICA] ... particion=N`).
2. Mira quién lidera cada partición:

```bash
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic marcador
```

3. Detén el broker líder de esa partición. Por ejemplo, si es el 2: `docker compose stop kafka-2`. Los brokers 1, 2 y 3 son los servicios `kafka`, `kafka-2` y `kafka-3`.
4. Repite el paso 2. Si detuviste `kafka`, ejecuta el comando en `kafka-2` con `--bootstrap-server localhost:9093`.
5. Haz clics. El marcador público sigue funcionando.
6. Detén un segundo broker y haz un clic.
7. Levanta los dos brokers con `docker compose start` y repite el paso 2.

**Qué mirar**

| Brokers arriba | Líder | ISR | Clics |
|---|---|---|---|
| 3 | uno por partición, repartidos | 3 réplicas | Funcionan |
| 2 | otro broker toma el liderazgo | 2 réplicas | Funcionan |
| 1 | el que queda | 1 réplica | Fallan: `[ERROR]` en el log, por `min.insync.replicas=2` |
| 3 de nuevo | se mantiene el nuevo | vuelve a 3 | Funcionan |

- Con un broker caído no se pierde ningún mensaje: las otras réplicas ya los tenían.
- Con solo uno arriba, Kafka prefiere rechazar antes que guardar una sola copia. El productor reintenta hasta 2 minutos (`delivery.timeout.ms`) antes de mostrar el `[ERROR]`. Si levantas un broker antes de ese plazo, el mensaje se publica igual.
- En Kafka UI → Brokers se ven los 3 brokers, y en Topics → `marcador` el líder y las réplicas de cada partición.

**En el código:** [ms-competitions/GUIA.md](ms-competitions/GUIA.md#replicación).

**Athenet:** con Kafka autogestionado, serían 3 instancias EC2, idealmente en zonas de disponibilidad distintas. Si una cae, los partidos siguen. Con una sola EC2 (hasta la fase 8), caída del broker es caída del marcador en vivo.
