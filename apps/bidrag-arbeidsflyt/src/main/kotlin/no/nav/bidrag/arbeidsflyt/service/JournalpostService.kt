package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.consumer.BidragDokumentConsumer
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import org.springframework.stereotype.Service

internal val JournalpostResponse.erBidragFagomrade get(): Boolean = journalpost?.fagomrade == Fagomrade.BIDRAG || journalpost?.fagomrade == Fagomrade.FARSKAP
internal val JournalpostResponse.erFarskap get(): Boolean = journalpost?.fagomrade == Fagomrade.FARSKAP

@Service
class JournalpostService(
    private val bidragDokumentConsumer: BidragDokumentConsumer,
) {
    companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }

    fun hentJournalpostMedStatusMottatt(journalpostId: String): JournalpostResponse? {
        val journalpost = bidragDokumentConsumer.hentJournalpost(journalpostId)
        LOGGER.debug { "Hentet journalpost ${journalpostId.sanitizeForLog()} fra bidrag-dokument" }
        return journalpost.takeIf { it?.journalpost?.status == JournalpostStatus.MOTTATT }
    }

    fun hentJournalpost(journalpostId: String): JournalpostResponse? = bidragDokumentConsumer.hentJournalpost(journalpostId)
}
