---
applyTo: "**/Dockerfile"
---

# Dockerfile Standards

Standarder for Dockerfile i `bidrag-backend`: distroless base images, Maven-bygg utenfor Docker, og sikkerhetspraksis.

Reference: [sikkerhet.nav.no/docs/sikker-utvikling](https://sikkerhet.nav.no/docs/sikker-utvikling/)

## Etablert mønster i dette repoet

`bidrag-backend`-apper bygges med Maven **utenfor** Docker (i CI, før `docker build`), og Dockerfilen kopierer kun den ferdige jar-filen inn. Standardmønsteret er `gcr.io/distroless/java21` med en egen `ubuntu`-locale-stage for norsk locale (`nb_NO.UTF-8`):

```dockerfile
FROM ubuntu:26.04 AS locales
RUN apt-get update && apt-get install -y locales
RUN locale-gen nb_NO.UTF-8 && \
    update-locale LANG=nb_NO.UTF-8 LANGUAGE="nb_NO:nb" LC_ALL=nb_NO.UTF-8

FROM gcr.io/distroless/java21
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY --from=busybox:1.35.0-glibc /bin/sh /bin/sh
COPY --from=busybox:1.35.0-glibc /bin/printenv /bin/printenv
COPY --from=locales /usr/lib/locale/ /usr/lib/locale/

WORKDIR /app
COPY ./target/app.jar app.jar

EXPOSE 8080
ENV LANG=nb_NO.UTF-8 LANGUAGE='nb_NO:nb' LC_ALL=nb_NO.UTF-8 TZ="Europe/Oslo"
ENV SPRING_PROFILES_ACTIVE=nais

CMD ["app.jar"]
```

**Følg dette mønsteret for nye apper i `apps/`** med mindre det finnes en dokumentert, god grunn til å avvike (f.eks. `bidrag-sak` sin egen `ENTRYPOINT` for å legge til en DB2-lisensfil for Bisys-tilkobling — avviket er kommentert direkte i Dockerfilen).