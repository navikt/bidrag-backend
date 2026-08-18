package no.nav.bidrag.dokument.journalpost.job

import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.exception.HentingAvDokumentFeiletException
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.dokument.journalpost.service.DokumentService
import no.nav.bidrag.transport.dokument.DokumentType
import no.nav.bidrag.transport.dokument.JournalpostStatus
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

data class OppdaterStatusPåDokumenterUnderProduksjonResultDto(
    val journalposterMedDokumentSomErFerdigstilt: MutableList<JournalpostDokumentDto>,
    val journalposterMedDokumentSomIkkeErFerdigstilt: MutableList<JournalpostDokumentDto>,
    val journalposterHvorDokumentErSlettet: MutableList<JournalpostDokumentDto>,
) {
    data class JournalpostDokumentDto(
        val journalpostId: Int,
        val dokumentreferanse: String,
        val tittel: String?,
        val brevkode: String?,
        val journalforendeEnhet: String?,
        val journalforendeEnhetNavn: String?,
        val status: JournalpostStatus?,
        val dokumentdato: LocalDate?,
        val journaldato: LocalDate?,
        val doktype: String,
        var bleFerdigstilt: Boolean,
    )
}

data class OppdaterStatusPåDokumenterUnderProduksjonRequestDto(
    val simuler: Boolean = true,
    val startFraPeker: Int = 0,
    val sjekkForAntallJournalposter: Int = 100,
)

val rensetDokRefMarkeringSlettet = "SJEKKET_DOKUMENT_STATUS_SLETTET"
val rensetDokRefMarkeringUnderProduksjon = "SJEKKET_DOKUMENT_STATUS_UNDER_PRODUKSJON"
val rensetDokRefMarkeringFerdigstilt = "SJEKKET_DOKUMENT_STATUS_FERDIGSTILT"

@Component
class OppdaterStatusPåDokumenterService(
    private val journalpostRepository: JournalpostRepository,
    private val dokumentService: DokumentService,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(OppdaterStatusPåDokumenterService::class.java)
    }

    @Transactional
    fun oppdaterStatusPåJournalposterSomHarStatusUnderProduksjon(
        request: OppdaterStatusPåDokumenterUnderProduksjonRequestDto,
    ): OppdaterStatusPåDokumenterUnderProduksjonResultDto {
        val journalposter =
            journalpostRepository.hentJournalposterMedStatusUnderProduksjon(request.startFraPeker, request.sjekkForAntallJournalposter)
        LOGGER.info(
            "Sjekker for ${journalposter.size} journalposter med dokumenter som har status UNDER_PRODUKSJON." +
                " Simuler=${request.simuler}, starter fra peker=${request.startFraPeker}, sjekker for antall=${request.sjekkForAntallJournalposter}",
        )
        val resultat =
            OppdaterStatusPåDokumenterUnderProduksjonResultDto(
                journalposterMedDokumentSomErFerdigstilt = mutableListOf(),
                journalposterMedDokumentSomIkkeErFerdigstilt = mutableListOf(),
                journalposterHvorDokumentErSlettet = mutableListOf(),
            )
        journalposter.forEach {
            LOGGER.info(
                "Sjekker om journalpost med id ${it.journalpostId} og status ${it.journalstatus} har dokumenter tilgjengelig og er ferdigstilt",
            )
            val journalpost = it.tilRespons()
            try {
                if (!request.simuler) {
                    it.dokStatusSjekket = LocalDateTime.now()
                }
                val erFerdigstilt = dokumentService.erFerdigstilt(it.dokumentreferanse)
                if (erFerdigstilt) {
                    resultat.journalposterMedDokumentSomErFerdigstilt.add(journalpost)
                    val melding =
                        if (request.simuler) {
                            "Gjør ingen endringer pga at kjøringen er i simuleringsmodus"
                        } else {
                            journalpost.bleFerdigstilt = true
                            it.journalstatus = Journalstatus.RESERVERT
                            "Oppdatert status til ${Journalstatus.RESERVERT}"
                        }
                    LOGGER.info(
                        "Journalpost med id ${it.journalpostId}, dokumentreferanse ${it.dokumentreferanse} og type ${journalpost.doktype} er ferdigstilt. $melding",
                    )
                } else {
                    resultat.journalposterMedDokumentSomIkkeErFerdigstilt.add(journalpost)
                    LOGGER.info(
                        "Journalpost med id ${it.journalpostId} og dokumentreferanse ${it.dokumentreferanse} er fortsatt under produksjon.",
                    )
                }
            } catch (e: HentingAvDokumentFeiletException) {
                resultat.journalposterHvorDokumentErSlettet.add(journalpost)
                if (!request.simuler) {
                    it.journalstatus = Journalstatus.DOKUMENT_SLETTET
                }
                LOGGER.warn(
                    "Feil ved henting av dokument for journalpost med id ${it.journalpostId} og dokumentreferanse ${it.dokumentreferanse}. Dokumentet finnes mest sannsynlig ikke lenger i brevlageret. Setter status til ${Journalstatus.DOKUMENT_SLETTET}. Feilmelding: ${e.message}",
                )
            }
        }
        return resultat
    }

    @Transactional
    fun hentSjekketStatusPåJournalposterSomHarStatusUnderProduksjon(
        request: OppdaterStatusPåDokumenterUnderProduksjonRequestDto,
    ): OppdaterStatusPåDokumenterUnderProduksjonResultDto {
        val journalposter =
            journalpostRepository.hentJournalposterMarkertSjekket(request.startFraPeker, request.sjekkForAntallJournalposter)
        LOGGER.info(
            "Hentet ${journalposter.size} journalposter som har blitt sjekket for dokument status" +
                " Starter fra peker=${request.startFraPeker}, sjekker for antall=${request.sjekkForAntallJournalposter}",
        )
        val resultat =
            OppdaterStatusPåDokumenterUnderProduksjonResultDto(
                journalposterMedDokumentSomErFerdigstilt = mutableListOf(),
                journalposterMedDokumentSomIkkeErFerdigstilt = mutableListOf(),
                journalposterHvorDokumentErSlettet = mutableListOf(),
            )
        journalposter.forEach {
            val journalpost = it.tilRespons()
            if (it.journalstatus == Journalstatus.UNDER_PRODUKSJON) {
                resultat.journalposterMedDokumentSomIkkeErFerdigstilt.add(journalpost)
            } else if (it.journalstatus == Journalstatus.DOKUMENT_SLETTET) {
                resultat.journalposterHvorDokumentErSlettet.add(journalpost)
            } else if (it.journalstatus == Journalstatus.RESERVERT) {
                journalpost.bleFerdigstilt = true
                resultat.journalposterMedDokumentSomErFerdigstilt.add(journalpost)
            }
        }
        return resultat
    }

    fun Journalpost.tilRespons() = OppdaterStatusPåDokumenterUnderProduksjonResultDto.JournalpostDokumentDto(
        journalpostId = journalpostId,
        dokumentreferanse = dokumentreferanse,
        tittel = beskrivelse,
        dokumentdato = dokumentdato,
        journaldato = journaldato,
        brevkode = brevkode,
        journalforendeEnhet = journalforendeEnhet,
        journalforendeEnhetNavn = journalforendeEnhetNavn,
        status = JournalpostStatus.fraKode(journalstatus),
        doktype =
        when (dokumentType) {
            DokumentType.NOTAT -> "Notat"
            DokumentType.INNGÅENDE -> "Inngående"
            DokumentType.UTGÅENDE -> "Utgående"
            else -> dokumentType
        },
        bleFerdigstilt = false,
    )
}
