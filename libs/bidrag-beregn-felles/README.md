# bidrag-beregn-felles

Felles biblioteker for beregning av barnebidrag, forskudd og særbidrag.

[![Release Drafter](https://github.com/navikt/bidrag-beregn-felles/actions/workflows/release-draft.yaml/badge.svg?branch=main)](https://github.com/navikt/bidrag-beregn-felles/actions/workflows/release-draft.yaml)

---

## Innholdsfortegnelse

- [Om prosjektet](#om-prosjektet)
- [Moduloversikt](#moduloversikt)
- [Arkitektur](#arkitektur)
- [Bygg og test](#bygg-og-test)
- [Debug-app](#debug-app)
- [Kodestil](#kodestil)
- [Logging](#logging)
- [Dokumentasjon](#dokumentasjon)
- [Kontakt](#kontakt)

---

## Om prosjektet

Prosjektet implementerer beregningslogikk for bidrag. Det er bygget som et multi-modul Maven-prosjekt der hver modul har ansvar for én del av beregningen.

### Sentrale begreper

| Begrep | Beskrivelse                                                      |
|---|------------------------------------------------------------------|
| **Barnebidrag** | Ordinært bidrag fra bidragspliktig (BP) til bidragsmottaker (BM) |
| **Forskudd** | Forskudd på barnebidrag                                          |
| **Særbidrag** | Ekstraordinært bidrag for særlige utgifter                       |
| **Bidragsevne** | BPs betalingsevne basert på inntekt                              |
| **Underholdskostnad** | Kostnad for å forsørge barnet                                    |
| **Samværsfradrag** | Fradrag for samvær med barnet                                    |
| **Sjablon** | Faste verdier for satser og grenser (skatt, minstefradrag, etc.) |

---

## Moduloversikt

```
bidrag-beregn-felles/
├── bidrag-beregn-core            Felles fundament: DTOer, periodebehandling, sjablonhjelpere, basisservice
├── bidrag-inntekt                Inntektsaggregering (A-register, skatt, trygd, kontantstøtte, m.m.)
├── bidrag-boforhold              Boforhold (barn og voksne i husstand)
├── bidrag-sivilstand             Sivilstandsoverganger
├── bidrag-beregn-forskudd        Beregning av forskudd
├── bidrag-beregn-særbidrag       Beregning av særbidrag
├── bidrag-beregn-barnebidrag     Hovedberegning av barnebidrag (orkestrerer øvrige moduler)
├── bidrag-indeksregulering       Årlig indeksregulering av bidragsbeløp
├── bidrag-vedtak                 Pakker beregningsresultater inn i vedtak
└── bidrag-beregn-debug-app       Spring Boot-app for lokal testing
```

### Avhengighetsflyt

```
bidrag-beregn-core
  ├── bidrag-inntekt
  ├── bidrag-boforhold
  ├── bidrag-sivilstand
  └── bidrag-beregn-forskudd / bidrag-beregn-særbidrag / bidrag-beregn-barnebidrag
        └── bidrag-vedtak
              └── bidrag-beregn-debug-app
```

### Eksterne avhengigheter

- **bidrag-domene-felles** – domeneoppsummeringer, `ÅrMånedsperiode`
- **bidrag-transport-felles** – DTOer
- **bidrag-commons-felles** – sjablonleverandører, hjelpefunksjoner

---

## Arkitektur

### Grunnlag-basert datamodell

All inndata er et `BeregnGrunnlag` med `grunnlagListe: List<GrunnlagDto>`. Hvert `GrunnlagDto` har:
- `referanse` – unik streng-ID for sporbarhet
- `type` – enum som identifiserer datatypen
- `innhold` – JSON-payload (deserialiseres via `filtrerOgKonverterBasertPåEgenReferanse()`)

Utdata er også `List<GrunnlagDto>`, der `innhold` er et serialisert resultatobjekt.

### Periodisering

Inndata kan strekke seg over flere kalenderperioder. Beregningsmotoren deler dem opp i **bruddperioder** (delperioder der alle inndata er konstante), beregner uavhengig per periode og aggregerer resultater. `Periodiserer` i `bidrag-beregn-core` håndterer oppstykkingen.

### Lagdelt beregningsmønster

```
BeregnXxxApi                   ← inngangspunkt
  └─ service/orkestrering/     ← orkestratorer (koordinerer flertrinns-flyter)
       └─ service/beregning/   ← tjenester (én per beregningstype)
            └─ beregning/      ← ren beregningslogikk (XxxBeregning.kt)
  mapper/                      ← mapper GrunnlagDto ↔ interne objekter
  bo/                          ← forretningsobjekter (immutable dataklasser)
```

### Navnekonvensjoner

| Konsept | Mønster | Eksempel |
|---|---|---|
| Inngangspunkt | `BeregnXxxApi` | `BeregnBarnebidragApi` |
| Tjeneste | `BeregnXxxService` | `BeregnBidragsevneService` |
| Ren beregningslogikk | `XxxBeregning` | `BidragsevneBeregning` |
| Mapper | `XxxMapper` | `BidragsevneMapper` |
| Forretningsobjekt | `XxxBO` | `BidragsevneBO` |
| Utdata-DTO | `DelberegningXxx` | `DelberegningBidragsevne` |

Pakkerot: `no.nav.bidrag.beregn.{modulnavn}`

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

### Testkonvensjoner

- **Rammeverk:** JUnit 5 + AssertJ/Kotest + Mockito/MockK
- **Testdata:** JSON-filer i `src/test/resources/testfiler/`
- **Sjablon-mocking:** `stubSjablonProvider()` i `@BeforeEach`
- **Basisklasse:** Testklasser arver `FellesTest` for felles oppsett
- **Navngivning:** `testXxx_Scenario` (f.eks. `testBidragsevne_Eksempel01`)

---

## Debug-app

`bidrag-beregn-debug-app` er en Spring Boot-app for lokal ende-til-ende-testing:

```bash
# Start med skript (starter på port 9898, remote debug på port 5005)
./start.sh

# Alternativt: hent miljøvariabler og start via IDE
./initEnv.sh
# Kjør deretter BeregningDebugAppApplication i IDE
```

- **REST-grensesnitt:** http://localhost:9898
- **Remote debug-port:** 5005

---

## Kodestil

Prosjektet bruker [ktlint](https://pinterest.github.io/ktlint/) for Kotlin-kodestil. Ktlint kjører automatisk:
- **Format:** i `validate`-fasen (`mvn validate`)
- **Sjekk:** i `verify`-fasen (`mvn verify`)

---

## Logging

For å skru på debug-logging, legg til følgende i `application.yaml`:

```yaml
logging:
  level:
    secureLogger: DEBUG
    no.nav.bidrag.inntekt: DEBUG
    no.nav.bidrag.beregn.forskudd: DEBUG
    no.nav.bidrag.beregn.barnebidrag: DEBUG
```

---

## Dokumentasjon

Detaljert dokumentasjon av forretningsregler for beregning av barnebidrag finnes i `bidrag-beregn-barnebidrag/docs/`:

- [Beregning av barnebidrag](bidrag-beregn-barnebidrag/docs/Beregning%20av%20barnebidrag%20-%20forretningsregler.md) – overordnet beregningsflyt
- [Orkestrering av bidragsberegning](bidrag-beregn-barnebidrag/docs/Orkestrering%20av%20bidragsberegning%20-%20forretningsregler.md) – regler for orkestrering på tvers av barn og vedtakstyper
- [Beregning av bidragsevne](bidrag-beregn-barnebidrag/docs/Beregning%20av%20bidragsevne%20-%20forretningsregler.md)
- [Beregning av underholdskostnad](bidrag-beregn-barnebidrag/docs/Beregning%20av%20underholdskostnad%20-%20forretningsregler.md)
- [Beregning av BPs andel av underholdskostnad](bidrag-beregn-barnebidrag/docs/Beregning%20av%20BPs%20andel%20av%20underholdskostnad%20-%20forretningsregler.md)
- [Beregning av samværsfradrag](bidrag-beregn-barnebidrag/docs/Beregning%20av%20samværsfradrag%20-%20forretningsregler.md)
- [Beregning av netto barnetillegg](bidrag-beregn-barnebidrag/docs/Beregning%20av%20netto%20barnetillegg%20-%20forretningsregler.md)
- [Beregning av netto tilsynsutgift](bidrag-beregn-barnebidrag/docs/Beregning%20av%20netto%20tilsynsutgift%20-%20forretningsregler.md)
- [Beregning av endelig bidrag V2](bidrag-beregn-barnebidrag/docs/Beregning%20av%20endelig%20bidrag%20V2%20-%20forretningsregler.md)
- [Beregning av endring sjekk grense](bidrag-beregn-barnebidrag/docs/Beregning%20av%20endring%20sjekk%20grense%20-%20forretningsregler.md)

Dokumentasjonen kan holdes oppdatert automatisk via [docs-agenten](bidrag-beregn-barnebidrag/docs/agents/).

---

## Kontakt

Slack: [#team-bidrag](https://nav-it.slack.com/archives/CAZ7A2074)

<!---------------------------------------------------------------------------->

[Publish button]: https://img.shields.io/badge/Publiser_siste_release_draft-37a779?style=for-the-badge
[Release draft]: https://github.com/navikt/bidrag-beregn-felles/releases
