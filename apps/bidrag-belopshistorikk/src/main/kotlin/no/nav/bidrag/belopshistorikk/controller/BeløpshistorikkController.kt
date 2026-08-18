package no.nav.bidrag.belopshistorikk.controller

import io.micrometer.core.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.belopshistorikk.service.BeløpshistorikkService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragssakerRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.SkyldnerStønaderRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragssakerResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.SkyldnerStønaderResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadMedPeriodeBeløpResponse
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Timed
class BeløpshistorikkController(private val beløpshistorikkService: BeløpshistorikkService) {

    @PostMapping(HENT_STØNAD)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Finn alle data for en stønad")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Stønad funnet"),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuell stønad",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Stønad ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentStønad(
        @NotNull
        @RequestBody
        request: HentStønadRequest,
    ): ResponseEntity<StønadDto> {
        val stønadFunnet = beløpshistorikkService.hentStønad(request)
        LOGGER.info("Følgende stønadsid ble hentet: ${stønadFunnet?.stønadsid}")
        secureLogger.debug { "Følgende stønad ble hentet: $stønadFunnet" }
        return ResponseEntity(stønadFunnet, HttpStatus.OK)
    }

    @PostMapping(HENT_STØNAD_HISTORISK)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Finn alle data for en stønad for angitt tidspunkt")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Stønad funnet"),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuell stønad",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Stønad ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentStønadHistorisk(
        @NotNull @RequestBody
        request: HentStønadHistoriskRequest,
    ): ResponseEntity<StønadDto> {
        val stønadFunnet = beløpshistorikkService.hentStønadHistorisk(request)
        LOGGER.info("Følgende historiske stønadsid ble hentet: ${stønadFunnet?.stønadsid}")
        secureLogger.debug { "Følgende historiske stønad ble hentet: $stønadFunnet" }
        return ResponseEntity(stønadFunnet, HttpStatus.OK)
    }

    @GetMapping(HENT_STØNADER_FOR_SAK)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Finner alle stønader innenfor angitt sak")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sak funnet"),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuell stønad",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Stønader ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentStønaderForSak(
        @PathVariable @NotNull
        sak: String,
    ): ResponseEntity<List<StønadDto>> {
        val stønaderFunnet = beløpshistorikkService.hentStønaderForSak(sak)
        LOGGER.info("Stønader ble hentet for sak: $sak")
        secureLogger.debug { "Følgende stønader ble hentet for sak $sak: $stønaderFunnet" }
        return ResponseEntity(stønaderFunnet, HttpStatus.OK)
    }

    @PostMapping(HENT_LØPENDE_BIDRAGSSAKER_FOR_SKYLDNER)
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Finn alle løpende bidragssaker der angitt personident er skyldner." +
            "Gjelder barnebidrag, oppfostringssbidrag og 18-årsbidrag",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Stønader funnet"),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuelle stønader",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Stønad ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentLøpendeBidragssakerForSkyldner(
        @NotNull @RequestBody
        request: LøpendeBidragssakerRequest,
    ): ResponseEntity<LøpendeBidragssakerResponse> {
        val respons = beløpshistorikkService.finnLøpendeBidragssaker(request)
        LOGGER.info("Følgende saker ble funnet: ${respons.bidragssakerListe.map { it.sak.toString() }}")
        secureLogger.debug { "Følgende saker ble funnet for skyldner ${request.skyldner}: ${respons.bidragssakerListe}" }
        return ResponseEntity(respons, HttpStatus.OK)
    }

    @PostMapping(HENT_ALLE_STØNADER_FOR_SKYLDNER)
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Finn alle stønader der angitt personident er skyldner.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Stønader funnet"),
        ],
    )
    fun hentAlleStønaderForSkyldner(
        @NotNull @RequestBody
        request: SkyldnerStønaderRequest,
    ): ResponseEntity<SkyldnerStønaderResponse> {
        val respons = beløpshistorikkService.finnAlleStønaderForSkyldner(request)
        LOGGER.info("Følgende stønader ble funnet: ${respons.stønader.map { it.sak.toString() }}")
        secureLogger.debug { "Følgende stønader ble funnet for skyldner ${request.skyldner}: ${respons.stønader}" }
        return ResponseEntity(respons, HttpStatus.OK)
    }

    @PostMapping(HENT_STØNAD_PERIODEBELØP)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Finn alle data for en stønad med historikk for perioder")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Stønad funnet"),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuell stønad",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Stønad ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentStønadMedPeriodebeløp(
        @NotNull @RequestBody
        request: HentStønadRequest,
    ): ResponseEntity<StønadMedPeriodeBeløpResponse> {
        val stønadFunnet = beløpshistorikkService.hentStønadMedPeriodebeløp(request)
        LOGGER.info("Stønad med periodebeløp ble hentet")
        secureLogger.debug { "Følgende stønad med periodebeløp ble funnet: $stønadFunnet" }
        return ResponseEntity(stønadFunnet, HttpStatus.OK)
    }

    @PostMapping(HENT_LØPENDE_STØNADER_I_PERIODE)
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Finn alle løpende stønader i mottatt periode tilknyttet skyldner angitt i request",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Stønader funnet"),
        ],
    )
    fun hentAlleLøpendeStønaderIPeriode(
        @NotNull @RequestBody
        request: LøpendeBidragPeriodeRequest,
    ): ResponseEntity<LøpendeBidragPeriodeResponse> {
        val respons = beløpshistorikkService.finnLøpendeBidragIPeriodeForSkyldner(request)
        LOGGER.info("Følgende stønader ble funnet: ${respons.bidragListe.map { it.sak.toString() }}")
        secureLogger.debug {
            "Følgende stønader ble funnet for skyldner ${request.skyldner}: ${respons.bidragListe.joinToString { it.periodeListe.toString() }}"
        }
        return ResponseEntity(respons, HttpStatus.OK)
    }

    @GetMapping("engangsbelop/{sak}")
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Finner alle engangsbeløp innenfor angitt sak")
    fun hentEngangsbelopForSak(
        @PathVariable @NotNull
        sak: Saksnummer,
    ): ResponseEntity<List<EngangsbeløpDto>> {
        val engangsbeløpFunnet = beløpshistorikkService.finnEngangsbeløpforSak(sak = sak)
        LOGGER.debug("Engangsbeløp ble hentet for sak: $sak")
        return ResponseEntity.ok(engangsbeløpFunnet)
    }

    companion object {
        const val HENT_STØNAD = "/hent-stonad/"
        const val HENT_STØNAD_HISTORISK = "/hent-stonad-historisk/"
        const val HENT_STØNADER_FOR_SAK = "/hent-stonader-for-sak/{sak}"
        const val HENT_LØPENDE_BIDRAGSSAKER_FOR_SKYLDNER = "/hent-lopende-bidragssaker-for-skyldner"
        const val HENT_ALLE_STØNADER_FOR_SKYLDNER = "/hent-alle-stonader-for-skyldner"
        const val HENT_STØNAD_PERIODEBELØP = "/hent-stonad-periodebeløp/"
        const val HENT_LØPENDE_STØNADER_I_PERIODE = "/hent-stonader-i-periode/"
        private val LOGGER = LoggerFactory.getLogger(BeløpshistorikkController::class.java)
    }
}
