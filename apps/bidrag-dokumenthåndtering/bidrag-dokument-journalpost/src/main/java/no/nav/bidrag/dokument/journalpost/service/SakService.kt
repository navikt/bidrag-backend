package no.nav.bidrag.dokument.journalpost.service

import no.nav.bidrag.dokument.journalpost.entity.Journalsak
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory
import org.springframework.stereotype.Service

@Service
class SakService(
    private val journalsakReposistory: JournalsakReposistory,
) {
    fun lagre(journalsak: Journalsak) {
        journalsakReposistory.save(journalsak)
    }

    fun finn(saksnummer: String): List<Journalsak> = journalsakReposistory.findBySaksnummer(saksnummer)
}
