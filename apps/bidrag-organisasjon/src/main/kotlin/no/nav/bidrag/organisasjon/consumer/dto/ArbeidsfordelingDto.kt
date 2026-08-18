package no.nav.bidrag.organisasjon.consumer.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.organisasjon.Enhetsnummer

data class ArbeidsfordelingEnheterRequest(
    val typeListe: List<String> = emptyList(),
    val tema: String = "",
)

data class ArbeidsfordelingEnheterRequestBody(
    val tema: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsfordelingEnheterResponse(
    val enhetNr: Enhetsnummer? = null,
    val navn: String? = null,
    val type: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsfordelingEnheterBestMatchRequest(
    val diskresjonskode: Diskresjonskode? = null,
    val tema: String = "",
    val geografiskOmraade: String? = "",
    val skjermet: Boolean = false,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsfordelingEnheterBestMatchResponse(
    val enhetNr: Enhetsnummer,
    val navn: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnhetArbeidsfordelingRespons(
    val tema: String,
    val enhetNavn: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnhetInfoResponse(
    val enhetId: String? = null,
    val enhetNr: Enhetsnummer,
    val navn: String? = null,
    val organisasjonsnummer: String? = null,
    val status: String? = null,
) {
    fun erNedlagt() = status == "Nedlagt"
}

data class EnhetKontaktinformasjonJson(
    val spraak: String,
    val enhetsnr: Enhetsnummer,
    val navn: String? = null,
    val telefonnummer: String? = null,
    val returadresse: EnhetAdresseJson? = null,
    val postadresse: EnhetAdresseJson? = null,
    val besoksadresse: EnhetAdresseJson? = null,
)

data class EnhetAdresseJson(
    val enhetsnr: Enhetsnummer? = null,
    val enhetsnavn: String? = null,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val postnr: String? = null,
    val poststed: String? = null,
    val land: String? = null,
    val kommunenr: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnhetKontakinformasjonResponse(
    val enhetNr: String? = null,
    val telefonnummer: String? = null,
    val postadresse: EnhetPostadresseResponse? = null,
)

data class EnhetPostadresseResponse(
    val type: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val postboksnummer: String? = null,
    val postboksanlegg: String? = null,
)
