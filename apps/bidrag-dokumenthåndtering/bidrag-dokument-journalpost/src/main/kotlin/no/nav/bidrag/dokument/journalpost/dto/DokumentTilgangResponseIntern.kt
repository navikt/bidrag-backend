package no.nav.bidrag.dokument.journalpost.dto

import no.nav.bidrag.transport.dokument.DokumentTilgangResponse

data class DokumentTilgangResponseIntern(
    var dokumentUrl: String = "",
    var type: String = "",
) {
    fun lagResponseDto() = DokumentTilgangResponse(dokumentUrl, type)
}
