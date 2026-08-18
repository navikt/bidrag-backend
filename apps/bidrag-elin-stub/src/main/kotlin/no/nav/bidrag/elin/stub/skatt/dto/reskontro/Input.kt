package no.nav.bidrag.elin.stub.skatt.dto.reskontro

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Input er samme for alle endepunktene. Det eksisterer 6 forskjellige kombinasjoner av input som er gyldige for uthenting av informasjon fra skatt.
 * Disse er som følger:
 *
 * | Endepunkt      | Aksjonskode | Bidragssaksnummer | FodselsOrgnr | TransaksjonsId | DatoFom                  | DatoTom                  | MaxAntallTransaksjoner | FodselsnrGjelder | fodselsnrNy |
 * |----------------|-------------|-------------------|--------------|----------------|--------------------------|--------------------------|------------------------|------------------|-------------|
 * | Bidragssak     | 1           | 123456            | null         | null           | null                     | null                     | null                   | null             | null        |
 * | Bidragssak     | 2           | null              | 010101xxxxx  | null           | null                     | null                     | null                   | null             | null        |
 * | Transaksjoner  | 3           | 123456            | null         | null           | 2023-01-01T00:00:00.000Z | 2023-01-02T00:00:00.000Z | 10                     | null             | null        |
 * | Transaksjoner  | 4           | null              | 010101xxxxx  | null           | 2023-01-01T00:00:00.000Z | 2023-01-02T00:00:00.000Z | 10                     | null             | null        |
 * | Transaksjoner  | 5           | null              | null         | 1234           | 2023-01-01T00:00:00.000Z | 2023-01-02T00:00:00.000Z | 10                     | null             | null        |
 * | Innkrevingssak | 6           | null              | 010101xxxxx  | null           | null                     | null                     | null                   | null             | null        |
 * | EndreRm        | 8           | 123456            | null         | null           | null                     | null                     | null                   | 010101xxxxx      | 010170xxxxx |
 */

data class Input(
    @field:Schema(
        description =
        "Aksjonskoden bestemmes ut i fra hvilken informasjon som skal uthentes. " +
            "I utgangspunktet er aksjonskoden ikke nødvendig da input er unike for alle mulige kall mot reskontro.",
    )
    val aksjonskode: Int,
    @field:Schema(
        description = "Må sette for kall mot bidragssak på saksnummer, transaksjoner på saksnummer, og på endreRm.",
    )
    val bidragssaksnummer: Long? = null,
    @field:Schema(
        description = "Må sette for kall mot bidragssak på personident, transaksjoner på personident, og for innkrevingssak.",
    )
    val fodselsOrgnr: String? = null,
    @field:Schema(
        description = "Må sette for kall mot transaksjoner på transaksjonsid.",
    )
    val transaksjonsId: Long? = null,
    @field:Schema(
        description = "Må sette for kall mot transaksjoner uansett input. Merk: Denne setter vi automatisk i bidrag-regnskap.",
    )
    val datoFom: String? = null,
    @field:Schema(
        description = "Må sette for kall mot transaksjoner uansett input. Merk: Denne setter vi automatisk i bidrag-regnskap.",
    )
    val datoTom: String? = null,
    @field:Schema(
        description = "Må sette for kall mot transaksjoner uansett input. Merk: Denne setter vi automatisk i bidrag-regnskap.",
    )
    val maxAntallTransaksjoner: Int? = null,
    @field:Schema(
        description = "Må sette for kall mot endre rm. Dette er barnet i saken.",
    )
    val fodselsnrGjelder: String? = null,
    @field:Schema(
        description = "Må sette for kall mot endre rm. Dette er ny RM i saken.",
    )
    val fodselsnrNy: String? = null,
)
