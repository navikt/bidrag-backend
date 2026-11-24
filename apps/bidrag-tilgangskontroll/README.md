# Bidrag-tilgangskontroll

## Beskrivelse
Felles applikasjon for tilgangskontroll av bidrag ressurser

#### Start applikasjon lokalt mot sky
For å kunne kjøre lokalt mot sky må du gjøre følgende

Åpne terminal på root mappen til `bidrag-tilgangskontroll`
Konfigurer kubectl til å gå mot kluster `dev-gcp`
```bash
# Sett cluster til dev-gcp
kubectx dev-gcp
# Sett namespace til bidrag
kubens bidrag 
```
Deretter kjør følgende kommando for å importere secrets. Viktig at filen som opprettes ikke committes til git

```bash
kubectl exec --tty deployment/bidrag-tilgangskontroll-feature printenv | grep -E 'AZURE_|URL|SCOPE|VALKEY_|CACHE_VALKEY_INSTANCE' > src/test/resources/application-lokal-nais-secrets.properties
```

Deretter kan tokenet brukes til å logge inn på swagger-ui http://localhost:8080/swagger-ui.html

### Cache

Tilgangsdata caches med [bidrag-cache](../../libs/bidrag-cache/README.md). Navn og avvikende levetider ligger i `CacheConfig.kt`.

| Cache | Annotasjon | Levetid |
|---|---|---|
| `PIP_SAK` | `@KortCache` | 10 sekunder |
| Øvrige | `@LangCache` (Caffeine og Valkey) | Til kl. 06:00 neste dag |

Enkeltoppslagene mot tilgangsmaskinen bruker `@KortCache` fordi `ResponseEntity` ikke kan lagres i Valkey.

Alle cacher er satt opp slik at de caches per bruker. `CacheKonfigurasjon` henter saksbehandlerident fra `TokenUtils`. Mangler saksbehandlerident, avvises oppslaget.

Valkey-instansen settes med `CACHE_VALKEY_INSTANCE` i `.nais/bidrag-tilgangskontroll/<miljø>.yaml`, og er opprettet i [Nais Console](https://console.nav.cloud.nais.io/). Lokalt og i tester er variabelen ikke satt, og `@LangCache` caches da bare i Caffeine.

### Live reload
Med `spring-boot-devtools` har Spring støtte for live-reload av applikasjon. Dette betyr i praksis at Spring vil automatisk restarte applikasjonen når en fil endres. Du vil derfor slippe å restarte applikasjonen hver gang du gjør endringer. Dette er forklart i [dokumentasjonen](https://docs.spring.io/spring-boot/docs/1.5.16.RELEASE/reference/html/using-boot-devtools.html#using-boot-devtools-restart).
For at dette skal fungere må det gjøres noe endringer i Intellij instillingene slik at Intellij automatisk re-bygger filene som er endret:

* Gå til `Preference -> Compiler -> check "Build project automatically"`
* Gå til `Preference -> Advanced settings -> check "Allow auto-make to start even if developed application is currently running"`