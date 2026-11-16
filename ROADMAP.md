# ROADMAP: Fases de construcción

Plan para construir la maqueta paso a paso. Cada fase es un commit y agrega su sección en `GUIA.md`.

## Reglas

- Archivos, carpetas y clases en inglés. Variables, campos JSON, endpoints y topics en español.
- Documentación en español correcto. Comentarios en código y mensajes de commit sin tildes ni símbolos propios del español.
- Implementación mínima, sin validaciones innecesarias.
- Un solo encuentro a la vez.
- Cada fase deja el ecosistema levantable y con su prueba. Para volver a una fase: `git checkout fase-N` y `docker compose down -v && docker compose up --build`.

## Componentes

| Carpeta / servicio | Stack maqueta | Representa en Athenet | Infra real |
|---|---|---|---|
| `web-org` | Flask | Web Angular `web-org` (panel del delegado) | GitHub Pages |
| `ms-competitions` | Spring Boot 4.1, Java 21 | `ms-competitions` | AWS EC2 |
| `ms-statistics` | Spring Boot 4.1, Java 21, STOMP | Nuevo: consumidor del marcador | AWS EC2 |
| `web-public` | React + Vite, CSS vanilla | Nueva: marcador público | GitHub Pages |
| `kafka`, `zookeeper`, `kafka-ui` | cp-kafka 7.5.0 | Nuevo | AWS EC2 (autogestionado) |
| `postgres` | PostgreSQL | BD de `ms-competitions` | AWS RDS (en la maqueta, equivale a una EC2 simple) |
| `mongo`, `mongo-express` | MongoDB | Nuevo: BD de `ms-statistics` | AWS EC2 |

Todo se levanta con un solo `docker-compose.yml`.

## Flujo

1. El delegado elige un partido `PROGRAMADO` en `web-org` y lo inicia. `ms-competitions` lo pasa a `EN_CURSO` y publica `INICIADO`.
2. Cada clic en +/- hace un POST a `ms-competitions`, que publica `PUNTO`. PostgreSQL no se toca.
3. Al finalizar, `ms-competitions` guarda el resultado, deja el partido en `FINALIZADO` y publica `FINALIZADO`.
4. `ms-statistics` consume `marcador`, guarda el estado en MongoDB y lo envía por STOMP.
5. `web-public` muestra el marcador en vivo.

**Antes en Athenet:** un solo POST al finalizar. **Ahora:** además, un POST por cada clic.

## Evento

- Topic: `marcador`, 3 particiones.
- Clave: `idEncuentro`. Todos los eventos de un partido caen en la misma partición y en orden.
- Valor: el marcador completo.

```json
{ "idEncuentro": 1, "equipoLocal": "Leones", "equipoVisita": "Halcones",
  "puntosLocal": 2, "puntosVisita": 1, "tipo": "PUNTO" }
```

`tipo`: `INICIADO`, `PUNTO` o `FINALIZADO`.

## Puertos

| Servicio | Puerto |
|---|---|
| Kafka UI | 8080 |
| ms-competitions | 8081 |
| ms-statistics | 8082 |
| mongo-express | 8083 |
| web-org | 5000 |
| web-public | 5173 |
| Kafka | 9092 |
| PostgreSQL | 5432 |
| MongoDB | 27017 |

## Fases

### Fase 1: infraestructura Kafka

- **Construye:** `docker-compose.yml` con ZooKeeper, Kafka y Kafka UI. Además, `.gitignore`, `GUIA.md` y la tabla de componentes en `README.md`.
- **Conceptos:** broker; listener interno (`kafka:29092`) y externo (`localhost:9092`).
- **Se observa:** Kafka UI muestra el broker 1, todavía sin topics.
- **Commit:** `feat(infra): levantar Kafka, ZooKeeper y Kafka UI`

### Fase 2: ms-competitions y PostgreSQL

