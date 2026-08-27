@file:Suppress("unused")

package no.nav.bidrag.domene.enums.sak

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Arbeidsfordeling(
    val visningsnavn: String,
    val bisysKode: String,
    val behandlingstema: String?,
) {
    // Full descriptive enum values (preferred)
    @JsonProperty("BBF")
    @JsonAlias("BARNEBORTFØRING")
    BARNEBORTFØRING("Barnebortføring", "BBF", "ab0323"),

    @JsonProperty("EEN")
    @JsonAlias("EIERENHET")
    EIERENHET("Eierenhet", "EEN", null),

    @JsonProperty("EFS")
    @JsonAlias("EKTEFELLLESAK")
    EKTEFELLLESAK("Ektefellesak", "EFS", "ab0325"),

    @JsonProperty("FRS")
    @JsonAlias("FARSKAP")
    FARSKAP("Farskap", "FRS", "ab0322"),

    @JsonProperty("INH")
    @JsonAlias("SETTEKONTOR")
    SETTEKONTOR("Settekontor", "INH", null),

    @JsonProperty("OPS")
    @JsonAlias("OPPFOSTRINGSSAK")
    OPPFOSTRINGSSAK("Oppfostringssak", "OPS", "ab0324"),

    @JsonProperty("RKS")
    @JsonAlias("REISEKOSTNADSAK")
    REISEKOSTNADSAK("Reisekostnadsak", "RKS", "ab0129"),
}
