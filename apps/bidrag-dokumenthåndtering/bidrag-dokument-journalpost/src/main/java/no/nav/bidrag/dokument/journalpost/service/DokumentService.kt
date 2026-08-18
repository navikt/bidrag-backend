package no.nav.bidrag.dokument.journalpost.service

import no.nav.bidrag.dokument.journalpost.consumer.BrevserverConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentTilgangConsumer
import no.nav.bidrag.dokument.journalpost.dto.DokumentTilgangResponseIntern
import no.nav.bidrag.dokument.journalpost.dto.Dokumentbestilling
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.exception.DokumentIkkeFunnetException
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.journalpost.exception.KanIkkeHenteDokumentUnderProduksjon
import no.nav.bidrag.dokument.journalpost.exception.UgyldigJournalpostStatus
import no.nav.bidrag.dokument.journalpost.exception.journalpostIkkeFunnet
import no.nav.bidrag.dokument.journalpost.model.DokumentType
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.model.Journalstatus.SLETTET
import no.nav.bidrag.dokument.journalpost.model.Journalstatus.UNDER_PRODUKSJON
import no.nav.bidrag.dokument.journalpost.model.Journalstatus.UTGAR
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import no.nav.bidrag.transport.dokument.DokumentFormatDto
import no.nav.bidrag.transport.dokument.DokumentMetadata
import no.nav.bidrag.transport.dokument.DokumentStatusDto
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.util.Optional

private val LOGGER = LoggerFactory.getLogger(DokumentService::class.java)

val Journalpost.erUnderProduksjon get() = journalstatus == UNDER_PRODUKSJON

