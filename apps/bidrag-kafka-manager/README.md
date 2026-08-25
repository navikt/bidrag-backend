# bidrag-kafka-manager

Web-UI for å utforske og administrere Kafka-clusteret til bidrag-teamet, basert på
[Kafbat UI](https://github.com/kafbat/kafka-ui) (`ghcr.io/kafbat/kafka-ui`). Kafbat UI er en
aktivt vedlikeholdt fork av deprikerte `provectus/kafka-ui` 

Appen inneholder ingen egen kildekode, kun en `Dockerfile` som pakker inn det offisielle
Kafbat UI-imaget med en egen `config.yml` tilpasset Nais/Aiven-oppsettet mot Kafka via
`KAFKA_BROKERS`/`KAFKA_TRUSTSTORE_PATH`/`KAFKA_KEYSTORE_PATH`/`KAFKA_CREDSTORE_PASSWORD`, som
settes automatisk av Nais når `kafka.pool` er konfigurert i `nais.yaml`.

## Tilgang

- Dev: https://bidrag-kafka-manager.intern.dev.nav.no/index.html
- Prod: https://bidrag-kafka-manager.intern.nav.no/index.html

Innlogging skjer via Azure AD.

## Deploy

Deploy skjer normalt automatisk via GitHub Actions (`.github/workflows/bidrag-kafka-manager.yaml`)
ved push. For manuell deploy fra rotmappen til monorepoet:

```bash
kubectx dev-gcp
kubens bidrag
k apply -f .nais/bidrag-kafka-manager/dev.yaml
```

Bytt til `prod-gcp` og `.nais/bidrag-kafka-manager/prod.yaml` for prod.
