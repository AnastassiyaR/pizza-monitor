# How Kafka and Redis work - through this project

## Kafka

### Why it is here
The sensor knows nothing about the monitor, and the monitor knows nothing about the alert service. They talk through a **message log**. Any service can be restarted, added or scaled without touching the others. While the monitor is stopped, messages are not lost: they wait in Kafka (7 days by default).

### Core concepts

| Concept | What it is | In this project |
|---|---|---|
| **Broker** | a Kafka server | the `kafka` container |
| **Topic** | a named, append-only log of messages | `pizza-metrics`, `pizza-alerts` |
| **Partition** | a topic is split into N independent logs; order is guaranteed only within one partition | 3 per topic |
| **Offset** | sequence number of a message within a partition | visible in logs: `[p1@42]` = partition 1, offset 42 |
| **Key** | message key; `hash(key) % partitions` picks the partition | `pizzeriaId` |
| **Producer** | writes to a topic | `KafkaTemplate` in sensor and monitor |
| **Consumer** | reads from a topic | `@KafkaListener` in monitor and alert |
| **Consumer group** | a team of consumers; each partition is read by exactly one member of the group | `monitor-group`, `alert-group` |
| **Offset commit** | a record saying "the group has read up to N"; after a restart reading continues from N | Spring does it after processing |

### Key ideas

1. **Key = ordering.** All metrics of `pizza-1` go to one partition, so they are processed strictly in order.
2. **Group = scaling.** Start two `monitor-service` instances and the partitions are split between them. `alert-service` is in a different group, so it gets its own full copy of the stream without taking anything away from the monitor.
3. **Partitions ≥ consumers.** If a group has more consumers than partitions, the extra ones sit idle. That is why `concurrency = "3"` with 3 partitions.
4. **Kafka is not a queue.** A message that was read is not deleted. A group can rewind its offset and re-read history.
5. **`auto-offset-reset`** applies only when a group has no offset yet: `latest` = only new messages, `earliest` = from the beginning.

### Where to look in the code
- `sensor-service/.../SensorPublisher.java` - sending (`kafkaTemplate.send(topic, key, value)`); the result arrives asynchronously.
- `monitor-service/.../MetricsListener.java` - `@KafkaListener`, group, partition/offset from `ConsumerRecord`.
- `common/.../KafkaTopicsConfig.java` - topic creation.
- `*/src/main/resources/application.yml` - JSON serialization, `acks=all`, idempotence, `ErrorHandlingDeserializer`.

### Experiments (after `docker compose up -d` and starting the services)

```bash
# topic description: partitions, leaders
docker compose exec kafka kafka-topics --bootstrap-server localhost:29092 --describe --topic pizza-metrics

# watch messages with key, partition and offset
docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:29092 \
  --topic pizza-metrics --from-beginning \
  --property print.key=true --property print.partition=true --property print.offset=true

# group state: current offset, log end offset, lag
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:29092 --describe --group monitor-group
```

Try this:
1. Stop `monitor-service` for a minute and watch LAG in `--describe --group monitor-group` grow. Start it again and it quickly catches up.
2. Start a second `monitor-service` (another terminal: `./gradlew :monitor-service:bootRun`). The logs show a rebalance, and each instance processes its own pizzerias.
3. Set `SENSOR_INTERVAL_MS=200` and look at the `[pN@offset]` log prefixes: each pizzeria always uses the same partition.

## Redis

### What it is
An **in-memory** database with very fast key access. Values are not just strings but also structures: lists, sets, hashes. It is ideal for "current state", counters, caches and keys with an expiry time.

### What is stored in Redis here

| Key | Type | Commands | Purpose |
|---|---|---|---|
| `pizzeria:{id}:latest` | STRING + TTL 60 s | `SET … EX 60`, `MGET` | latest reading. If a sensor is silent for a minute the key disappears and the dashboard shows offline |
| `pizzeria:{id}:history` | LIST | `LPUSH`, `LTRIM 0 99`, `LRANGE` | last 100 readings; newest on the left |
| `alerts:recent` | LIST | `LPUSH`, `LTRIM 0 49`, `LRANGE` | alert feed |
| `alerts:count:{id}` | STRING (number) | `INCR` | atomic counter |
| `alerts:cooldown:{id}:{type}` | STRING + TTL 30 s | `SET … NX EX 30` | anti-spam: a "lock" that expires by itself |

### Key ideas

1. **TTL** - a key deletes itself. No manual cleanup.
2. **LPUSH + LTRIM** - the classic trick for a "bounded list of the latest N".
3. **Atomicity.** Every Redis command is atomic; `INCR` is safe under concurrent calls. For several commands in a row use `MULTI/EXEC` (a transaction): see `PizzaRedisRepository.saveMetric`.
4. **`SET NX EX`** - "set only if the key does not exist". A simple distributed lock and anti-spam are built on it: among ten monitor instances exactly one gets the lock.
5. **Redis is not Kafka.** Redis holds the *current state* (what is true now), Kafka holds the *event stream* (what happened). The monitor turns events into state.

### How it looks in Spring Data Redis
`StringRedisTemplate` is the client; `opsForValue()` gives STRING commands and `opsForList()` gives LIST commands. See `common/.../redis/PizzaRedisRepository.java`: every method documents the Redis command it runs.

### Experiments

```bash
docker compose exec redis redis-cli

KEYS *                                    # all keys (debugging only; use SCAN in production)
GET pizzeria:pizza-1:latest               # latest reading (JSON)
TTL pizzeria:pizza-1:latest               # seconds left to live
LLEN pizzeria:pizza-1:history             # at most 100
LRANGE pizzeria:pizza-1:history 0 2       # 3 freshest entries
LRANGE alerts:recent 0 4
GET alerts:count:pizza-1
TTL alerts:cooldown:pizza-1:HIGH_TEMP     # while the key lives, this alert will not repeat
MONITOR                                   # live stream of all commands - shows the services at work
```

Try this:
1. Stop `sensor-service`, wait a minute, then run `GET pizzeria:pizza-1:latest`: it returns `(nil)` and the dashboard shows "No data".
2. Run `MONITOR` and watch monitor-service send `MULTI`, `SET`, `LPUSH`, `LTRIM`, `EXEC` for every message.
3. Open RedisInsight (http://localhost:5540) and browse keys and their TTLs.

## The full path of one message

1. `sensor-service`: `kafkaTemplate.send("pizza-metrics", "pizza-1", metric)` → JSON → partition chosen by key hash.
2. `monitor-service`: `@KafkaListener` receives the message → `saveMetric` writes `SET/LPUSH/LTRIM` to Redis.
3. If temperature > 310 or dough < 20 kg and the `SET NX EX` lock is free → `send("pizza-alerts", …)`.
4. `alert-service`: another `@KafkaListener` (another group) → log + `LPUSH alerts:recent` + `INCR alerts:count:{id}`.
5. `api-service`: every 2 s reads Redis (`MGET`, `LRANGE`) and pushes a snapshot to the dashboard over WebSocket.
