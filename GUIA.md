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
