## Bidrag-oppgave

Proxy mot oppgave-tjenesten. https://oppgave.intern.dev.nav.no

### Local utvikling
Autentisering skjer med bearertoken. Det er satt opp autentisering via https://github.com/navikt/localauth mot q2 miljøet

Start opp Docker-containeren med `docker compose up -d`

Start app med spring profil `dev`

Token kan hentes fra https://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp:bidrag:bidrag-localauth

swagger api er tilgjengelig på http://localhost:8080/swagger-ui/index.html#
