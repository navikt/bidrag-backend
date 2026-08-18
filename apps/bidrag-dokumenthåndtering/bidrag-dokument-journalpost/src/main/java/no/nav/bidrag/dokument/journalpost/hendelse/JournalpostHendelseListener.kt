package no.nav.bidrag.dokument.journalpost.hendelse

import no.nav.bidrag.dokument.journalpost.service.TokenInformationService
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.Sporingsdata
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class JournalpostHendelseListener(
    private val journalpostKafkaEventProducer: JournalpostKafkaEventProducer,
    private val tokenInformationService: TokenInformationService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publish(journalpostHendelse: JournalpostHendelse) {
        val sporing = hentSporingMedSaksbehandlerInfo(journalpostHendelse.sporing!!)
        journalpostKafkaEventProducer.publish(
            journalpostHendelse.copy(
                sporing = sporing,
            ),
        )
    }

    private fun hentSporingMedSaksbehandlerInfo(sporingsdata: Sporingsdata): Sporingsdata {
        val muligSaksbehandler = tokenInformationService.hentSaksbehandler()
        return if (muligSaksbehandler.isPresent) {
            val (brukerident, saksbehandlersNavn) = muligSaksbehandler.get()
            sporingsdata.copy(
                brukerident = brukerident,
                saksbehandlersNavn = saksbehandlersNavn,
            )
        } else {
            sporingsdata.copy(
                brukerident = tokenInformationService.hentSaksbehandlersBrukerid(),
            )
        }
    }
}
