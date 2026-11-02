package no.nav.bidrag.vedtak.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.transport.behandling.vedtak.request.HentManuelleVedtakRequest
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import no.nav.bidrag.transport.behandling.vedtak.response.OpprettVedtakResponseDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.vedtak.exception.custom.ConflictException
import no.nav.bidrag.vedtak.service.VedtakService
import no.nav.bidrag.vedtak.util.VedtakUtil.Companion.tilJson
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Timed
class VedtakController(private val vedtakService: VedtakService) {

    @PostMapping(OPPRETT_VEDTAK, "/vedtak")
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Oppretter nytt vedtak")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak opprettet"),
            ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Unik referanse finnes fra før",
                content = [Content(schema = Schema(implementation = ConflictException::class))],
            ),
            ApiResponse(
                responseCode = "412",
                description = "Angitt sisteVedtaksid er ikke nyeste vedtak",
                content = [
                    Content(
                        schema = Schema(implementation = ConflictException::class),
                    ),
                ],
            ),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun opprettVedtak(
        @Valid @RequestBody
        request: OpprettVedtakRequestDto,
    ): ResponseEntity<OpprettVedtakResponseDto>? {
        val vedtakOpprettet = vedtakService.opprettVedtak(
            vedtakRequest = request,
            vedtaksforslag = false,
        )
        LOGGER.info { "Vedtak er opprettet med følgende id: ${vedtakOpprettet.vedtaksid}" }
        return ResponseEntity(vedtakOpprettet, HttpStatus.OK)
    }

    @GetMapping(HENT_VEDTAK)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter et vedtak")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak funnet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuelt vedtak",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtak ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentVedtak(
        @PathVariable @NotNull
        vedtaksid: Int,
    ): ResponseEntity<VedtakDto> {
        val vedtakFunnet = vedtakService.hentVedtak(vedtaksid)
        secureLogger.debug { "Følgende vedtak ble hentet: $vedtaksid ${tilJson(vedtakFunnet)}".sanitizeForLog() }
        return ResponseEntity(vedtakFunnet, HttpStatus.OK)
    }

    @PostMapping(OPPDATER_VEDTAK)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Oppdaterer grunnlag på et eksisterende vedtak")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak oppdatert"),
            ApiResponse(
                responseCode = "400",
                description = "Data i innsendt vedtak matcher ikke lagrede vedtaksopplysninger",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtak ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun oppdaterVedtak(
        @PathVariable @NotNull
        vedtaksid: Int,
        @Valid @RequestBody
        request: OpprettVedtakRequestDto,
    ): ResponseEntity<Int>? {
        val vedtakOppdatert = try {
            vedtakService.oppdaterVedtak(vedtaksid, request)
        } catch (e: Exception) {
            secureLogger.error { "Følgende request feilet om å oppdatere vedtak med id $vedtaksid: ${tilJson(request)}".sanitizeForLog() }
            throw e
        }
        secureLogger.info { "Vedtak med id $vedtakOppdatert er oppdatert ut i fra id: $vedtaksid, med request: ${tilJson(request)}".sanitizeForLog() }
        return ResponseEntity(vedtakOppdatert, HttpStatus.OK)
    }

    @PostMapping(HENT_VEDTAK_FOR_STØNAD)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter endringsvedtak for angitt sak, skyldner, kravhaver og type")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak hentet"),
            ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentVedtakForStønad(
        @Valid @RequestBody
        request: HentVedtakForStønadRequest,
    ): ResponseEntity<HentVedtakForStønadResponse>? {
        secureLogger.debug { "Følgende request for å hente vedtak for stønad ble mottatt: ${tilJson(request)}".sanitizeForLog() }
        val respons = vedtakService.hentVedtakForStønad(request)
        secureLogger.debug { "Følgende endringsvedtak ble hentet for request: ${tilJson(request)}: ${tilJson(respons)}".sanitizeForLog() }
        return ResponseEntity(respons, HttpStatus.OK)
    }

    @GetMapping(HENT_VEDTAK_FOR_BEHANDLINGSREFERANSE)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter et vedtak for angitt kilde og behandlingsreferanse")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak funnet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuelt vedtak",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtak ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentVedtakForBehandlingsreferanse(
        @PathVariable @NotNull
        kilde: BehandlingsrefKilde,
        @PathVariable @NotNull
        behandlingsreferanse: String,
    ): ResponseEntity<List<Int>> {
        LOGGER.debug { "Request for å hente vedtak for kilde $kilde og behandlingsreferanse ${behandlingsreferanse.sanitizeForLog()} mottatt" }
        val vedtakFunnet = vedtakService.hentVedtakForBehandlingsreferanse(kilde, behandlingsreferanse)
        if (vedtakFunnet.isNotEmpty()) {
            secureLogger.debug { "Følgende vedtak ble hentet: ${tilJson(vedtakFunnet)}".sanitizeForLog() }
        } else {
            secureLogger.debug { "Fant ingen vedtak for kilde $kilde og behandlingsreferanse ${behandlingsreferanse.sanitizeForLog()}" }
        }
        return ResponseEntity(vedtakFunnet, HttpStatus.OK)
    }

    // Endepunkter for Vedtaksforslag
    // Endepunkt for å opprette vedtaksforslag
    @PostMapping(OPPRETT_VEDTAKSFORSLAG)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Oppretter nytt vedtaksforslag")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtaksforslag opprettet"),
            ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "Unik referanse finnes fra før",
                content = [Content(schema = Schema(implementation = ConflictException::class))],
            ),
            ApiResponse(
                responseCode = "412",
                description = "Angitt sisteVedtaksid er ikke nyeste vedtak",
                content = [
                    Content(
                        schema = Schema(implementation = ConflictException::class),
                    ),
                ],
            ),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun opprettVedtaksforslag(
        @Valid @RequestBody
        request: OpprettVedtakRequestDto,
    ): ResponseEntity<Int> {
        val vedtaksforslagOpprettet = vedtakService.opprettVedtak(
            vedtakRequest = request,
            vedtaksforslag = true,
        )
        secureLogger.info { "Vedtaksforslag er opprettet med følgende id: ${vedtaksforslagOpprettet.vedtaksid} ut i fra request: ${tilJson(request)}".sanitizeForLog() }
        return ResponseEntity(vedtaksforslagOpprettet.vedtaksid, HttpStatus.OK)
    }

    @GetMapping(HENT_ALLE_VEDTAKSFORSLAG)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Hent alle vedtaksforslag ider")
    fun hentAlleVedtaksforslag(@RequestParam(required = false, defaultValue = "100") limit: Int) = vedtakService.hentAlleVedtaksforslagIder(limit)

    // Endepunkt for å oppdatere vedtaksforslag
    @PutMapping(VEDTAKSFORSLAG)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Oppdaterer grunnlag på et eksisterende vedtaksforslag")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtaksforslag oppdatert"),
            ApiResponse(
                responseCode = "400",
                description = "Data i innsendt vedtak matcher ikke lagrede vedtaksforslagsopplysninger",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtaksforslag ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun oppdaterVedtaksforslag(
        @PathVariable @NotNull
        vedtaksid: Int,
        @Valid @RequestBody
        request: OpprettVedtakRequestDto,
    ): ResponseEntity<Int>? {
        val vedtaksforslagOppdatert = vedtakService.oppdaterVedtaksforslag(vedtaksid, request)
        secureLogger.info { "Vedtaksforslag med id $vedtaksforslagOppdatert er oppdatert ut i fra request: $vedtaksid: ${tilJson(request)}".sanitizeForLog() }
        return ResponseEntity(vedtaksforslagOppdatert, HttpStatus.OK)
    }

    // Endepunkt for å fatte vedtak fra vedtaksforslag
    @PostMapping(VEDTAKSFORSLAG)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Fatter vedtak fra vedtaksforslag")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak fattet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å fatte vedtak",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtaksforslag ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun fattVedtakFraVedtaksforslag(
        @PathVariable @NotNull
        vedtaksid: Int,
    ): ResponseEntity<Int> {
        vedtakService.fattVedtakForVedtaksforslag(vedtaksid)
        val vedtakFattet = vedtakService.hentVedtak(vedtaksid)
        secureLogger.info { "Følgende vedtak ble fattet fra vedtaksforslag: $vedtaksid ${tilJson(vedtakFattet)}".sanitizeForLog() }
        return ResponseEntity(vedtaksid, HttpStatus.OK)
    }

    // Endepunkt for å fatte slette vedtaksforslag
    @DeleteMapping(VEDTAKSFORSLAG)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Sletter vedtaksforslag")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "vedtaksforslag slettet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuelt vedtaksforslag",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtaksforslag ikke funnet", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun slettVedtaksforslag(
        @PathVariable @NotNull
        vedtaksid: Int,
    ): ResponseEntity<Int> {
        val vedtaksforslagSlettet = vedtakService.slettVedtaksforslag(vedtaksid)
        secureLogger.info { "Følgende vedtaksforslag ble slettet: $vedtaksid ${tilJson(vedtaksforslagSlettet)}".sanitizeForLog() }
        return ResponseEntity(vedtaksforslagSlettet, HttpStatus.OK)
    }

    @PostMapping(HENT_VEDTAK_FOR_UNIK_REFERANSE)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter et vedtak tilknyttet unik referanse")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak funnet"),
            ApiResponse(responseCode = "401", description = "Manglende eller utløpt id-token", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese data for aktuelt vedtak",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "404", description = "Vedtak ikke funnet", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentVedtakForUnikReferanse(
        @RequestBody @NotNull
        unikReferanse: String,
    ): ResponseEntity<VedtakDto> {
        val vedtakFunnet = vedtakService.hentVedtakForUnikReferanse(unikReferanse)
        secureLogger.debug { "Følgende vedtak ble hentet: $unikReferanse $vedtakFunnet".sanitizeForLog() }
        return ResponseEntity(vedtakFunnet, HttpStatus.OK)
    }

    @PostMapping(HENT_MANUELLE_VEDTAK)
    @Operation(security = [SecurityRequirement(name = "bearer-key")], summary = "Henter manuelle vedtak for BP i angitt periode")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtak hentet"),
            ApiResponse(responseCode = "400", description = "Feil opplysinger oppgitt", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(responseCode = "500", description = "Serverfeil", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig", content = [Content(schema = Schema(hidden = true))]),
        ],
    )
    fun hentManuelleVedtak(
        @Valid @RequestBody
        request: HentManuelleVedtakRequest,
    ): ResponseEntity<HentVedtakForStønadResponse>? {
        val respons = vedtakService.hentManuelleVedtak(request)
        secureLogger.debug { "Følgende endringsvedtak ble hentet for bp: ${request.skyldner.verdi}}: ${tilJson(respons)}".sanitizeForLog() }
        return ResponseEntity(respons, HttpStatus.OK)
    }

    companion object {
        const val OPPRETT_VEDTAK = "/vedtak/"
        const val HENT_VEDTAK = "/vedtak/{vedtaksid}"
        const val OPPDATER_VEDTAK = "/vedtak/oppdater/{vedtaksid}"
        const val HENT_VEDTAK_FOR_STØNAD = "/vedtak/hent-vedtak"
        const val HENT_VEDTAK_FOR_BEHANDLINGSREFERANSE = "/vedtak/hent-vedtak-for-behandlingsreferanse/{kilde}/{behandlingsreferanse}"
        const val HENT_VEDTAK_FOR_UNIK_REFERANSE = "/vedtak/unikreferanse"
        const val OPPRETT_VEDTAKSFORSLAG = "/vedtaksforslag"
        const val HENT_ALLE_VEDTAKSFORSLAG = "/vedtaksforslag/alle"
        const val VEDTAKSFORSLAG = "/vedtaksforslag/{vedtaksid}"
        const val HENT_MANUELLE_VEDTAK = "/vedtak/hent-manuelle-vedtak"
        private val LOGGER = KotlinLogging.logger {}
    }
}
