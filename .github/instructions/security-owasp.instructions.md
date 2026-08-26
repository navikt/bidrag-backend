---
applyTo: "**/*.{kt,go,java,ts,tsx}"
---

# Sikkerhetsprinsipper

For detaljerte OWASP Top 10:2025-mønstre på kodenivå (Kotlin, Go, Java, Node.js), bruk `$security-owasp`-skillen.

## Kritiske regler (gjelder alltid)

- **Kun parameteriserte spørringer** — aldri konkatener brukerinput inn i SQL/kommandoer
- **Ingen PII i logger** — ikke fnr, navn, adresse eller tokens i logg-setninger
- **Hemmeligheter fra environment** — hardkod aldri tokens, passord eller nøkler
- **Verifiser eierskap til ressurs** — ikke bare "er autentisert", men "eier denne ressursen"
- **Valider `azp` for M2M** — sjekk mot `AZURE_APP_PRE_AUTHORIZED_APPS`
- **TLS 1.2+** — sett aldri `InsecureSkipVerify: true` eller deaktiver sertifikatvalidering

For skanning-workflows (trivy, zizmor, govulncheck), bruk `$security-review`-skillen.
For trusselmodellering på arkitekturnivå, bruk `$threat-model`-skillen.
For å fikse CodeQL-/code-scanning-varsler korrekt, bruk `@codeql-fix`.
