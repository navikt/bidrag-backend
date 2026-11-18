---
applyTo: "**/Dockerfile"
---

# Dockerfile Standards

Standarder for Dockerfile i `bidrag-backend`: distroless base images, Maven-bygg utenfor Docker, og sikkerhetspraksis.

Reference: [sikkerhet.nav.no/docs/sikker-utvikling](https://sikkerhet.nav.no/docs/sikker-utvikling/)

## Etablert mønster i dette repoet

`bidrag-backend`-apper bygges med Maven **utenfor** Docker (i CI, før `docker build`), og Dockerfilen kopierer kun den ferdige jar-filen inn. Bruk [Dockerfile i roten](../../Dockerfile) som mønster for nye apper: Den bruker Chainguard/Wolfi med Java 25 og henter norsk locale, shell og `printenv` fra en egen dev-stage.

Behold dokumenterte avvik for apper som trenger dem, for eksempel `bidrag-sak` sin `ENTRYPOINT` for DB2-lisensfilen og `bidrag-admin-fss` sine Java-proxyinnstillinger.