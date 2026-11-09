# Bidrag-cache

Brukerbasert Spring-cache med Caffeine lokalt og Valkey som delt cache mellom podder og lar cache overleve applikasjonsrestart.

Biblioteket er uavhengig og har ingen avhengigheter til `bidrag-felles`, tokenbiblioteker eller domenetyper.
Appen leverer brukeridentitet fra sin validerte innloggingskontekst.

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

Biblioteket bruker TLS og leser `VALKEY_HOST_<INSTANS>`, `VALKEY_PORT_<INSTANS>`, `VALKEY_USERNAME_<INSTANS>` og `VALKEY_PASSWORD_<INSTANS>` fra miljøet. 

### 4. Registrer CacheSettings-bean
Se [hvordan det gjøres i bidrag-tilgangskontroll](../../apps/bidrag-tilgangskontroll/src/main/kotlin/no/nav/bidrag/tilgangskontroll/konfigurasjon/CacheKonfigurasjon.kt)

### 5. Bruk vanlig Cacheable

```kotlin
import org.springframework.cache.annotation.Cacheable

@Cacheable("PERSON")
fun hentPerson(personIdent: String): PersonDto = personClient.hentPerson(personIdent)
```

Biblioteket aktiverer `@EnableCaching` og setter standardnøkkelgeneratoren for `@Cacheable`. En egen cacheannotasjon er ikke nødvendig. Ikke angi en egen `key`, `keyGenerator` eller `cacheManager`; det ville omgå bibliotekets oppsett.

Spring-proxyen må håndtere kallet. Cacheannotasjoner virker ikke ved selvkall i samme objekt. Flyten gjelder vanlige, synkrone metodekall; ikke bruk `sync = true` eller asynkrone cachemetoder.

Ved overgang fra gammelt oppsett: fjern egne `CacheManager`-/`CachingConfigurer`-beans og eventuell `@EnableUserCache`.

## Faste standarder og utløp

| Standard | Verdi |
|---|---|
| Størrelse | Maksimalt 10 000 oppføringer per cache og pod. |
| Valkey-timeout | 500 ms for tilkobling og kommandoer. |
| Applikasjonsnavn | `spring.application.name`, som må være satt. |
| Tidssone | JVM-ens tidssone. |
| Feilmetrikker| `bidrag.cache.error`, med operasjonen `get` eller `put`. |

Begge lag følger samme utløpsregel. Ved treff i Valkey fylles Caffeine med gjenværende levetid. Treff forlenger ikke fristen og skriver ikke verdien tilbake til Valkey. Utløpte verdier avvises selv om de fortsatt ligger fysisk i et av lagene.

## Nøkler og serialisering

Nøkler utledes av brukeridentitet, metode og parametere med HMAC-SHA256 og Valkey-passordet. Identer står dermed ikke i klartekst i Valkey-nøkler. Manglende eller blank brukeridentitet avvises før cache eller kilde aksesseres. Appen må skille validerte applikasjonskall eksplisitt fra saksbehandlere; biblioteket tolker ikke token eller velger systembrukere. Igjen, se oppsett i [bidrag-tilgangskontoll](../../apps/bidrag-tilgangskontroll/src/main/kotlin/no/nav/bidrag/tilgangskontroll/konfigurasjon/CacheKonfigurasjon.kt) om du er usikker.

Strenger, tall, boolske verdier, enum, `null` og lister støttes som nøkkelparametere. For egne domenetyper angir appen `nøkkelprefix` i samme `CacheSettings`. Det er slik så bidrag-cache ikke har noe forhold til andre biblioteker.

```kotlin
nøkkelprefix = { verdi ->
    when (verdi) {
        is Saksnummer -> "saksnummer:${verdi.verdi}"
        is Personident -> "personident:${verdi.verdi}"
        else -> null
    }
}
```

Jackson serialiserer verdiene og støtter `ResponseEntity` med status, headere og body. Tillatte pakker er fast begrenset til `no.nav.bidrag.`, `java.util.`, `java.lang.` og `kotlin.collections.`. Nullverdier caches ikke.

Cache-nøkler erstatter ikke autentisering eller tilgangskontroll. Appen må fortsatt validere token og kontrollere tilgang til person- og saksdata.

## Feil, restart og målinger

Oppslagsrekkefølgen er Caffeine -> Valkey -> kilde. Ved treff i Valkey fylles Caffeine. Ved bom i begge fylles begge etter et vellykket kildekall og en vellykket Valkey-skriving.

Ved feil i Valkey går oppslaget til kilden. Utløpte lokale verdier brukes aldri som reserve, og kildefeil caches ikke. Cachefeil logges uten nøkkel, verdi eller exception-melding.

Biblioteket endrer ikke appens health-oppsett. Hvis Valkey bare er en cache og ikke skal gjøre appen usunn ved nedetid, kan Redis-healthchecken slås av i den aktuelle profilen:

```yaml
management.health.redis.enabled: false
```

Lokale treff, bom og størrelse finnes i Micrometers cachemålinger med cache-navn `<navn>-caffeine`. Målingene registreres når cachen tas i bruk. Feiltelleren heter `bidrag.cache.error`.