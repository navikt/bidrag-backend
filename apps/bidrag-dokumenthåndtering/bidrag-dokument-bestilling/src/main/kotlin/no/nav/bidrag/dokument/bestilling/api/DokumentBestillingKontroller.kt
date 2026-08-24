package no.nav.bidrag.dokument.bestilling.api

import io.micrometer.core.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.dokument.bestilling.SIKKER_LOGG
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingResponse
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentMalDetaljer
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DataGrunnlag
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalBrevserver
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalBucket
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalProduksjon
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentMalType
import no.nav.bidrag.dokument.bestilling.bestilling.dto.DokumentType
import no.nav.bidrag.dokument.bestilling.bestilling.dto.alleDokumentmaler
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.model.dokumentMalEksistererIkke
import no.nav.bidrag.dokument.bestilling.tjenester.DokumentBestillingService
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Timed
class DokumentBestillingKontroller(
    private val dokumentBestillingService: DokumentBestillingService,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(DokumentBestillingKontroller::class.java)
    }

    @PostMapping("/bestill/{dokumentMalKode}")
    @Operation(
        description = "Bestiller dokument for oppgitt brevkode/dokumentKode",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "400",
                description = "Dokument ble bestilt med ugyldig data",
            ),
        ],
    )
    fun bestillBrev(
        @RequestBody bestillingRequest: DokumentBestillingForespørsel,
        @PathVariable dokumentMalKode: String,
    ): DokumentBestillingResponse {
        val dokumentMal =
            hentDokumentMal(dokumentMalKode) ?: dokumentMalEksistererIkke(dokumentMalKode)
        SIKKER_LOGG.info("Bestiller dokument for dokumentmal $dokumentMal med data $${commonObjectmapper.writeValueAsString(bestillingRequest)} og enhet ${bestillingRequest.enhet}")
        val result = dokumentBestillingService.bestill(bestillingRequest, dokumentMal)
        LOGGER.info("Bestilt dokument for brevkode $dokumentMal og enhet ${bestillingRequest.enhet} med respons $result")
        return result
    }

    @PostMapping("/dokument/{dokumentMalKode}")
    @Operation(
        description = "Henter dokument for oppgitt brevkode/dokumentKode",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "400",
                description = "Dokument ble bestilt med ugyldig data",
            ),
        ],
    )
    fun hentDokument(
        @RequestBody(required = false) bestillingRequest: DokumentBestillingForespørsel?,
        @PathVariable dokumentMalKode: String,
    ): ResponseEntity<ByteArray> {
        val dokumentMal =
            hentDokumentMal(dokumentMalKode) ?: dokumentMalEksistererIkke(dokumentMalKode)

        LOGGER.info("Henter dokument for dokumentmal $dokumentMal og enhet ${bestillingRequest?.enhet}")
        SIKKER_LOGG.info("Henter dokument for dokumentmal $dokumentMal med data $bestillingRequest og enhet ${bestillingRequest?.enhet}")
        val result = dokumentBestillingService.hentDokument(bestillingRequest, dokumentMal)
        LOGGER.info("Hentet dokument for dokumentmal $dokumentMal og enhet ${bestillingRequest?.enhet} med respons $result")
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, dokumentMal.tittel)
            .body(result)
    }

    @PostMapping("/produser/{dokumentMalKode}")
    @Operation(
        description = "Henter dokument for oppgitt brevkode/dokumentKode",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "400",
                description = "Dokument ble bestilt med ugyldig data",
            ),
        ],
    )
    fun produserOgHent(
        @RequestBody(required = false) bestillingRequest: DokumentBestillingForespørsel,
        @PathVariable dokumentMalKode: String,
    ): ResponseEntity<ByteArray> {
        val dokumentMal =
            hentDokumentMal(dokumentMalKode) ?: dokumentMalEksistererIkke(dokumentMalKode)

        LOGGER.info("Henter dokument for dokumentmal $dokumentMal og enhet ${bestillingRequest?.enhet}")
        SIKKER_LOGG.info("Henter dokument for dokumentmal $dokumentMal med data $bestillingRequest og enhet ${bestillingRequest?.enhet}")
        val result = dokumentBestillingService.bestillOgHent(bestillingRequest, dokumentMal)
        LOGGER.info("Hentet dokument for dokumentmal $dokumentMal og enhet ${bestillingRequest?.enhet} med respons $result")
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, dokumentMal.tittel)
            .body(result.innhold)
    }

    @RequestMapping("/brevkoder", method = [RequestMethod.OPTIONS])
    @Operation(
        description = "Henter brevkoder som er støttet av applikasjonen",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentStottedeBrevkoder(): List<String> =
        alleDokumentmaler
            .filter { it.enabled && it !is DokumentMalBucket }
            .map { it.kode }
            .let {
                LOGGER.info("Hentet støttede brevkoder $it")
                it
            }

    @GetMapping("/dokumentmal/detaljer")
    @Operation(
        description = "Henter detaljer om alle støttede dokumentmaler",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentDokumentmalDetaljer(): Map<String, DokumentMalDetaljer> =
        alleDokumentmaler
            .associate {
                it.kode to
                    DokumentMalDetaljer(
                        malId = it.kode,
                        beskrivelse = it.beskrivelse,
                        tittel = it.tittel,
                        type =
                            when (it.type) {
                                DokumentMalType.NOTAT -> DokumentType.NOTAT
                                else -> DokumentType.UTGÅENDE
                            },
                        kanBestilles = it.enabled,
                        redigerbar = it.redigerbar,
                        kreverBehandling = it.inneholderDatagrunnlag(DataGrunnlag.BEHANDLING) && listOf("BI01S04", "BI01S18", "BI01S08", "BI01S27").contains(it.kode),
                        kreverVedtak = it.inneholderDatagrunnlag(DataGrunnlag.VEDTAK),
                        språk =
                            if (it is DokumentMalBucket) {
                                listOf(it.språk)
                            } else if (it is DokumentMalBrevserver) {
                                it.støttetSpråk
                            } else {
                                emptyList()
                            },
                        innholdType = it.type,
                        nyDokumentProduksjon = it is DokumentMalProduksjon,
                        statiskInnhold = it is DokumentMalBucket,
                        gruppeVisningsnavn = if (it is DokumentMalBucket) it.gruppeVisningsnavn else null,
                        tilhorerEnheter = if (it is DokumentMalBucket) it.tilhørerEnheter else emptyList(),
                    )
            }
}