- **Construye:** PostgreSQL con 3 partidos `PROGRAMADO`. Endpoints `iniciar`, `marcador` y `finalizar`. Creación del topic `marcador` y productor con clave `idEncuentro`.
- **Conceptos:** topic, partición, offset y clave.
- **Se observa:** después de varios `curl`, en Kafka UI todos los mensajes del partido están en la misma partición, con offsets crecientes. Con `psql` se ve el estado del partido.
- **Commit:** `feat(ms-competitions): publicar eventos del marcador en Kafka`

### Fase 3: web-org

- **Construye:** página Flask para elegir un partido programado, iniciarlo, sumar o restar con +/- y finalizarlo. El JS llama directo a `ms-competitions`.
- **Conceptos:** productor detrás de un flujo real; un clic genera un mensaje.
- **Se observa:** cada clic agrega un mensaje en Kafka UI. Un partido nuevo puede caer en otra partición.
- **Commit:** `feat(web-org): panel del delegado con marcador`

### Fase 4: ms-statistics y MongoDB

- **Construye:** `@KafkaListener` en el grupo `estadisticas`, que guarda un documento por `idEncuentro` en MongoDB. Se agrega mongo-express.
- **Conceptos:** consumer group, offset confirmado (marca), `earliest` y retoma al reiniciar. Leer no borra.
- **Se observa:** en Kafka UI → Consumers, el grupo y sus offsets. En mongo-express, el documento. Si se reinicia `ms-statistics`, no pierde ni repite mensajes.
- **Commit:** `feat(ms-statistics): consumir marcador y guardar en MongoDB`

### Fase 5: tiempo real con STOMP y web-public

- **Construye:**
  - En `ms-statistics`: STOMP publicando en `/topic/marcador` y un endpoint que entrega el partido en curso.
  - `web-public` con React, `@stomp/stompjs` y nginx.
- **Conceptos:** Kafka desacopla. `web-public` no conoce Kafka y `ms-competitions` no conoce a sus consumidores.
- **Se observa:** un clic en `web-org` se refleja en `web-public` sin recargar.
- **Commit:** `feat(web-public): marcador en vivo por WebSocket`

### Fase 6: escalamiento

- **Construye:** `concurrency` 1, 3 y 4 en el listener, y un segundo listener en el grupo `auditoria`.
- **Conceptos:**
  - El paralelismo de un grupo llega hasta el número de particiones.
  - Un partido es una clave, y por lo tanto una partición: solo trabaja un hilo.
  - Otro grupo lee todo con su propia marca.
  - Con varias instancias de `ms-statistics`, cada una recibiría solo parte de las particiones y sus clientes STOMP no verían todo.
- **Se observa:** `kafka-consumer-groups --describe --members`.
- **Commit:** `feat(ms-statistics): escalar consumidores y agregar segundo grupo`

### Fase 7: consumer lag

- **Construye:** solo la sección de `GUIA.md`.
- **Conceptos:** `lag = log-end-offset − current-offset`; consumidor caído.
- **Se observa:** con `ms-statistics` detenido en pleno partido, los clics suben el lag en Kafka UI y `web-public` se congela. Al levantarlo, se pone al día.
- **Commit:** `docs(guia): observar consumer lag`

### Fase 8: retención

- **Construye:** retención configurada en el topic `marcador`. Se resetean los offsets del grupo para reconstruir MongoDB desde Kafka.
- **Conceptos:** retención por tiempo y por tamaño; compactación por `idEncuentro` como alternativa.
- **Se observa:** los mensajes siguen en el topic después de leídos. Con MongoDB vacío, al releer el topic queda reconstruido.
- **Commit:** `feat(infra): configurar retencion del topic marcador`

### Fase 9: replicación

- **Construye:** 3 brokers, factor de replicación 3, `min.insync.replicas=2` y `acks=all` en el productor.
- **Conceptos:** líder, réplicas e ISR; tolerancia a fallos.
- **Se observa:** si se bota al broker líder en pleno partido, el marcador sigue funcionando.
- **Commit:** `feat(infra): cluster de 3 brokers con replicacion`
