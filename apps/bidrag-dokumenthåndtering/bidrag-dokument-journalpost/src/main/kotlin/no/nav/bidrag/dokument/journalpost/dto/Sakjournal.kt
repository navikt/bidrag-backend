package no.nav.bidrag.dokument.journalpost.dto

data class Sakjournal(
    val saksnummer: String,
    val fagomrade: List<String>,
    val medFeilforte: Boolean?,
) {
    constructor(saksnummer: String, fagomrade: String) : this(saksnummer, listOf(fagomrade), null)
}
