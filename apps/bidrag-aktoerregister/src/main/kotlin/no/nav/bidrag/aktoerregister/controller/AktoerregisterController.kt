package no.nav.bidrag.aktoerregister.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.aktoerregister.dto.AktoerDTO
import no.nav.bidrag.aktoerregister.dto.AktoerIdDTO
import no.nav.bidrag.aktoerregister.dto.HendelseDTO
import no.nav.bidrag.aktoerregister.exception.AktørNotFoundException
import no.nav.bidrag.aktoerregister.service.AktørService
import no.nav.bidrag.aktoerregister.service.HendelseService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.samhandler.SamhandlerSøk
import no.nav.bidrag.transport.samhandler.SamhandlersøkeresultatDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

private val LOGGER = KotlinLogging.logger {}

@RestController
@ProtectedWithClaims(issuer = "maskinporten", claimMap = ["scope=nav:bidrag:aktoerregister.read"])
@Tag(
    name = "Aktørregister",
    description = "Endepunkter for oppslag og forvaltning av aktører (personer og samhandlere) og hendelser i aktørregisteret.",
)
class AktoerregisterController(
    private val aktørService: AktørService,
    private val hendelseService: HendelseService,
) {

    @Operation(
        summary = "Hent informasjon om aktør",
        description = "Henter aktørinformasjon basert på identtype og ident. For personer returneres kun kontonummer. For samhandlere/organisasjoner returneres også navn og adresse. Sett `tvingOppdatering=true` for å tvinge et nytt oppslag mot kildesystem.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aktørinformasjon returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AktoerDTO::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på identtype eller ident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Maskinporten-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Ingen aktør med gitt identtype og ident ble funnet.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
        ],
    )
    @PostMapping(path = ["/aktoer"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentAktoer(
        @SwaggerRequestBody(description = "Aktørident og identtype det skal gjøres oppslag på.", required = true)
        @RequestBody request: AktoerIdDTO,
        @Parameter(
            description = "Tving oppdatering av aktøren mot kildesystem uavhengig av cache.",
            required = false,
            example = "false",
        )
        @RequestParam(required = false)
        tvingOppdatering: Boolean = false,
    ): ResponseEntity<AktoerDTO> = try {
        secureLogger.info { "Kall mot /aktoer for å hente ut aktør: Type: ${request.identtype.name} Id: ${request.aktoerId}" }
        val aktoer = aktørService.hentAktoer(request, tvingOppdatering)
        ResponseEntity.ok(aktoer)
    } catch (e: AktørNotFoundException) {
        secureLogger.info { "Aktør ${request.aktoerId} ikke funnet." }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Finner ingen aktør med oppgitt ident", e)
    } catch (e: Exception) {
        secureLogger.error(e) { "Feil ved henting av aktør ${request.aktoerId}. Feilmelding: ${e.message}" }
        throw ResponseStatusException(INTERNAL_SERVER_ERROR, "Intern tjenestefeil. Feil ved henting av aktør. Prøv igjen senere.", e)
    }

    @Operation(
        summary = "Hent aktørhendelser fra sekvensnummer",
        description = "Ingen informasjon om aktøren leveres av denne tjenesten utover aktørIden.\n" +
            "Hendelsene legges inn med stigende sekvensnummer. Klienten må selv ta vare på hvilke sekvensnummer som sist er behandlet, og be om å få hendelser fra det neste sekvensnummeret ved neste kall.\n" +
            "Dersom det ikke returneres noen hendelser er ingen av aktørene endret siden siste kall. Samme sekvensnummer må da benyttes i neste kall.\n\n" +
            "Nye hendelser vil alltid ha høyere sekvensnummer enn tidligere hendelser.\n" +
            "Det kan forekomme hull i sekvensnummer-rekken.\n" +
            "Dersom det kommer en hendelse for en aktør med tidligere hendelser (lavere sekvensnummer) er det ikke garantert at de tidligere hendelsene ikke returneres.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aktørhendelser returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = HendelseDTO::class)))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på fraSekvensnummer eller antall.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Maskinporten-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
        ],
    )
    @GetMapping(path = ["/hendelser"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHendelser(
        @Parameter(
            description = "Sekvensnummer å starte fra (inklusivt). Bruk 0 for å starte fra begynnelsen.",
            required = false,
            example = "0",
        )
        @RequestParam(name = "fraSekvensnummer", defaultValue = "0")
        fraSekvensnummer: Int = 0,
        @Parameter(
            description = "Maks antall hendelser som returneres per kall.",
            required = false,
            example = "1000",
        )
        @RequestParam(name = "antall", defaultValue = "1000")
        antall: Int = 1000,
    ): ResponseEntity<List<HendelseDTO>> = try {
        ResponseEntity.ok(hendelseService.hentHendelser(fraSekvensnummer, antall))
    } catch (e: Exception) {
        LOGGER.error(e) { "Feil ved henting av $antall hendelser fra sekvensnummer $fraSekvensnummer" }
        throw ResponseStatusException(INTERNAL_SERVER_ERROR, "Intern tjenestefeil. Problem ved henting av hendelser. Prøv igjen senere", e)
    }

    @Operation(
        summary = "Avmeld aktør fra aktørregisteret",
        description = "Avmelder aktøren slik at det ikke lenger genereres hendelser for den. Aktøren fjernes fra aktørregisteret.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Aktøren ble avmeldt."),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på identtype eller ident.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Maskinporten-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
        ],
    )
    @PostMapping(path = ["/avmelding"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun avmeldAktør(
        @SwaggerRequestBody(description = "Aktørident og identtype for aktøren som skal avmeldes.", required = true)
        @RequestBody request: AktoerIdDTO,
    ): ResponseEntity<Any> = try {
        aktørService.slettAktoer(request)
        ResponseEntity.ok().build()
    } catch (_: AktørNotFoundException) {
        ResponseEntity.notFound().build()
    }

    @PostMapping("/samhandlersok", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Søk etter samhandlere",
        description = "Søker etter samhandlere basert på navn, organisasjonsnummer eller annen identifikasjon.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Samhandlersøk returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = SamhandlersøkeresultatDto::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på søkekriteriene.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Maskinporten-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
        ],
    )
    fun samhandlerSøk(
        @SwaggerRequestBody(description = "Søkekriterier for samhandlersøk.", required = true)
        @RequestBody samhandlerSøk: SamhandlerSøk,
    ): SamhandlersøkeresultatDto = aktørService.samhandlerSøk(samhandlerSøk)
}
