package no.nav.bidrag.dokument.journalpost.controller

import no.nav.bidrag.dokument.journalpost.job.OppdaterStatusPåDokumenterService
import no.nav.bidrag.dokument.journalpost.job.OppdaterStatusPåDokumenterUnderProduksjonRequestDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class AdminController(
    private val oppdaterStatusService: `OppdaterStatusPåDokumenterService`,
) {
    @PostMapping("/api/admin/oppdaterStatus")
    fun oppdaterStatusPåDokumentUnderProduksjon(
        @RequestBody request: OppdaterStatusPåDokumenterUnderProduksjonRequestDto,
    ) = oppdaterStatusService.oppdaterStatusPåJournalposterSomHarStatusUnderProduksjon(request)

    @GetMapping("/api/admin/oppdaterStatus")
    fun hentSjekketStatusPåJournalposterSomHarStatusUnderProduksjon(
        @RequestBody request: OppdaterStatusPåDokumenterUnderProduksjonRequestDto,
    ) = oppdaterStatusService.hentSjekketStatusPåJournalposterSomHarStatusUnderProduksjon(request)
}
