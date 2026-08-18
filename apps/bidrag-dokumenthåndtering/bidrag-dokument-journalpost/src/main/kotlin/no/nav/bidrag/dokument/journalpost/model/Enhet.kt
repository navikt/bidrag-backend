package no.nav.bidrag.dokument.journalpost.model

data class Enhet(
    var navn: String? = null,
    var enhetNr: String? = null,
    var orgNivaa: String? = null,
) {
    fun hentEnhetsinformasjon() = "${hentOrgNivaa()}$enhetNr${hentNavn()}"

    private fun hentOrgNivaa(): String = when (orgNivaa) {
        "EN" -> "enhet "
        "SPESEN" -> "spesialenhet "
        else -> ""
    }

    private fun hentNavn(): String = navn?.let { " - $it" } ?: ""
}

data class SaksbehandlersEnhet(
    val enhetsnummer: String,
)
