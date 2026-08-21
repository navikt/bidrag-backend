package no.nav.bidrag.dokument.arkivering.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.arkivering.config.BidragDokumentArkiveringConfig
import no.nav.bidrag.dokument.arkivering.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.arkivering.consumer.JournalpostApiConsumer
import no.nav.bidrag.dokument.arkivering.dto.ArkivereJournalpostResponse
import no.nav.bidrag.dokument.arkivering.dto.AvvikHendelseIntern
import no.nav.bidrag.dokument.arkivering.dto.JournalpostStatus
import no.nav.bidrag.dokument.arkivering.dto.validereJournalpostResponse
import no.nav.bidrag.dokument.arkivering.exceptions.ArkiveringAvDokumentFeiletException
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostHarIkkeGyldigStatusException
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkivering.exceptions.JournalpostKanIkkeArkiveres
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.function.Consumer
import kotlin.collections.ArrayList

private val log = KotlinLogging.logger {}

@Service
class ArkiveringService(
    private val bidragDokumentConsumer: BidragDokumentConsumer,
    private val journalpostApiConsumer: JournalpostApiConsumer,
) {
    fun arkivereJournalpost(jpId: String): ArkivereJournalpostResponse {
        val journalpostConsumerRespons = bidragDokumentConsumer.hentBidragJournalpost(jpId)
        val journalpostResponse =
            journalpostConsumerRespons.fetchBody().orElse(JournalpostResponse())
        if (harArkivertJournalpostIJoark(journalpostResponse)) {
            val joarkJournalpostId = journalpostResponse.journalpost!!.joarkJournalpostId
            log.info {
                "Bidrag journalpost $jpId er allerede arkivert i Joark med id $joarkJournalpostId. Stopper videre behandling"
            }

            oppdaterJournalpostArkiveringSuksessHvisIkkeRiktigStatus(jpId, journalpostResponse)
            return ArkivereJournalpostResponse(jpId, journalpostResponse)
        }
        validateJournalpost(jpId, journalpostConsumerRespons.fetchBody())
        return utforArkiverJournalpost(jpId, journalpostResponse)
    }

    private fun oppdaterJournalpostArkiveringSuksessHvisIkkeRiktigStatus(
        jpId: String,
        journalpostResponse: JournalpostResponse,
    ) {
        val journalStatus = journalpostResponse.journalpost!!.journalstatus
        if (journalStatus != JournalpostStatus.EKSPEDERT_JOARK) {
            val joarkJournalpostId = journalpostResponse.journalpost!!.joarkJournalpostId
            val journalforendeEnhet = journalpostResponse.journalpost!!.journalforendeEnhet!!
            val sakstilknytninger = journalpostResponse.sakstilknytninger
            oppdaterSakerStatusSuksess(
                jpId,
                journalforendeEnhet,
                sakstilknytninger,
                joarkJournalpostId,
            )
        }
    }

    private fun harArkivertJournalpostIJoark(journalpostResponse: JournalpostResponse): Boolean = Objects.nonNull(journalpostResponse.journalpost) &&
        Objects.nonNull(
            journalpostResponse.journalpost!!.joarkJournalpostId,
        )

    private fun validateJournalpost(
        jpId: String,
        journalpostResponseOptional: Optional<JournalpostResponse>,
    ) {
        if (journalpostResponseOptional.isEmpty) {
            throw JournalpostIkkeFunnetException(jpId)
        }
        val journalpostResponse = journalpostResponseOptional.get()
        if (!harJournalpostGyldigStatus(journalpostResponse.journalpost!!)) {
            throw JournalpostHarIkkeGyldigStatusException(jpId)
        }
        val (kanArkivere, reason) = bidragDokumentConsumer.kanArkivereJournalpost(jpId)
        if (!kanArkivere) {
            throw JournalpostKanIkkeArkiveres(jpId, reason)
        }
        validereJournalpostResponse(jpId, journalpostResponse)
    }

    private fun utforArkiverJournalpost(
        jpId: String,
        journalpostResponse: JournalpostResponse,
    ): ArkivereJournalpostResponse {
        val journalforendeEnhet = journalpostResponse.journalpost!!.journalforendeEnhet!!
        val sakstilknytninger = journalpostResponse.sakstilknytninger
        return try {
            oppdaterSakerStatusStartet(jpId, journalforendeEnhet, sakstilknytninger)
            val arkiverResponse = arkiverJournalpost(journalpostResponse)
            oppdaterSakerStatusSuksess(
                jpId,
                journalforendeEnhet,
                sakstilknytninger,
                arkiverResponse.jpIdJoark,
            )
            //      tilknyttSakerTilJournalpost(arkiverResponse.getJpIdJoark(), journalforendeEnhet, sakstilknytninger);
            arkiverResponse
        } catch (e: Exception) {
            oppdaterSakerStatusFeilet(jpId, journalforendeEnhet, sakstilknytninger)
            throw e
        }
    }

    private fun arkiverJournalpost(journalpostResponse: JournalpostResponse): ArkivereJournalpostResponse {
        val fysiskDokument =
            bidragDokumentConsumer.hentDokument(journalpostResponse.journalpost!!.journalpostId!!)
        // Opprette journalpost med dokument i Joark for arkivering
        val response =
            journalpostApiConsumer.arkivereJournalpost(journalpostResponse, fysiskDokument)
        if (!response.is2xxSuccessful()) {
            throw ArkiveringAvDokumentFeiletException()
        }
        return response.fetchBody().get()
    }

    private fun tilknyttSakerTilJournalpost(
        joarkJournalpostId: String,
        journalforendeEnhet: String,
        saker: List<String>,
    ) {
        bidragDokumentConsumer.tilknyttSakerTilJoarkJournalpost(
            joarkJournalpostId,
            journalforendeEnhet,
            saker,
        )
    }

    private fun oppdaterSakerStatusFeilet(
        jpId: String,
        journalforendeEnhet: String,
        sakstilknytninger: List<String>,
    ) {
        val avvikHendelseRequests: MutableList<Avvikshendelse> = ArrayList()
        sakstilknytninger.forEach(
            Consumer { saksnummer: String ->
                avvikHendelseRequests.add(
                    AvvikHendelseIntern(saksnummer = saksnummer).toAvvikHendelseArkiverFeilet(),
                )
            },
        )
        oppdaterSakerStatus(jpId, journalforendeEnhet, avvikHendelseRequests)
    }

    private fun oppdaterSakerStatusStartet(
        jpId: String,
        journalforendeEnhet: String,
        sakstilknytninger: List<String>,
    ) {
        val avvikHendelseRequests: MutableList<Avvikshendelse> = ArrayList()
        sakstilknytninger.forEach(
            Consumer { saksnummer: String ->
                avvikHendelseRequests.add(
                    AvvikHendelseIntern(saksnummer = saksnummer).toAvvikHendelseArkiverStartet(),
                )
            },
        )
        oppdaterSakerStatus(jpId, journalforendeEnhet, avvikHendelseRequests)
    }

    private fun oppdaterSakerStatusSuksess(
        jpId: String,
        journalforendeEnhet: String,
        sakstilknytninger: List<String>,
        joarkJournalpostId: String?,
    ) {
        val avvikHendelseRequests: MutableList<Avvikshendelse> = ArrayList()
        sakstilknytninger.forEach(
            Consumer { saksnummer: String ->
                avvikHendelseRequests.add(
                    AvvikHendelseIntern(
                        joarkJournalpostId,
                        saksnummer,
                    ).toAvvikHendelseArkiverFullfort(),
                )
            },
        )
        oppdaterSakerStatus(jpId, journalforendeEnhet, avvikHendelseRequests)
    }

    private fun oppdaterSakerStatus(
        jpId: String,
        journalforendeEnhet: String,
        avvikHendelseRequests: List<Avvikshendelse>,
    ) {
        avvikHendelseRequests
            .forEach(
                Consumer { avvikHendelseRequest: Avvikshendelse ->
                    log.info {
                        "Sender avvik arkiver journalpost med arkiver status " +
                            "${avvikHendelseRequest.detaljer[AvvikHendelseIntern.AVVIK_DETAIL_JOARK_STATUS]}"
                    }
                    bidragDokumentConsumer.sendAvvikHendelse(
                        jpId,
                        "BID-",
                        journalforendeEnhet,
                        avvikHendelseRequest,
                    )
                },
            )
    }

    private fun harJournalpostGyldigStatus(journalpostDto: JournalpostDto): Boolean = BidragDokumentArkiveringConfig.BIDRAG_JOURNALPOSTSTATUS_RESERVERT == journalpostDto.journalstatus ||
        BidragDokumentArkiveringConfig.BIDRAG_JOURNALPOSTSTATUS_KLAR_TIL_PRINT == journalpostDto.journalstatus
}
