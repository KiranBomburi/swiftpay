# Dev Notes

Random notes while building this.

## Issues hit

- flyway-database-postgresql needs explicit version, not managed by spring boot parent — wasted 30 mins on this
- local postgres on 5432 conflicted with docker container, changed docker ports to 5440/5441/5442
- Kafka consumer group rebalancing logs are very noisy on startup, not errors, just ignore them
- `createdAt` coming back null in gateway response because hibernate sets it after flush, not a bug but looks weird

## TODO (ran out of time)

- [ ] add pagination to /v1/ledger/history — right now returns everything
- [ ] currency validation — should check sender/receiver currencies match
- [ ] add prometheus metrics endpoint properly
- [ ] write more integration tests, only have basic ones
- [ ] the analytics volume endpoint needs date range filtering

## Useful commands

```bash
# check kafka topics
docker exec swiftpay-kafka kafka-topics --bootstrap-server localhost:9092 --list

# check redis keys
docker exec swiftpay-redis redis-cli keys "*"

# tail gateway logs only
docker compose logs -f transaction-gateway

# reset everything
docker compose down -v && docker compose up --build
```
