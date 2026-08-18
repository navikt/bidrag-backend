# Bidrag Maskinporten Client

Applikasjonen gir Team Bidrag mulighet til å utstede gyldige tokens for test av endepunkter som krever Maskinporten-tokens. Applikasjonen kjører i intern sone på GCP og skal kun brukes i forbindelse med test. 

Applikasjonen ligger tilgjengelig på:
* [https://bidrag-maskinporten-client.dev.intern.nav.no/](https://bidrag-maskinporten-client.dev.intern.nav.no/) (dev-gcp)
* [https://bidrag-maskinporten-client.intern.nav.no/](https://bidrag-maskinporten-client.intern.nav.no/) (prod-gcp)

## Generere token

For å generere et token må man oppgi parameteren `scopes` som en kommaseparert liste. 

Eksempelvis:

```/token?scopes=nav:bidrag:scope1.read,nav:bidrag:scope2.read```

Ofte vil man kun være ute etter token for ett scope og da blir det slik:

```/token?scopes=nav:bidrag:scope1.read```

Dersom scopet du ber om ikke er definert i lista over støttede scopes vil du få `400-BadRequest`.

> MERK: Dersom et scope ikke har blitt registrert av tjenesten som krever det, 
> vil en klient konfigurert med det samme scopet heller ikke registreres korrekt. 
> Altså må tjenesten, som krever Maskinporten token med det bestemte scopet, deployes før klienten som ønsker å utstede tokens med samme scope. 
> Gjøres dette i "feil" rekkefølge må klienten redeployes for å få det til å fungere.

## Legge til flere støttede scopes

Applikasjonen kan enkelt utvides til å støtte flere scopes ved behov. For å legge til flere scopes må de legges til i `nais.yaml` under `maskinporten.scopes.consumes`:

```
maskinporten:
  enabled: true
  scopes:
    consumes:
      - name: "nav:bidrag:scope1.read"
      - name: "nav:bidrag:scope2.read"
      - name: ... flere scopes her
```

Disse scopene vil da dukke opp i environment-variabelen `MASKINPORTEN_SCOPES` og tjenesten vil kunne begynne å generere tokens med disse.

