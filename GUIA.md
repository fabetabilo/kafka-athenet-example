# Guía de la maqueta

Cada sección corresponde a una fase del [ROADMAP](ROADMAP.md). Las referencias `archivo:línea` apuntan al código de esa fase.

## Fase 1: infraestructura Kafka

**Levantar**

```bash
docker compose up -d
```

Espera unos 30 segundos y abre http://localhost:8080.

**Qué mirar**

- En Kafka UI, el cluster `athenet` con 1 broker y sin topics de la aplicación.
- Desde la terminal, el broker responde y no hay topics:

```bash
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Conceptos**

| Concepto | Dónde | Qué significa |
|---|---|---|
| Broker | `docker-compose.yml:8` | El servidor de Kafka. Guarda los topics y atiende a productores y consumidores. |
| ZooKeeper | `docker-compose.yml:2` y `:16` | Coordina el cluster en esta versión de Kafka. Kafka 4.x lo reemplaza por KRaft. |
| Listener interno | `docker-compose.yml:19` | `kafka:29092`, la dirección que usan los contenedores (Kafka UI y, más adelante, los microservicios). |
| Listener externo | `docker-compose.yml:19` | `localhost:9092`, la dirección que se usa desde tu PC. |
| Topics explícitos | `docker-compose.yml:24` | Kafka no crea topics solo. Se crean desde el código, con particiones definidas. |
| Kafka UI | `docker-compose.yml:26` | Herramienta para ver topics, particiones, offsets y consumidores. |

**Relación con Athenet**

En producción, Kafka sería un servicio autogestionado en una instancia EC2. Los microservicios en EC2 se conectarían por su dirección interna, igual que aquí usan `kafka:29092`.

**Apagar**

```bash
docker compose down
```
