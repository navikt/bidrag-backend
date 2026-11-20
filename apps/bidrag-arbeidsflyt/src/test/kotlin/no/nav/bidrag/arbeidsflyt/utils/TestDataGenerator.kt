package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.persistence.entity.Oppgave
import no.nav.bidrag.arbeidsflyt.persistence.entity.Sak
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.DLQKafkaRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.JournalpostRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.OppgaveRepository
import no.nav.bidrag.arbeidsflyt.persistence.repository.SakRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class TestDataGenerator {
    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @Autowired
    lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var dlqKafkaRepository: DLQKafkaRepository

    @Autowired
    lateinit var sakRepository: SakRepository

    fun opprettSak(saksnummer: String, eierfogd: String) {
        sakRepository.save(Sak(saksnummer = saksnummer, eierfogd = eierfogd))
    }

    fun hentSak(saksnummer: String): Sak? = sakRepository.findBySaksnummer(saksnummer)

    fun hentDlKafka(): List<DLQKafka> = dlqKafkaRepository.findAll()

    fun hentOppgave(oppgaveId: Long): Optional<Oppgave> = oppgaveRepository.findById(oppgaveId)

    fun opprettDLQMelding(dlqKafka: DLQKafka) {
        dlqKafkaRepository.save(dlqKafka)
    }

    fun opprettOppgave(oppgave: Oppgave) {
        oppgaveRepository.save(oppgave)
    }

    fun deleteAll() {
        dlqKafkaRepository.deleteAll()
        journalpostRepository.deleteAll()
        oppgaveRepository.deleteAll()
        behandlingRepository.deleteAll()
        sakRepository.deleteAll()
    }

    fun hentJournalpost(journalpostId: String): Journalpost? = journalpostRepository.findByJournalpostId(journalpostId)

    fun opprettJournalpost(journalpost: Journalpost) {
        journalpostRepository.save(journalpost)
    }
}