class DokumentService(
    private val brevserverUrl: String,
    private val systemId: String,
    private val journalpostService: JournalpostService,
    private val dokumentTilgangConsumer: DokumentTilgangConsumer,
    private val dokumentConsumer: DokumentConsumer,
    private val brevserverConsumer: BrevserverConsumer,
    private val brukBrevserverRest: Boolean = false,
) {
    fun lagTilgangUrl(dokumentreferanse: String): DokumentTilgangResponseIntern {
        val dokumentTilgang = dokumentTilgangConsumer.bestillDokumenttilgang(dokumentreferanse)
        val urlForDokumentreferanse = generateMbdokUrl(dokumentreferanse, dokumentTilgang.klientToken)
        return DokumentTilgangResponseIntern(urlForDokumentreferanse, "BREVLAGER")
    }

    fun hentDokument(
        journalpostId: Int?,
        dokumentReferanse: String?,
    ): ResponseEntity<ByteArray> {
        val journalpost =
            journalpostService
                .hentJournalpostEntitet(journalpostId)
                .orElseThrow { JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId)) }
        validerHentDokument(journalpost, dokumentReferanse)
        val dokRef = if (Strings.isNotEmpty(dokumentReferanse)) dokumentReferanse else journalpost.dokumentreferanse
        return hentDokument(dokRef!!)
    }

    fun hentDokumentRTF(
        journalpostId: Int?,
        dokumentReferanse: String?,
    ): ResponseEntity<ByteArray> {
        val journalpost =
            journalpostService
                .hentJournalpostEntitet(journalpostId)
                .orElseThrow { JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId)) }
        validerHentDokument(journalpost, dokumentReferanse)
        val dokRef = if (Strings.isNotEmpty(dokumentReferanse)) dokumentReferanse else journalpost.dokumentreferanse
        return hentDokumentRTF(dokRef!!)
    }

    fun hentDokumentRTF(dokumentReferanse: String): ResponseEntity<ByteArray> {
        val dokumentbestilling = bestillOgOpprettDokumenttilgang(dokumentReferanse)
        val dokumentByte = dokumentConsumer.henteDokumentRTF(dokumentbestilling).get()
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(dokumentByte)
    }

    fun hentDokument(dokumentReferanse: String): ResponseEntity<ByteArray> {
        val dokumentbestilling = bestillOgOpprettDokumenttilgang(dokumentReferanse)
        val dokumentByte =
            if (brukBrevserverRest) {
                LOGGER.info("Henter dokument $dokumentReferanse via REST fra brevserver")
                brevserverConsumer.hentDokument(dokumentbestilling.brevreferanse!!)
            } else {
                dokumentConsumer.henteDokument(dokumentbestilling).get()
            }
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=%s.pdf", dokumentReferanse))
            .body(dokumentByte)
    }

    fun erFerdigstilt(dokumentReferanse: String): kotlin.Boolean {
        val dokumentbestilling = bestillOgOpprettDokumenttilgang(dokumentReferanse)
        val erFerdigstilt = dokumentConsumer.erFerdigstilt(dokumentbestilling)
        return erFerdigstilt
    }

    fun ferdigstillNotat(dokumentReferanse: String): Boolean {
        if (erFerdigstilt(dokumentReferanse)) {
            val journalpost =
                journalpostService.hentJournalpostForDokumentReferanse(
                    dokumentReferanse,
                ) ?: journalpostIkkeFunnet("Fant ikke journalpost med dokumentreferanse=$dokumentReferanse")
            if (journalpost.dokumentType != DokumentType.NOTAT) {
                throw UgyldigJournalpostStatus("Kan kun ferdigstille notat, dokumentreferanse=$dokumentReferanse")
            }
            if (journalpost.journalstatus != UNDER_PRODUKSJON) {
                return true
            }
            journalpost.journalstatus = Journalstatus.RESERVERT
            journalpostService.lagreJournalpost(journalpost)
            return true
        }
        return false
    }

    fun hentDokumentMetadata(
        journalpostId: Int? = null,
        dokumentReferanse: String?,
    ): DokumentMetadata {
        val journalpost =
            if (journalpostId != null) {
                journalpostService.hentJournalpostEntitetForId(
                    journalpostId,
                ) ?: journalpostIkkeFunnet("Fant ikke journalpost med journalpostid=$journalpostId")
            } else {
                journalpostService.hentJournalpostForDokumentReferanse(
                    dokumentReferanse,
                ) ?: journalpostIkkeFunnet("Fant ikke journalpost med dokumentreferanse=$dokumentReferanse")
            }

        if (!dokumentReferanse.isNullOrEmpty() && journalpost.dokumentreferanse != dokumentReferanse) {
            journalpostIkkeFunnet(
                "Journalpost $journalpostId har ikke dokument med dokumentreferanse $dokumentReferanse",
            )
        }

        return DokumentMetadata(
            journalpostId = journalpost.journalpostId?.let { "BID-$it" },
            dokumentreferanse = dokumentReferanse ?: journalpost.dokumentreferanse,
            format = if (journalpost.erUnderProduksjon) DokumentFormatDto.MBDOK else DokumentFormatDto.MBDOK,
            status =
            when (journalpost.journalstatus) {
                UNDER_PRODUKSJON -> DokumentStatusDto.UNDER_REDIGERING
                SLETTET, UTGAR -> DokumentStatusDto.AVBRUTT
                else -> DokumentStatusDto.FERDIGSTILT
            },
            tittel = journalpost.beskrivelse,
            arkivsystem = DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER,
        )
    }

    private fun validerHentDokument(
        journalpost: Journalpost,
        dokumentReferanse: String?,
    ) {
        val journalpostId = journalpost.journalpostId
        if (Strings.isNotEmpty(dokumentReferanse) && dokumentReferanse != journalpost.dokumentreferanse) {
            throw DokumentIkkeFunnetException(
                "Fant ingen journalpost med journalpostId $journalpostId og dokumentreferanse $dokumentReferanse",
            )
        }
        if (journalpost.erUnderProduksjon) {
            throw KanIkkeHenteDokumentUnderProduksjon(journalpostId)
        }
    }

    private fun bestillOgOpprettDokumenttilgang(dokRef: String): Dokumentbestilling {
        val dokumenttilgang = dokumentTilgangConsumer.bestillDokumenttilgang(dokRef)
        return Dokumentbestilling
            .Builder()
            .brevreferanse(dokumenttilgang.brev!!.brevref)
            .systemId(dokumenttilgang.sysid)
            .token(dokumenttilgang.klientToken)
            .build()
    }

    private fun generateMbdokUrl(
        dokumentReferanse: String,
        token: String?,
    ): String = "mbdok://brevklient/system/$systemId/dokument/$dokumentReferanse?token=$token&server=" +
        Optional
            .ofNullable(
                brevserverUrl,
            ).orElse("sjekk fasit!!!")
            .replace(":", "%3A")
            .replace("/", "%2F")
            .replace("?", "%3F")
            .replace("=", "%3D")
            .replace("&", "%26")
            .replace("#", "%23")
}
