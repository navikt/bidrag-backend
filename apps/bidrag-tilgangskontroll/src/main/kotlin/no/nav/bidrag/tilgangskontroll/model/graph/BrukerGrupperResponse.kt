package no.nav.bidrag.tilgangskontroll.model.graph

import com.fasterxml.jackson.annotation.JsonProperty

data class EnhetResponse(
    val value: List<Gruppe>,
)

data class BrukerGrupperResponse(
    val value: List<Gruppe>?,
)

data class BrukerEnheterRespons(
    val enhetIder: List<String>,
)

class BrukerinformasjonResponse(
    @param:JsonProperty("@odata.nextLink")
    val nextLink: String?,
    val value: List<Brukerinformasjon>?,
)

data class CheckMemberGroupsResponse(
    val value: List<String>?,
)

data class Gruppe(
    @param:JsonProperty("id")
    val id: String?,
    @param:JsonProperty("description")
    val beskrivelse: String?,
    @param:JsonProperty("displayName")
    val navn: String?,
)

enum class Søknadsgruppe(
    val enheter: List<String>,
) {
    BARNEBORTFØRING(listOf("2103", "4883", "4849")),
    EKTEFELLEBIDRAG(listOf("2103", "4883", "4849", "4865")),
    OPPFOSTRINGSBIDRAG(listOf("2103", "4883", "4833")),
    REISEUTGIFTER(listOf("2103", "4883", "4849")),
}
