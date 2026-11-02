package no.nav.bidrag.dokument.arkiv.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.arkiv.consumer.InnsendingConsumer
import no.nav.bidrag.dokument.arkiv.consumer.dto.DokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.consumer.dto.HentEtterseningsoppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.service.utvidelser.tilRequest
import no.nav.bidrag.transport.dokument.OpprettEttersendingsppgaveDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
private val LOGGER = KotlinLogging.logger {}

@Service
class InnsendingService(private val innsendingConsumer: InnsendingConsumer) {

    fun hentEttersendingsoppgaver(gjelderId: String, skjemaId: String): List<DokumentSoknadDto> {
        try {
            return innsendingConsumer.hentEttersendingsoppgave(
                HentEtterseningsoppgaveRequest(
                    gjelderId,
                    skjemaId,
                ),
            )
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved henting av ettersendingsoppgaver" }
            return emptyList()
        }
    }

    fun hentEttersendingsoppgaverOpprettetEtterJournalpost(
        journalpost: Journalpost,
        ettersending: OpprettEttersendingsppgaveDto,
    ): List<DokumentSoknadDto> {
        try {
            val gjelder = journalpost.hentGjelderId()

            val eksisterendeOppgaver = hentEttersendingsoppgaver(
                gjelder!!,
                ettersending.skjemaId,
            )
            return eksisterendeOppgaver.filter { it.opprettetDato.toLocalDate() >= journalpost.hentDatoJournalfort() }
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved henting av ettersendingsoppgaver" }
            return emptyList()
        }
    }

    fun opprettEttersendingsoppgave(journalpost: Journalpost, ettersendingsoppgaveRequest: OpprettEttersendingsppgaveDto?): DokumentSoknadDto? {
        val ettersending = ettersendingsoppgaveRequest ?: run {
            journalpost.tilleggsopplysninger.hentInnsendingsoppgave()?.toOpprettEttersendingsoppgaveDto() ?: return null
        }
        if (journalpost.hentDatoJournalfort() != null &&
            journalpost.isDistribusjonBestilt() &&
            Period.between(journalpost.hentDatoJournalfort(), LocalDate.now()).days > 2
        ) {
            LOGGER.warn { "Journalpost ${journalpost.journalpostId} er eldre enn 2 dager. Oppretter ikke ettersendingsoppgave" }
            return null
        }

        journalpost.tilleggsopplysninger.hentInnsendingsoppgave()?.let {
            if (it.innsendingsId != null) {
                LOGGER.warn {
                    "Det finnes allerede en ettersendingsoppgave med innsendingsid=${it.innsendingsId} på journalpost ${journalpost.journalpostId}. Oppretter ikke på nytt"
                }
                return null
            }
        }

        val eksisterendeOppgaver = hentEttersendingsoppgaverOpprettetEtterJournalpost(journalpost, ettersending)
        if (eksisterendeOppgaver.isNotEmpty()) {
            LOGGER.warn {
                "Det finnes allerede ${eksisterendeOppgaver.size} ettersendingsoppgaver på journalpost ${journalpost.journalpostId} for skjema ${ettersending.skjemaId}."
            }
//            return eksisterendeOppgaver.maxBy { it.opprettetDato }
        }
        val oppgave = innsendingConsumer.opprettEttersendingsoppgave(
            ettersending.tilRequest(journalpost),
        )
        return oppgave
    }
}
