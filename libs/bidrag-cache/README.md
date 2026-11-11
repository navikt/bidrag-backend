# Bidrag-cache

Cacheløsing med Caffeine og Valkey. Løsning benytter caffeine som en lokal cache i applikasjonspodden og valkey som en delt cache mellom podder. På denne måten overlever cache applikasjonsrestart.

Løsningen inneholder to forskjellige annotasjonsbaserte caches. Disse er `@KortCache` og `@LangCache`.

`@KortCache` er kun caffeine, ment for å brukes der hvor cachen kun trengs en liten stund. Apper som bare bruker `@KortCache`, trenger ikke Valkey. Hopp da over steg 2 og 3 under [Ta biblioteket i bruk](#ta-biblioteket-i-bruk).

`@LangCache` benytter både caffeine og valkey og lagrer til begge. Dette fører til at applikasjonsrestart fremdeles har en valkey-cache som den kan treffe. Dette er lurt å benytte i tilfeller hvor vi ikke forventer at responsen endres fortløpende.

## Levetid

| Annotasjon | Standard levetid |
|---|---|
| `@KortCache` | 10 minutter |
| `@LangCache` | Fram til kl. 06:00 neste dag, både i Caffeine og Valkey |

Standarden kan overstyres per metode med `levetid`. Verdien kan skrives som `"30s"`, `"5m"`, `"2h"`, `"1d"` eller ISO-8601 (`"PT1H"`):

```kotlin
@KortCache("TILGANG", perBruker = true, levetid = "1m")
fun harTilgang(saksnummer: String): Boolean = ...

@LangCache("KODEVERK", perBruker = false, levetid = "2h")
fun hentKodeverk(navn: String): Kodeverk = ...
```

Ugyldig eller negativ `levetid` gir feil første gang metoden kalles. Lesing forlenger ikke levetiden. Ved treff i Valkey legges verdien i Caffeine med full levetid.

## Støttede typer

### `@KortCache`

Alle typer støttes. Verdien ligger bare i Caffeine og blir aldri gjort om til JSON.

### `@LangCache`

Verdien lagres som JSON i Valkey, med typeinformasjon, slik at den leses tilbake som samme type. Vi snakker her om *returtypen* metoden som er cachet har. 

| Støttet | Ikke støttet |
|---|---|
| `String`, `Int`, `Long`, `Double`, `Boolean` og `BigDecimal` | Klasser utenfor de tillatte pakkene, for eksempel `HttpStatus` |
| `LocalDate`, `LocalDateTime`, `YearMonth` og andre typer i `java.time` | `ResponseEntity` da Jackson kan ikke lage den. Cache bodyen i stedet. |
| Enum | `mapOf` med ett element (`SingletonMap`). Bruk `hashMapOf` eller `listOf` i stedet. |
| Data classes, sealed classes, `data object` og vanlige klasser i `no.nav.*` | Value classes som felt.|
| `List`, `Set` og `Map`, også tomme, og med elementer av typene over | |
| `Array`, `ByteArray` og andre primitive arrayer | |
| `Optional` som returtype. Spring pakker den ut før lagring og inn igjen ved retur. | |
| `Pair`, `Triple` og `Unit` | |
| Felt av typen `Any`, når verdien er av en støttet type | |
| `null` | |

Tillatte pakker er `no.nav.`, `java.util.`, `java.lang.`, `java.time.`, `java.math.` og `kotlin.`. I tillegg er alle arrayer tillatt (`[`).

**Tommelfingerregel:** La metoden returnere en data class fra `no.nav.*`, eller en `List` eller `Set` av slike.

> [!WARNING]
> Feil med typene oppdages først når verdien leses fra Valkey, altså på en annen pod eller etter en omstart. Frem til da svarer Caffeine med det opprinnelige objektet.

## Faste standarder

| Standard | Verdi |
|---|---|
| Størrelse på cache | Maks 10 000 oppføringer per cache og pod |
| Tidsavbrudd mot Valkey | 500 ms. Kan endres med `bidrag.cache.valkey.tidsavbrudd`. |
| Tidssone | `Europe/Oslo`, uavhengig av JVM-ens tidssone |

## Nøkler

Nøkkelen består av cachenavn, klasse og metode, parametere og eventuelt brukerident. I Valkey ser den slik ut:

```
bidrag-cache::<cachenavn>::<Klasse.metode>::<sha256 av brukerident og parametere som JSON>
```

Brukerident og parametere gjøres om til JSON med Jackson og hashes, så fødselsnumre og andre personopplysninger står ikke i klartekst i Valkey. JSON brukes i stedet for `toString()`, som kan være maskert. For eksempel viser `Personident.toString()` bare annenhver siffer.

Parametere må kunne serialiseres med Jackson, for eksempel `String`, tall, enum, data classes og typer med `@JsonValue` som `Personident`. Felt og map-nøkler sorteres, så rekkefølgen påvirker ikke nøkkelen. Kan en parameter ikke serialiseres, behandles det som en feil mot Valkey, og verdien caches bare i Caffeine.

## Feil

Oppslagsrekkefølgen for `@LangCache` er Caffeine, så Valkey, så kilden.

- Feil ved lesing fra Valkey behandles som bom, og kilden kalles.
- Feil ved lagring i Valkey gir bare lagring i Caffeine.
- Feil fra kilden caches ikke.
- Feil logges uten nøkkel og verdi i vanlig logg. Nøkkelen logges bare til `secureLogger`.

### Helsesjekk

Biblioteket slår av Spring Boots helsesjekk for Redis som standard. Den inngår ellers i `/actuator/health`, som mange apper bruker som startprobe i Nais. Er Valkey nede, ville nye podder gått i startup-loop til Valkey var oppe igjen.

Vil appen likevel ha helsesjekken, kan du slå den på i `application.yaml`:

```yaml
management:
  health:
    redis:
      enabled: true
```

## Metrikker

Metrikkene registreres første gang en cache tas i bruk. De havner i appens `MeterRegistry`, og hvis appen ikke har actuator, i `Metrics.globalRegistry`.

| Metrikk | Tags | Innhold |
|---|---|---|
| `cache.gets` | `cache`, `cache.manager`, `result` (`hit`/`miss`) | Treff og bom i Caffeine |
| `cache.size`, `cache.evictions`, `cache.puts` m.fl. | `cache`, `cache.manager` | Micrometers standardmålinger for Caffeine |
| `bidrag.cache.valkey.gets` | `cache`, `result` (`hit`/`miss`) | Treff og bom i Valkey etter bom i Caffeine. Gjelder bare `@LangCache`. |
| `bidrag.cache.error` | `cache`, `operasjon` (`get`/`put`) | Feil mot Valkey som ble behandlet som bom eller ikke ble lagret |

`cache.manager` er `kortCacheManager` for `@KortCache` og `langCacheManager` for `@LangCache`.

## Tømme cache

Biblioteket lager disse endepunktene automatisk i appen:

| Endepunkt | Virkning |
|---|---|
| `DELETE /bidrag-cache` | Tømmer alle cacher |
| `DELETE /bidrag-cache/{navn}` | Tømmer cachen med navnet `navn` |

Begge svarer `204 No Content`. Cachenavn kan bare inneholde bokstaver, tall, `_`, `.` og `-`.

Endepunktene er beskyttet med `@Protected`, så alle gyldige tokens fra appens konfigurerte utstedere slipper gjennom. Det krever at appen har `@EnableJwtTokenValidation`. Uten den er endepunktene åpne.

Slik tømmes cachen:

1. Valkey tømmes direkte. Alle nøkler starter med `bidrag-cache::<navn>::`, så andre data i instansen berøres ikke.
2. Podden som får kallet tømmer sin egen Caffeine.
3. De andre poddene får beskjed via Valkey pub/sub på kanalen `bidrag-cache:tøm` og tømmer sin Caffeine.

Pub/sub leverer ikke beskjeden på nytt. En pod som ikke er koblet til Valkey når beskjeden sendes, beholder sine lokale verdier til de utløper.

Apper uten Valkey tømmer bare Caffeine i podden som får kallet. Andre podder beholder sine verdier til de utløper.

## Ta biblioteket i bruk

### 1. Legg til avhengigheten

```xml
<dependency>
    <groupId>no.nav.bidrag</groupId>
    <artifactId>bidrag-cache</artifactId>
    <version>${project.version}</version>
</dependency>
```

Biblioteket ligger i `libs/bidrag-cache` og brukes internt i monorepoet. Maven bygger det fra samme checkout når appen bygges med `-am`. Det publiseres ikke til GitHub Packages.
Legg `libs/bidrag-cache/**` til `APP_PATHS` i appens workflow, slik at endringer i biblioteket også velger appen for bygg og test.
Biblioteket bruker Java 25, Kotlin og Spring Boot 4, og inkluderer nødvendige Spring-, Valkey- og Caffeine-avhengigheter.

### 2. Opprett Valkey i Nais Console

Steg 2 og 3 trengs bare for `@LangCache`.

Opprett instansen i [Nais Console](https://console.nav.cloud.nais.io/) etter [Nais-veiledningen](https://docs.nais.io/persistence/valkey/how-to/create/):

1. Velg team og miljø, og åpne **Valkey**.
2. Velg **Create Valkey**, angi instansnavn, kapasitet og tilgjengelighet.  
   1. Anbefaler; Tier: High availability for prod, single node for dev, Memory: 1 GB, Max memory policy: allkeys-lru 
3. Vent til instansen er klar før appen deployes.

Bruk separate instanser for miljøer som ikke skal dele data. q1 og q2 kan enten av hver sin i dev, eller dele en felles. Prod må naturligvis ha en egen i prod. 
Instansene forvaltes i konsollen.

### 3. Koble appen til instansen

Nais-manifestet refererer til instansen:

```yaml
spec:
    valkey:
    - instance: {{cache_valkey_instance}}
      access: readwrite
```

Dette gir tilgang og lar Nais injisere tilkoblingsinformasjon. Se [Nais-veiledningen for bruk i en app](https://docs.nais.io/persistence/valkey/how-to/use-in-workload/).

Sett i tillegg miljøvariabelen `CACHE_VALKEY_INSTANCE` til instansnavnet:

```yaml
spec:
    env:
    - name: CACHE_VALKEY_INSTANCE
      value: {{cache_valkey_instance}}
```

Uten `CACHE_VALKEY_INSTANCE` kjører biblioteket uten Valkey. `@LangCache` caches da bare i Caffeine i hver pod, med samme levetid, og overlever ikke restart. Appen logger en advarsel ved oppstart for hver bean som bruker `@LangCache`. Spring Boots egen Redis-autokonfigurasjon slås også av, slik at helsesjekken ikke prøver å nå Redis på localhost. Dette gjelder ikke apper som selv setter `spring.data.redis.host` eller `spring.data.redis.url`.

Biblioteket bruker TLS og leser `VALKEY_HOST_<INSTANS>`, `VALKEY_PORT_<INSTANS>`, `VALKEY_USERNAME_<INSTANS>` og `VALKEY_PASSWORD_<INSTANS>` fra miljøet. 

### 4. Bruk annotasjonene

```kotlin
@KortCache("TILGANG", perBruker = true)
fun harTilgang(saksnummer: String): Boolean = ...

@LangCache("KODEVERK", perBruker = false, unless = "#result == null")
fun hentKodeverk(navn: String): Kodeverk? = ...
```

| Parameter | Betydning |
|---|---|
| `value` | Cachenavn. Påkrevd. |
| `perBruker` | `true` gir egen verdi per innlogget bruker. Påkrevd. |
| `unless` | SpEL-uttrykk. Når det er sant, caches ikke resultatet. |
| `levetid` | Overstyrer standard levetid. Se [Levetid](#levetid). |

`perBruker = true` krever en bean som forteller hvem brukeren er:

```kotlin
@Bean
fun cacheBrukerIdentifikator() = CacheBrukerIdentifikator { hentInnloggetBrukerFraToken() }
```

Bruk `perBruker = true` når svaret avhenger av hvem som spør, for eksempel tilgangssjekker.

Annotasjonene virker bare når kallet går gjennom Spring-proxyen. Kall fra en metode i samme klasse caches ikke.