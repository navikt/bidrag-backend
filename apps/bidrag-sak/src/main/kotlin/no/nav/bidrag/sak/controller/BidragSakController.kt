package no.nav.bidrag.sak.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.dto.FogdhistorikkDto
import no.nav.bidrag.sak.dto.NySakCommandDto
import no.nav.bidrag.sak.dto.NySakResponseDto
import no.nav.bidrag.sak.dto.SakshendelseDto
import no.nav.bidrag.sak.service.BidragSakService
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.FjernMidlertidligTilgangRequest
import no.nav.bidrag.transport.sak.OppdaterRollerISakRequest
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import no.nav.bidrag.transport.sak.OppdaterSakResponse
import no.nav.bidrag.transport.sak.OpprettMidlertidligTilgangRequest
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.OpprettSakResponse
import no.nav.bidrag.transport.sak.SamhandlerSakerDto
import no.nav.bidrag.transport.sak.SamhandlerSakerRequestDto
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class BidragSakController(
    private val bidragSakService: BidragSakService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/bidrag-sak$SAK_SOK/{saksnummer}")
    @Operation(
        description = "Finn metadata for en bidragssak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                "Metadata for sak hentet. Hvis saksbehandler mangler tilgang til å lese " +
                    "metadata for aktuell sak så blir det sendt tom liste med roller i responsen",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Sak ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun findMetadataForSak(
        @PathVariable saksnummer: Saksnummer,
        @RequestParam(name = "vis-rollehistorikk", required = false, defaultValue = "false") visRollehistorikk: Boolean,
    ): ResponseEntity<BidragssakDto> {
        val sakDto = bidragSakService.finnSakMetadata(saksnummer, visRollehistorikk)
        secureLogger.info { "Hentet sak $saksnummer $sakDto" }
        return ResponseEntity(sakDto, if (sakDto == null) HttpStatus.NOT_FOUND else HttpStatus.OK)
    }

    @GetMapping("/bidrag-sak$SAK_SOK/{saksnummer}/fogdhistorikk")
    @Operation(
        description = "Finn fogdhistorikk for en bidragssak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun finnFogdhistorikk(
        @PathVariable saksnummer: Saksnummer,
    ): ResponseEntity<List<FogdhistorikkDto>> {
        val fogdHistorikk = bidragSakService.finnFogdhistorikk(saksnummer)
        secureLogger.debug { "Hentet fogdhistorikk for sak $saksnummer $fogdHistorikk" }
        return ResponseEntity.ok(fogdHistorikk)
    }

    @GetMapping("/bidrag-sak$PERSON_SAK/{personident}")
    @Operation(
        description = "Finn metadata for bidragsaker tilknyttet gitt person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                "Metadata for sak hentet. Hvis saksbehandler mangler tilgang til å lese " +
                    "metadata for sak så blir kun fødslesnummer fra kallet inkludert i saken",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = arrayOf(Content(schema = Schema(hidden = true))),
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen saker funnet for person i datasettet til Bisys",
                content = arrayOf(Content(schema = Schema(hidden = true))),
            ),
        ],
    )
    @Deprecated("Bruk post-endepunkt i stedet.")
    fun find(
        @PathVariable personident: Personident,
    ): ResponseEntity<List<BidragssakDto>> {
        val bidragssaker = bidragSakService.finnSakerFor(personident)
        secureLogger.debug { "Hentet saker $bidragssaker for ${personident.verdi}" }
        return if (bidragssaker.isEmpty()) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(bidragssaker, HttpStatus.OK)
        }
    }

    @PostMapping("/person/sak")
    @Operation(
        description = "Finn metadata for bidragsaker tilknyttet gitt person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun finnForFødselsnummer(
        @RequestBody personident: Personident,
    ): ResponseEntity<List<BidragssakDto>> {
        val bidragssaker = bidragSakService.finnSakerFor(personident)
        secureLogger.debug { "Hentet saker $bidragssaker for ${personident.verdi}" }
        return if (bidragssaker.isEmpty()) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(bidragssaker, HttpStatus.OK)
        }
    }

    @PostMapping("/samhandler/sak")
    @Operation(
        description = "Finn saksliste hvor en samhandler er i bruk",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun finnSamhandlerSaker(
        @RequestBody request: SamhandlerSakerRequestDto,
    ): SamhandlerSakerDto {
        val samhandlerSakDto = bidragSakService.finnSakerForSamhandler(request.samhandlerId)
        secureLogger.debug { "Hentet saker $samhandlerSakDto for samhandler ${request.samhandlerId}" }
        return samhandlerSakDto
    }

    @PostMapping("/sak/oppdater/roller")
    @Operation(
        description = "Oppdater sak roller",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun oppdaterSakRoller(
        @RequestBody request: OppdaterRollerISakRequest,
    ): OppdaterSakResponse {
        secureLogger.debug { "oppdaterSakRoller request: ${commonObjectmapper.writeValueAsString(request)}" }
        return bidragSakService.oppdaterRollerISak(request)
    }

    @PostMapping("/sak/oppdater")
    @Operation(
        description = "Oppdater sak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun oppdaterSak(
        @RequestBody oppdaterSakRequest: OppdaterSakRequest,
    ): OppdaterSakResponse {
        secureLogger.debug { "oppdaterSak request: ${commonObjectmapper.writeValueAsString(oppdaterSakRequest)}" }
        return bidragSakService.oppdaterSak(oppdaterSakRequest)
    }

    @PostMapping("/sak/tilgang/fjern")
    @Operation(
        description = "Fjern midlertidlig tilgang fra sak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun fjernMidlertidligTilgang(
        @RequestBody request: FjernMidlertidligTilgangRequest,
    ) = bidragSakService.fjernMidlertidligTilgangSak(request)

    @PostMapping("/sak/tilgang/opprett")
    @Operation(
        description = "Opprett midlertidlig tilgang til sak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettMidlertidligTilgang(
        @RequestBody request: OpprettMidlertidligTilgangRequest,
    ) = bidragSakService.opprettEllerUtvidMidlertidligTilgangSak(request)

    @PostMapping("/sak")
    @Operation(description = "Opprette ny sak", security = [SecurityRequirement(name = "bearer-key")])
    fun opprettSak(
        @RequestBody opprettSakRequest: OpprettSakRequest,
    ): OpprettSakResponse {
        secureLogger.debug { "opprettSak request ${commonObjectmapper.writeValueAsString(opprettSakRequest)}" }
        return bidragSakService.opprettSak(opprettSakRequest)
    }

    @PostMapping("/bidrag-sak$SAK_NY")
    @Operation(description = "Opprette ny sak", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [

            ApiResponse(
                responseCode = "201",
                description = "Sak opprettet",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
            ),
            ApiResponse(
                responseCode = "403",
                description = "Saksbehandler mangler tilgang til å lese metadata for aktuell sak",
            ),
        ],
    )
    fun post(
        @Parameter(
            name = "X-Enhet",
            required = true,
            description = "Saksbehandlers påloggede enhet",
        )
        @RequestHeader("X-Enhet")
        enhet: String,
        @RequestBody nySakCommandDto: NySakCommandDto,
    ): ResponseEntity<NySakResponseDto> {
        logger.info("Oppretter ny sak. Saksbehandlers påloggede enhet: {}", enhet)
        val nySakResponseDto = bidragSakService.nySak(nySakCommandDto)
        return ResponseEntity(nySakResponseDto, HttpStatus.CREATED)
    }

    @GetMapping("$SAK_SOK/{saksnummer}/hendelser")
    @Operation(
        description = "Finn hendelser for en bidragssak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                "Hendelser for en sak",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Sak ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun finnHendelserForSak(
        @PathVariable saksnummer: Saksnummer,
    ): List<SakshendelseDto> = bidragSakService.finnHendelserForSak(saksnummer)

    @Deprecated("Midlertidig løsning til nye AD-gupper er delt ut til alle sakbehandlere 2026-07-03")
    @GetMapping("$SAK_SOK/{saksnummer}/kanSkrive")
    @Operation(
        description = "Sjekk om en enhet har skrivetilgang til en bidragssak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Skrivetilgang sjekket"),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller utløpt id-token",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun harSkrivetilgang(
        @PathVariable saksnummer: Saksnummer,
        @RequestParam enhet: String,
    ): Boolean = bidragSakService.harSkrivetilgang(saksnummer, enhet)

    companion object {
        const val SAK_SOK = "/sak"
        const val PERSON_SAK = "/person/sak"
        const val SAK_NY = "/sak/ny"
    }
}
