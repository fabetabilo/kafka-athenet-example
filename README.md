## Maqueta Athenet Kafka para el streaming de datos

Este proyecto tiene el objetivo de estudiar y comprender el uso de Kafka a través del flujo 
que usa Athenet para encuentros entre dos equipos en vivo. 

El objetivo es mostrar en una web mediante websocket el marcador cambiando en tiempo real.


## Componentes

| Servicio | Representa en Athenet | Infra real |
|---|---|---|
| `kafka`, `zookeeper`, `kafka-ui` | Nuevo: broker de eventos | AWS EC2 (autogestionado) |

## Levantar

```bash
docker compose up -d
```

| Servicio | URL |
|---|---|
| Kafka UI | http://localhost:8080 |

Guía paso a paso en [GUIA.md](GUIA.md).
