package no.nav.bidrag.dokument.journalpost.model

import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern

data class EndreJournalpostHendelseData(
    private val journalpostIntern: JournalpostIntern,
    val endreJournalpostCommandIntern: EndreJournalpostCommandIntern,
) {
    fun hentTilknyttSaker() = endreJournalpostCommandIntern.tilknyttSaker
}
