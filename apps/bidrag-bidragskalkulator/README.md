# 📊 Bidragskalkulator API

**Bidragskalkulator API** beregner hvor mye en forelder skal betale eller motta i barnebidrag.

Applikasjonen fungerer som backend for [bidrag-bidragskalkulator-ui](https://github.com/navikt/bidrag-bidragskalkulator-ui) og er bygget med **Spring Boot**.

---

## 🚀 Teknologi

- **Språk:** Kotlin
- **Rammeverk:** Spring Boot
- **Byggeverktøy:** Maven
- **Dokumentasjon:** OpenAPI / Swagger

---

## 📌 Kom i gang

### 🚧 Krav

For å kjøre applikasjonen lokalt, må følgende være installert:

- Java 21
- Maven
- (Ved kjøring mot sky) `gcloud` og `kubectl` (Se [cammand line access](https://doc.nais.io/operate/how-to/command-line-access/))

---

### 📌 Kjøre applikasjonen lokalt

**Merk**: Ved lokal kjøring vil du kun ha tilgang til endepunkter som ikke er beskyttet (altså åpne endepunkter). For å kalle beskyttede endepunkter kreves autentisering via gyldig token. Se **Kjøre applikasjonen lokalt mot sky (nais)**

Du kan starte applikasjonen lokalt enten via terminalen eller direkte i din IDE.

#### 🖥️ Alternativ 1: Kjøre via terminal
Bygg og start applikasjonen med **local profilen** ved å kjøre (fra rotmappen til monorepoet):

```bash
mvn spring-boot:run -pl apps/bidrag-bidragskalkulator -am -Dspring-boot.run.profiles=local
```

#### 🖥️ Alternativ 2: Kjøre via IntelliJ IDEA

1. Høyreklikk på BidragBidragskalkulatorApiApplication.kt
2. Velg **More Run/Debug**
3. Klikk på **Modify Run Configuration**
4. Under **Active Profiles**, legg til "local"
5. Klikk **Apply** og deretter **OK**
6. Start BidragBidragskalkulatorApiApplication.kt

---

### ☁️ Kjøre applikasjonen lokalt mot sky (nais)

**Merk**: `gcloud` og `kubectl` må være installert. (Se [cammand line access](https://doc.nais.io/operate/how-to/command-line-access/))

For å kjøre applikasjonen lokalt mot sky, følg disse stegene:

##### 1. Logg inn i gcp
```bash
gcloud auth login
```

##### 2. Sette nødvendige miljøvariabler med skripten
```bash
./initEnv.sh
```

#### 3. Start applikasjonen med local-nais-profil

Du kan starte applikasjonen på to måter:

**✅ Via terminal:**
```bash
mvn spring-boot:run -pl apps/bidrag-bidragskalkulator -am -Dspring-boot.run.profiles=local-nais
```

✅ **Eller kjør BidragBidragskalkulatorApiApplication.kt** i en IDE med profilen local-nais.

#### 4. Generer token for autentisering

For å kunne autentisere deg mot API-et via Swagger (Authorize), må du generere et gyldig token.

Gå til TokenX Token Generator:

🔗 [TokenX Token Generator](https://tokenx-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp:bidrag:bidrag-bidragskalkulator-api)

---

### 🧪 Testing

Alle pull requests kjører automatisk gjennom en test-pipeline som:

✔ Bygger applikasjonen <br>
✔ Kjører alle tester

```bash
mvn test -pl apps/bidrag-bidragskalkulator -am
```
---

### 📜 API-dokumentasjon

- **Lokal:** http://localhost:8080/swagger-ui/index.html
- **Dev:** https://bidragskalkulator-api.intern.dev.nav.no/swagger-ui/index.html
- **Prod:** https://bidragskalkulator-api.intern.nav.no/swagger-ui/index.html

---

### 🚀 Deployments

#### Automatisk deploy til prod

Applikasjonen deployer automatisk til prod-miljøet når man merger en pull request til
`main`-branchen.

#### Deploy til q1

Push til en branch som starter med `q1/` (f.eks. `q1/min-endring`) trigger automatisk
deploy til q1-miljøet.

Deploy kan også trigges manuelt via "Run workflow" på
[Bidrag-bidragskalkulator](../../.github/workflows/bidrag-bidragskalkulator.yaml)-workflowen
i GitHub Actions, med ønsket miljø som input.

### 👥 Eierskap

**Team Bidragskalkulator**, en del av **Team Bidrag** i PO Familie, er ansvarlig for vedlikehold av denne applikasjonen.

For spørsmål eller bidrag, ta kontakt med teamet i NAV sin interne Slack-kanal.
