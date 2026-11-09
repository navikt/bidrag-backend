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

### Delt cache

Tilgangsdata caches med [bidrag-cache](../../libs/bidrag-cache/README.md), som setter opp Caffeine foran Nais Valkey. `Cache.kt` samler navn, standardregler og innstillinger for hver cache. De fleste peker på `standardregler`, mens `PIP_SAK` har sin egen levetid. Endre en enkelt cache i `innstillinger`, eller endre `standardregler` for å justere alle som bruker dem. Cachene opprettes automatisk ved første bruk. Ved bom i begge cachelag hentes data fra kilden.

Oppslagene bruker vanlig `@Cacheable`. `CacheKonfigurasjon` registrerer en `CacheSettings`-bean i profilene `nais` og `lokal-nais`, med Valkey-instans fra `CACHE_VALKEY_INSTANCE`, regler fra `Cache` og brukeridentitet fra `TokenUtils`. Beanen aktiverer biblioteket. Uten Nais-profil opprettes ikke cacheoppsettet.

Valkey er opprettet i [Nais Console](https://console.nav.cloud.nais.io/) for hvert miljø.

Begge cachelag bruker samme brukerbaserte nøkkel. Nøkkelen inkluderer innlogget saksbehandler, metode og parametere, og er laget med HMAC basert på Valkey-passordet. To saksbehandlere får dermed forskjellige nøkler selv ved identiske oppslag. Manglende eller blank saksbehandlerident avvises. Applikasjonskall beholder en egen systembrukernøkkel, adskilt fra saksbehandlernes nøkler. Ved passordrotasjon får nye podder nye nøkler; gamle oppføringer utløper etter vanlig cache-levetid.

Begge cachelag følger samme utløpsfrist: `PIP_SAK` utløper etter 10 sekunder, og øvrige oppslag klokken 06 dagen etter at de ble hentet. Fristen lagres sammen med verdien i Valkey. Ved tilbakefylling bruker Caffeine den gjenværende levetiden. Treff forlenger ikke fristen.

Følg lokale treff, bom og størrelse med Micrometer-cachemålingene for cache-navn som slutter på `-caffeine`, Valkey-målingene i Nais Console og `bidrag.cache.error` i Prometheus.

### Live reload
Med `spring-boot-devtools` har Spring støtte for live-reload av applikasjon. Dette betyr i praksis at Spring vil automatisk restarte applikasjonen når en fil endres. Du vil derfor slippe å restarte applikasjonen hver gang du gjør endringer. Dette er forklart i [dokumentasjonen](https://docs.spring.io/spring-boot/docs/1.5.16.RELEASE/reference/html/using-boot-devtools.html#using-boot-devtools-restart).
For at dette skal fungere må det gjøres noe endringer i Intellij instillingene slik at Intellij automatisk re-bygger filene som er endret:

* Gå til `Preference -> Compiler -> check "Build project automatically"`
* Gå til `Preference -> Advanced settings -> check "Allow auto-make to start even if developed application is currently running"`