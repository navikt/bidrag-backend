package no.nav.bidrag.grunnlag.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.service.GrunnlagspakkeService
import no.nav.bidrag.grunnlag.service.HentGrunnlagService
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.OppdaterGrunnlagspakkeRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.OpprettGrunnlagspakkeRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagspakkeDto
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagspakkeDto
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class GrunnlagController(
    private val grunnlagspakkeService: GrunnlagspakkeService,
    private val hentGrunnlagService: HentGrunnlagService,
    private val hentValutakursService: HentValutakursService,
) {

    @PostMapping(GRUNNLAGSPAKKE_NY)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Oppretter grunnlagspakke")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlagspakke opprettet"),
            ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt"),
            ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
            ApiResponse(responseCode = "500", description = "Serverfeil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun opprettNyGrunnlagspakke(
        @Valid @RequestBody
        request: OpprettGrunnlagspakkeRequestDto,
    ): Int? {
        val grunnlagspakkeOpprettet = grunnlagspakkeService.opprettGrunnlagspakke(request)
        LOGGER.info("Grunnlagspakke er opprettet med id: $grunnlagspakkeOpprettet")
        return grunnlagspakkeOpprettet
    }

    @PostMapping(GRUNNLAGSPAKKE_OPPDATER)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Trigger innhenting av grunnlag for grunnlagspakke")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlagspakke oppdatert"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token"),
            ApiResponse(responseCode = "403", description = "Saksbehandler mangler tilgang til å lese data for aktuell grunnlagspakke"),
            ApiResponse(responseCode = "404", description = "Grunnlagspakke ikke funnet"),
            ApiResponse(responseCode = "500", description = "Serverfeil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun oppdaterGrunnlagspakke(
        @PathVariable @NotNull
        grunnlagspakkeId: Int,
        @Valid @RequestBody
        request: OppdaterGrunnlagspakkeRequestDto,
    ): OppdaterGrunnlagspakkeDto? {
        SECURE_LOGGER.info("Oppdaterer grunnlagspakkeId: $grunnlagspakkeId med request: ${tilJson(request)}")
        val grunnlagspakkeOppdatert = grunnlagspakkeService.oppdaterGrunnlagspakke(grunnlagspakkeId, request)
        LOGGER.info("Følgende grunnlagspakke ble oppdatert: $grunnlagspakkeId")
        return grunnlagspakkeOppdatert
    }

    @GetMapping(GRUNNLAGSPAKKE_HENT)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Finn alle data for en grunnlagspakke")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlagspakke funnet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token"),
            ApiResponse(responseCode = "403", description = "Saksbehandler mangler tilgang til å lese data for aktuell grunnlagspakke"),
            ApiResponse(responseCode = "404", description = "Grunnlagspakke ikke funnet"),
            ApiResponse(responseCode = "500", description = "Serverfeil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun hentGrunnlagspakke(
        @PathVariable @NotNull
        grunnlagspakkeId: Int,
    ): HentGrunnlagspakkeDto? {
        val grunnlagspakkeFunnet = grunnlagspakkeService.hentGrunnlagspakke(grunnlagspakkeId)
        LOGGER.info("Følgende grunnlagspakke ble hentet: ${grunnlagspakkeFunnet.grunnlagspakkeId}")
        SECURE_LOGGER.info("Hent av grunnlagspakke med id: $grunnlagspakkeId ga følgende response: ${tilJson(grunnlagspakkeFunnet)}")

        return grunnlagspakkeFunnet
    }

    @PostMapping(GRUNNLAGSPAKKE_LUKK)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Setter gyldigTil-dato = dagens dato for angitt grunnlagspakke")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlagspakke oppdatert"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token"),
            ApiResponse(responseCode = "403", description = "Saksbehandler mangler tilgang til å lese data for aktuell grunnlagspakke"),
            ApiResponse(responseCode = "404", description = "Grunnlagspakke ikke funnet"),
            ApiResponse(responseCode = "500", description = "Serverfeil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    fun lukkGrunnlagspakke(
        @PathVariable @NotNull
        grunnlagspakkeId: Int,
    ): Int? {
        val oppdatertgrunnlagspakke = grunnlagspakkeService.lukkGrunnlagspakke(grunnlagspakkeId)
        LOGGER.info("Følgende grunnlagspakke ble oppdatert med gyldigTil-dato: $oppdatertgrunnlagspakke")
        return grunnlagspakkeId
    }

    @PostMapping(HENT_GRUNNLAG)
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Trigger asynkron innhenting av grunnlag for personer angitt i requesten",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlag innhentet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token"),
            ApiResponse(responseCode = "403", description = "Saksbehandler mangler tilgang til å lese data"),
            ApiResponse(responseCode = "404", description = "Grunnlag ikke funnet"),
            ApiResponse(responseCode = "500", description = "Serverfeil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    suspend fun hentGrunnlag(
        @Valid @RequestBody
        request: HentGrunnlagRequestDto,
    ): HentGrunnlagDto? {
        SECURE_LOGGER.info("Henter grunnlag med request: ${tilJson(request)}")
        val hentGrunnlagDto = hentGrunnlagService.hentGrunnlag(request)
        SECURE_LOGGER.info("Hent av grunnlag ga følgende respons: ${tilJson(hentGrunnlagDto)}")
        return hentGrunnlagDto
    }

    @PostMapping(HENT_VALUTAKURS)
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Trigger innhenting av valutakurs fra Norges Bank",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Grunnlag innhentet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token"),
            ApiResponse(responseCode = "403", description = "Saksbehandler mangler tilgang til å lese data"),
            ApiResponse(responseCode = "404", description = "Grunnlag ikke funnet"),
            ApiResponse(responseCode = "500", description = "Serverfeil"),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig"),
        ],
    )
    suspend fun hentValutakurs(
        @Valid @RequestBody
        request: HentValutakursRequest,
    ): HentValutakursResponse? {
        SECURE_LOGGER.info("Henter grunnlag med request: ${tilJson(request)}")
        val hentGrunnlagDto = hentValutakursService.hentValutakurs(request)
        SECURE_LOGGER.info("Hent av grunnlag ga følgende respons: ${tilJson(hentGrunnlagDto)}")
        return hentGrunnlagDto
    }

    companion object {

        const val GRUNNLAGSPAKKE_NY = "/grunnlagspakke"
        const val GRUNNLAGSPAKKE_OPPDATER = "/grunnlagspakke/{grunnlagspakkeId}/oppdater"
        const val GRUNNLAGSPAKKE_HENT = "/grunnlagspakke/{grunnlagspakkeId}"
        const val GRUNNLAGSPAKKE_LUKK = "/grunnlagspakke/{grunnlagspakkeId}/lukk"
        const val HENT_GRUNNLAG = "/hentgrunnlag"
        const val HENT_VALUTAKURS = "/hentvalutakurs"
        private val LOGGER = LoggerFactory.getLogger(GrunnlagController::class.java)
    }
}
