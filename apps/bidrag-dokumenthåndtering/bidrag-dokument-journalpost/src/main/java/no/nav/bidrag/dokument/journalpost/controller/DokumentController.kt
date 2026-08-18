package no.nav.bidrag.dokument.journalpost.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.dokument.journalpost.service.DokumentService
import no.nav.bidrag.transport.dokument.DokumentMetadata
import no.nav.bidrag.transport.dokument.DokumentTilgangResponse
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class DokumentController(
    private val dokumentService: DokumentService,
) {
    @GetMapping(path = ["$ROOT_TILGANG/{journalpostId}/{dokumentreferanse}", "$ROOT_TILGANG/dokumentreferanse/{dokumentreferanse}"])
    fun giTilgangTilDokument(
        @PathVariable(required = false) journalpostId: String?,
        @PathVariable dokumentreferanse: String,
    ): ResponseEntity<DokumentTilgangResponse> {
        val dokumentTilgangResponse = dokumentService.lagTilgangUrl(dokumentreferanse).lagResponseDto()
        LOGGER.info("Opprettet tilgang til dokument: $dokumentTilgangResponse")
        return ResponseEntity(dokumentTilgangResponse, HttpStatus.OK)
    }

    @GetMapping(
        value = ["/dokument/{journalpostId}/{dokumentreferanse}", "/dokument/{journalpostId}", "/dokumentreferanse/{dokumentreferanse}"],
    )
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter dokument for journalpostid og dokumentreferanse. ")
    fun hentDokument(
        @PathVariable(required = false) journalpostId: String?,
        @PathVariable(required = false) dokumentreferanse: String?,
        @RequestParam("rtf", required = false) rtfFile: Boolean = false,
    ): ResponseEntity<ByteArray> {
        LOGGER.info("Henter dokument for journalpost {} og dokumentId {}, rtf {}", journalpostId, dokumentreferanse, rtfFile)
        if (journalpostId.isNullOrEmpty() && dokumentreferanse.isNullOrEmpty()) {
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, "Kan ikke hente dokument uten journalpostId eller dokumentereferanse")
                .build()
        }
        if (journalpostId.isNullOrEmpty()) {
            return if (rtfFile) dokumentService.hentDokumentRTF(dokumentreferanse!!) else dokumentService.hentDokument(dokumentreferanse!!)
        }
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId)
        return if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId")
                .build()
        } else {
            if (rtfFile) {
                dokumentService.hentDokumentRTF(kildesystemIdenfikator.hentJournalpostId(), dokumentreferanse)
            } else {
                dokumentService.hentDokument(kildesystemIdenfikator.hentJournalpostId(), dokumentreferanse)
            }
        }
    }

    @GetMapping("/dokumentreferanse/{dokumentreferanse}/erFerdigstilt")
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Sjekk om dokument er ferdigstilt ")
    fun erFerdigstilt(
        @PathVariable dokumentreferanse: String,
    ): Boolean {
        LOGGER.debug("Sjekker om $dokumentreferanse er ferdigstilt")
        return dokumentService.erFerdigstilt(dokumentreferanse)
    }

    @RequestMapping(
        value = ["/dokument/{journalpostId}/{dokumentreferanse}", "/dokument/{journalpostId}", "/dokumentreferanse/{dokumentreferanse}"],
        method = [RequestMethod.OPTIONS],
    )
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter dokument for journalpostid og dokumentreferanse. ")
    fun hentDokumentMetadata(
        @PathVariable(required = false) journalpostId: String?,
        @PathVariable(required = false) dokumentreferanse: String?,
    ): ResponseEntity<List<DokumentMetadata>> {
        LOGGER.info("Henter dokument for journalpost {} og dokumentId {}", journalpostId, dokumentreferanse)
        if (journalpostId.isNullOrEmpty() && dokumentreferanse.isNullOrEmpty()) {
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, "Kan ikke hente dokument uten journalpostId eller dokumentereferanse")
                .build()
        }
        if (journalpostId.isNullOrEmpty()) {
            return ResponseEntity.ok(listOf(dokumentService.hentDokumentMetadata(dokumentReferanse = dokumentreferanse)))
        }
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId)
        return if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId")
                .build()
        } else {
            ResponseEntity.ok(listOf(dokumentService.hentDokumentMetadata(kildesystemIdenfikator.hentJournalpostId(), dokumentreferanse)))
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DokumentController::class.java)
        const val ROOT_TILGANG = "/tilgang"
    }
}
