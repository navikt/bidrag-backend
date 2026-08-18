# Copilot Instructions for bidrag-belopshistorikk

## Key Patterns

- **Auth:** Azure AD med token-validation-spring. Inbound fra andre bidrag-tjenester.
- **Data:** JPA-entiteter (Stønad, Periode, Engangsbeløp) med Spring Data repositories.
- **Hendelser:** VedtakHendelseListener konsumerer Kafka-topic og delegerer til BehandleHendelseService.
- **Periodelogikk:** Nye vedtak erstatter eksisterende perioder. Overlapp ugyldiggjøres, restperioder gjenskapes.
- **Caching:** Caffeine-cache for hyppige oppslag.

## Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.
