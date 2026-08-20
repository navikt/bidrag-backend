# bidrag-backend

Monorepo for team Bidrag sine backend-tjenester. Bygget med Spring Boot og Kotlin
på Java 21, kjører på Nais.

## Automatisk PR-beskrivelse

Når du oppretter en pull request, legger workflowen
[`pr_beskrivelse.yaml`](.github/workflows/pr_beskrivelse.yaml) inn en oversikt
over endringene i PR-beskrivelsen: et kort sammendrag skrevet av Copilot, og en
tabell over hvilke maven-moduler som er berørt med antall filer og linjer.

Innholdet legges i en markert blokk:

```markdown
<!-- pr-beskrivelse:start -->
...generert innhold...
<!-- pr-beskrivelse:slutt -->
```

**Teksten du selv skriver utenfor blokken blir aldri rørt.** Alt inne i blokken
blir derimot overskrevet ved neste kjøring, så ikke rediger den.

### Når kjører den

| Hendelse | Modultabell | AI-sammendrag |
| --- | --- | --- |
| PR opprettet eller gjenåpnet | ✅ | ✅ |
| Draft merket som klar for review | ✅ | ✅ |
| Ny push til PR-en | ✅ | ♻️ beholdes fra forrige kjøring |
| Etiketten `oppdater-beskrivelse` settes på | ✅ | ✅ |

Modultabellen er ren git-utregning og oppdateres derfor på hver push.
AI-sammendraget koster å kjøre, så det
genereres bare når PR åpnes. Trenger du et nytt
sammendrag etter PR er opprettet, sett på etiketten `oppdater-beskrivelse` (fjern den
etterpå for å kunne bruke den igjen).

Draft-PR-er får tabellen, men ikke AI-sammendrag — det kommer først når PR-en
merkes klar for review. PR-er fra forks hoppes helt over, siden de får et
read-only token og uansett ikke kan oppdatere beskrivelsen. Feiler
Copilot-kallet, blokkerer det ikke PR-en: tabellen legges inn, forrige
sammendrag beholdes, og årsaken logges som en advarsel i jobben.