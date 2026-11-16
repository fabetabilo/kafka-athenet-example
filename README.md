## Maqueta Athenet Kafka para el streaming de datos

Este proyecto tiene el objetivo de estudiar y comprender el uso de Kafka a través del flujo 
que usa Athenet para encuentros entre dos equipos en vivo. 

El objetivo es mostrar el marcador cambiando en tiempo real en una web mediante websocket.

## Componentes

| Servicio | Representa en Athenet | Infra real |
|---|---|---|
| `kafka`, `zookeeper`, `kafka-ui` | Nuevo: broker de eventos | AWS EC2 (autogestionado) |
| [`ms-competitions`](ms-competitions) | `ms-competitions` | AWS EC2 |
| `postgres` | BD de `ms-competitions` | AWS RDS (en la maqueta, equivale a una EC2 simple) |

## Levantar

```bash
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Kafka UI | http://localhost:8080 |
| ms-competitions | http://localhost:8081/encuentros |

Guía paso a paso en [GUIA.md](GUIA.md).
