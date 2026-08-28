# Copilot-instruksjoner for bidrag-beregn-felles

## Domene

Beregningssystem for norsk barnebidrags- og forskuddsordning. Viktige begreper:
- **barnebidrag** – ordinært bidrag fra bidragspliktig til bidragsmottaker
- **forskudd** – NAVs forskudd på barnebidrag
- **særbidrag** – ekstraordinært bidrag for særlige utgifter
- **bidragsevne** – bidragspliktiges betalingsevne basert på inntekt
- **underholdskostnad** – kostnad for å forsørge barnet
- **samværsfradrag** – fradrag for samvær med barnet
- **sjablon** – konfigurasjonstabeller for satser og grenser (skatt, minstefradrag, etc.)
- **grunnlag** – inndata representert som `GrunnlagDto`
- **vedtak** – rettslig avgjørelse
- **bruddperiode** – delperiode der alle inndata er konstante

---

## Bygg og test

```bash
# Bygg alle moduler
mvn clean install

# Bygg én modul
mvn clean install -pl bidrag-beregn-barnebidrag

# Bygg uten tester
mvn clean install -DskipTests

# Kjør alle tester
mvn clean test

# Kjør tester for én modul
mvn test -pl bidrag-beregn-barnebidrag

# Kjør én testklasse
mvn test -Dtest=BeregnBidragsevneTest

# Kjør én testmetode
mvn test -Dtest=BeregnBidragsevneTest#testBidragsevne_Eksempel01

# Sjekk kodestil (ktlint kjører i verify-fasen)
mvn verify
```

Kode og tester er skrevet i **Kotlin 2.x** med **Java 21**. Kilder ligger i `src/main/kotlin` og `src/test/kotlin`.

---

## Moduloversikt

```
bidrag-beregn-core          Felles fundament: DTOer, periodebehandling, sjaблонhjelpere, basisservice
bidrag-inntekt              Inntektsaggregering (A-register, skatt, trygd, kontantstøtte, etc.)
bidrag-boforhold            Boforhold (barn og voksne i husstand)
bidrag-sivilstand           Sivilstandsoverganger
bidrag-beregn-forskudd      Beregning av forskudd
bidrag-beregn-særbidrag     Beregning av særbidrag
bidrag-beregn-barnebidrag   Hovedberegning av barnebidrag (orkestrerer øvrige moduler)
bidrag-indeksregulering     Årlig indeksregulering av bidragsbeløp
bidrag-vedtak               Pakker beregningsresultater inn i vedtak
bidrag-beregn-debug-app     Spring Boot-app for lokal testing (port 9898, debug-port 5005)
```

Avhengighetsflyt: `bidrag-beregn-core` → datamoduler → beregningsmoduler → `bidrag-beregn-barnebidrag` → `bidrag-beregn-vedtak` → debug-app.

Eksterne biblioteker: `bidrag-domene-felles` (domeneoppsummeringer, `ÅrMånedsperiode`), `bidrag-transport-felles` (DTOer), `bidrag-commons-felles` (sjablonleverandører, hjelpefunksjoner).

---

## Arkitektur

### Grunnlag-basert datamodell

All inndata er et `BeregnGrunnlag` med en `grunnlagListe: List<GrunnlagDto>`. Hvert `GrunnlagDto` har:
- `referanse` – unik streng-ID (brukes for sporbarhet)
- `type` – enum som identifiserer datatypen
- `innhold` – JSON-payload (deserialiseres via `filtrerOgKonverterBasertPåEgenReferanse()`)

Utdata er også `List<GrunnlagDto>`, der `innhold` er et serialisert resultatobjekt (f.eks. `DelberegningBidragsevne`).

### Periodisering

Inndata-grunnlag kan strekke seg over flere kalenderperioder. Motoren deler dem opp i **bruddperioder** – delperioder der alle inndata er konstante – beregner uavhengig per periode og aggregerer resultater.

`Periodiserer` i `bidrag-beregn-core` håndterer oppstykkingen.

### Lagdelt beregningsmønster (barnebidrag som kanonisk eksempel)

```
BeregnXxxApi                   ← inngangspunkt
  └─ service/orkestrering/     ← orkestratorer (koordinerer flertrinns-flyter)
       └─ service/beregning/   ← tjenester (én per beregningstype)
            └─ beregning/      ← ren beregningslogikk (XxxBeregning.kt)
  mapper/                      ← mapper GrunnlagDto ↔ interne objekter
  bo/                          ← forretningsobjekter (XxxBO.kt, immutable dataklasser)
```

Typisk beregningsmetode:
```kotlin
fun beregnXxx(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
    val grunnlag = mapTilGrunnlag(mottattGrunnlag)      // hent typede inndata
    val bruddperioder = lagBruddPerioder(grunnlag)       // stykk opp i bruddperioder
    val resultater = bruddperioder.map { periode ->
        val bg = lagBeregningGrunnlag(grunnlag, periode)
        XxxPeriodeResultat(periode, XxxBeregning.beregn(bg))
    }
    return resultater.map { mapTilGrunnlag(it) }         // tilbake til GrunnlagDto-liste
}
```

---

## Navnekonvensjoner

| Konsept | Mønster | Eksempel |
|---|---|---|
| Inngangspunkt | `BeregnXxxApi` | `BeregnBarnebidragApi` |
| Tjeneste (orkestrering) | `BeregnXxxService` | `BeregnBidragsevneService` |
| Ren beregningslogikk | `XxxBeregning` | `BidragsevneBeregning` |
| Mapper | `XxxMapper` | `BidragsevneMapper` |
| Forretningsobjekt | `XxxBO` | `BidragsevneBO` |
| Perioderesultat | `XxxPeriodeResultat` | `BidragsevnePeriodeResultat` |
| Beregningsinndata | `XxxBeregningGrunnlag` | `BidragsevneBeregningGrunnlag` |
| Utdata-DTO | `DelberegningXxx` | `DelberegningBidragsevne` |

Pakkerot: `no.nav.bidrag.beregn.{moduleName}`

---

## Testkonvensjoner

- **Rammeverk:** JUnit 5 + AssertJ + Mockito (`@ExtendWith(MockitoExtension::class)`)
- **Testdata:** JSON-filer i `src/test/resources/testfiler/` – én fil per scenario
- **Sjablon-mocking:** Kall `stubSjablonProvider()` i `@BeforeEach`
- **Basisklasse:** Testklasser arver `FellesTest` for felles oppsett og hjelpemetoder for assertions
- **Assertions-hjelper:** `utførBeregningerOgEvaluerResultat()` (og typespesifikke varianter) kjører beregningen og verifiserer alle forventede verdier
- **Navngiving:** `testXxx_Scenario` (f.eks. `testBidragsevne_Eksempel01`)

```kotlin
@ExtendWith(MockitoExtension::class)
internal class BeregnBidragsevneTest : FellesTest() {
    @BeforeEach
    fun initMock() {
        stubSjablonProvider()
        api = BeregnBarnebidragApi()
    }

    @Test
    @DisplayName("Bidragsevne - eksempel 1 - ...")
    fun testBidragsevne_Eksempel01() {
        filnavn = "src/test/resources/testfiler/bidragsevne/bidragsevne_eksempel1.json"
        forventetBidragsevne = BigDecimal.ZERO.setScale(2)
        utførBeregningerOgEvaluerResultatBidragsevne()
    }
}
```

---

## Debug-app

`bidrag-beregn-debug-app` er en Spring Boot-app for lokal ende-til-ende-testing:
- Start med `./start.sh` eller `./initEnv.sh`
- REST-grensesnitt på `http://localhost:9898`
- Remote debug-port: `5005`
