package no.nav.bidrag.dokument.journalpost.dto

data class Saksbehandler(
    var ident: String? = null,
    var navn: String? = null,
) {
    fun hentIdentMedNavn() = "$ident - $navn"

    fun hentSaksbehandlerInfo(journalforendeEnhet: String) = "$navn ($ident - $journalforendeEnhet)"

    fun tilEnhet(enhetsnummer: String?): SaksbehandlerMedEnhet = SaksbehandlerMedEnhet(this, enhetsnummer)
}

data class SaksbehandlerMedEnhet(
    val saksbehandler: Saksbehandler,
    val enhetsnummer: String?,
)
