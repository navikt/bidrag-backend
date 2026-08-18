package no.nav.bidrag.dokument.journalpost.service

import jakarta.transaction.Transactional
import no.nav.bidrag.dokument.journalpost.consumer.BidragPersonConsumer
import no.nav.bidrag.dokument.journalpost.consumer.NorgConsumer
import no.nav.bidrag.dokument.journalpost.dto.AvsenderMottaker
import no.nav.bidrag.dokument.journalpost.dto.OpprettUtgaaendeJournalpostIntern
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.entity.Journalsak
import no.nav.bidrag.dokument.journalpost.extensions.hentJournalførendeEnhet
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

val OpprettJournalpostRequest.brevkode get() = dokumenter[0].brevkode

@Service
class OpprettJournalpostService(
    val journalpostRepository: JournalpostRepository,
    val tokenInformationService: TokenInformationService,
    val personConsumer: BidragPersonConsumer,
    val kodeService: KodeService,
    val norgConsumer: NorgConsumer,
    val sakService: SakService,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(OpprettJournalpostService::class.java)
    }

    @Transactional
    fun opprettJournalpost(opprettJournalpostRequest: OpprettJournalpostRequest): OpprettJournalpostResponse {
        OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(opprettJournalpostRequest)
        val opprettetAvId = opprettJournalpostRequest.saksbehandlerIdent ?: tokenInformationService.hentSaksbehandlersBrukerid()
        val opprettetAvNavn = tokenInformationService.hentSaksbehandlersNavn(opprettJournalpostRequest.saksbehandlerIdent)
        val brevkode = kodeService.hentBrevKode(opprettJournalpostRequest.brevkode).orElse(null)
        val enhetInfo = norgConsumer.hentEnhetsinformasjon(opprettJournalpostRequest.hentJournalførendeEnhet()).orElse(null)
        val opprettJournalpostIntern =
            OpprettUtgaaendeJournalpostIntern(opprettJournalpostRequest, opprettetAvId, opprettetAvNavn, brevkode, enhetInfo)

        populerMedMottakerInformasjon(opprettJournalpostIntern)

        val opprettetJournalpost = journalpostRepository.save(Journalpost().opprett(opprettJournalpostIntern))

        opprettetJournalpost.leggTilDokumentreferanse()
        tilknyttTilSaker(opprettJournalpostIntern, opprettetJournalpost)

        LOGGER.info(
            "Opprettet journalpost med journalpostId=${opprettetJournalpost.journalpostId}, " +
                "dokumentReferanse=${opprettetJournalpost.dokumentreferanse}, brevkode=${opprettJournalpostRequest.brevkode} " +
                "og knyttet til saker ${opprettetJournalpost.journalsaker.joinToString(
                    ",",
                ) }}",
        )

        return OpprettJournalpostResponse(
            journalpostId = opprettetJournalpost.journalpostId.toString(),
            dokumenter =
            listOf(
                OpprettDokumentDto(
                    dokumentreferanse = opprettetJournalpost.dokumentreferanse,
                    tittel = opprettetJournalpost.beskrivelse,
                    brevkode = opprettetJournalpost.brevkode,
                ),
            ),
        )
    }

    fun tilknyttTilSaker(
        opprettJournalpost: OpprettUtgaaendeJournalpostIntern,
        opprettetJournalpost: Journalpost,
    ) {
        opprettJournalpost.tilknyttSaker.forEach { saksnummer: String? ->
            if (opprettetJournalpost.tilhorerIkkeSak(saksnummer)) {
                sakService.lagre(Journalsak(opprettetJournalpost, saksnummer))
                LOGGER.debug("Tilknyttet sak $saksnummer til journalpost ${opprettetJournalpost.journalpostId}")
            }
        }
    }

    fun populerMedMottakerInformasjon(opprettUtgaaendeJournalpostIntern: OpprettUtgaaendeJournalpostIntern) {
        if (!opprettUtgaaendeJournalpostIntern.harMottakerNavn()) {
            val mottakerId = opprettUtgaaendeJournalpostIntern.mottaker?.avsenderMottakerId!!
            personConsumer
                .hentPerson(mottakerId)
                .ifPresent {
                    LOGGER.debug("Mottakernavn mangler, populerer request objekt med mottakernavn. Mottakerid=$mottakerId")
                    opprettUtgaaendeJournalpostIntern.mottaker =
                        AvsenderMottaker(
                            avsenderNavn = it.navn ?: "",
                            avsenderMottakerId = mottakerId,
                        )
                }
        }
    }
}
