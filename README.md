## Maqueta Athenet Kafka para el streaming de datos

Este proyecto tiene el objetivo de estudiar y comprender el uso de Kafka a través del flujo 
que usa Athenet para encuentros entre dos equipos en vivo. 

El objetivo es mostrar el marcador cambiando en tiempo real en una web mediante websocket.

## Componentes

| Servicio | Representa en Athenet | Infra real |
|---|---|---|
| `kafka`, `kafka-2`, `kafka-3`, `zookeeper`, `kafka-ui` | Nuevo: cluster de 3 brokers | AWS EC2 (autogestionado, una instancia por broker) |
| [`ms-competitions`](ms-competitions) | `ms-competitions` | AWS EC2 |
| [`web-org`](web-org) | Web Angular `web-org` (panel del delegado) | GitHub Pages |
| `postgres` | BD de `ms-competitions` | AWS RDS (en la maqueta, equivale a una EC2 simple) |
| [`ms-statistics`](ms-statistics) | Nuevo: consumidor del marcador | AWS EC2 |
| `mongo`, `mongo-express` | Nuevo: BD de `ms-statistics` | AWS EC2 |
| [`web-public`](web-public) | Nueva: marcador público | GitHub Pages |

## Levantar

```bash
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Kafka UI | http://localhost:8080 |
| ms-competitions | http://localhost:8081/encuentros |
| web-org | http://localhost:5000 |
| mongo-express | http://localhost:8083 |
| web-public | http://localhost:5173 |

## Borrar

Para probar el flujo de nuevo

```bash
docker compose down -v
```

Guía paso a paso en [GUIA.md](GUIA.md).
