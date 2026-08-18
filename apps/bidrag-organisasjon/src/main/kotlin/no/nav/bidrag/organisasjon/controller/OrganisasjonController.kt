package no.nav.bidrag.organisasjon.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import no.nav.bidrag.organisasjon.service.OrganisasjonService
import no.nav.bidrag.transport.organisasjon.BidragEnheterResponsDto
import no.nav.bidrag.transport.organisasjon.EnhetBrukerDto
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.EnhetKontaktinfoDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import no.nav.bidrag.transport.organisasjon.JournalførendeEnhetDto
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
class OrganisasjonController(private val organisasjonService: OrganisasjonService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/arbeidsfordeling/enhet/geografisktilknytning")
    @Operation(
        description = "Hent enheter fra arbeidsfordeling basert på geografisk tilknytning for en person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentArbeidsfordelingGeografiskTilknytningEnhet(@RequestBody hentEnhetRequest: HentEnhetRequest): EnhetDto? {
        logger.info("request: bidrag-organisasjon/arbeidsfordeling/enhet/geografisktilknytning")
        return organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnhet(hentEnhetRequest)
    }

    @GetMapping(path = ["$ENDPOINT_ARBEIDSFORDELING_GT/{ident}"])
    @Operation(
        description = "Hent enheter fra arbeidsfordeling basert på geografisk tilknytning for en person",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Geografisk enhet funnet"),
            ApiResponse(responseCode = "204", description = "Geografisk Enhet ikke funnet"),
        ],
    )
    @Deprecated("Bruk post mot /arbeidsfordeling/enhet/geografisktilknytning")
    fun hentArbeidsfordelingGeografiskTilknytningEnheter(
        @PathVariable ident: String?,
        @RequestParam(required = false) tema: String?,
        @RequestParam(required = false) behandlingstema: String?,
        @RequestParam(required = false) arbeidsfordeling: Arbeidsfordeling?,
    ): ResponseEntity<EnhetDto> {
        logger.info("request: bidrag-organisasjon$ENDPOINT_ARBEIDSFORDELING_GT/ident")
        val response =
            organisasjonService.hentArbeidsfordelingGeografiskTilknytningEnheter(
                Personident(ident!!),
                tema,
                behandlingstema ?: arbeidsfordeling?.behandlingstema,
            )
        return if (response == null) ResponseEntity.noContent().build() else ResponseEntity.ok(response)
    }

    @GetMapping(path = ["$ENDPOINT_ENHET_INFO/{enhetNr}"])
    @Operation(description = "Hent informasjon om enhet", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Enhet funnet"), ApiResponse(
                responseCode = "204",
                description = "Enhet ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "500",
                description = "Ukjent feil",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "503",
                description = "Tjeneste utilgjengelig",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun hentEnhetInfo(@PathVariable enhetNr: Enhetsnummer): ResponseEntity<EnhetDto> {
        LOGGER.info("request: bidrag-organisasjon{}/{}", ENDPOINT_ENHET_INFO, enhetNr)
        val enhetInfo = organisasjonService.hentEnhetInfo(enhetNr)
        return ResponseEntity(enhetInfo, HttpStatus.OK)
    }

    @GetMapping(path = ["$ENDPOINT_ENHET_KONTAKTINFO/{enhetNr}", "$ENDPOINT_ENHET_KONTAKTINFO/{enhetNr}/{spraak}"])
    @Operation(description = "Hent kontaktinformasjon til enhet", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Enhet kontaktinformasjon funnet"), ApiResponse(
                responseCode = "204",
                description = "Enhet kontaktinformasjon ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "500",
                description = "Ukjent feil",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "503",
                description = "Tjeneste utilgjengelig",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun hentEnhetKontaktinfo(
        @PathVariable enhetNr: Enhetsnummer,
        @PathVariable(required = false) spraak: String?,
    ): ResponseEntity<EnhetKontaktinfoDto> = ResponseEntity.ok(organisasjonService.hentEnhetKontaktinformasjon(enhetNr, spraak))

    @GetMapping(path = ["$ENDPOINT_SAKSBEHANDLERINFO/{saksbehandlerIdent}"])
    @Operation(description = "Hent informasjon om saksbehandler", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Saksbehandler funnet"), ApiResponse(
                responseCode = "204",
                description = "Saksbehandler ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "500",
                description = "Ukjent feil",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "503",
                description = "Tjeneste utilgjengelig",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun hentSaksbehandlerInfo(@PathVariable saksbehandlerIdent: String): ResponseEntity<SaksbehandlerDto> {
        LOGGER.info("request: bidrag-organisasjon{}/{}", ENDPOINT_SAKSBEHANDLERINFO, saksbehandlerIdent)
        val saksbehandlerDto = organisasjonService.hentSaksbehandlerInfo(saksbehandlerIdent)
        return ResponseEntity(saksbehandlerDto, if (saksbehandlerDto == null) HttpStatus.NO_CONTENT else HttpStatus.OK)
    }

    @GetMapping("$ENDPOINT_SAKSBEHANDLERENHETER/{saksbehandlerIdent}")
    @Operation(description = "Hent enheter for saksbehandler", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Enheter funnet"), ApiResponse(
                responseCode = "204",
                description = "Enheter ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "404",
                description = "Saksbehandler ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "500",
                description = "Ukjent feil",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "503",
                description = "Tjeneste utilgjengelig",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun hentSaksbehandlerEnheter(@PathVariable saksbehandlerIdent: String): ResponseEntity<List<EnhetDto>> {
        LOGGER.info("request: bidrag-organisasjon{}/{}", ENDPOINT_SAKSBEHANDLERENHETER, saksbehandlerIdent)
        val enhetDtoer = organisasjonService.hentSaksbehandlerEnheter(saksbehandlerIdent)
        return ResponseEntity(enhetDtoer, if (enhetDtoer.isEmpty()) HttpStatus.NO_CONTENT else HttpStatus.OK)
    }

    @GetMapping(path = [ENDPOINT_ARBEIDSFORDELING_JF])
    @Operation(description = "Hent journalførende enheter fra arbeidsfordeling", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Enheter funnet"), ApiResponse(
                responseCode = "204",
                description = "Enheter ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "500",
                description = "Ukjent feil",
                content = [Content(schema = Schema(hidden = true))],
            ), ApiResponse(
                responseCode = "503",
                description = "Tjeneste utilgjengelig",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun hentArbeidsfordelingJournalforendeEnheter(): ResponseEntity<List<JournalførendeEnhetDto>> {
        LOGGER.info("request: bidrag-organisasjon{}", ENDPOINT_ARBEIDSFORDELING_JF)
        val enhetDtoResponse = organisasjonService.hentArbeidsfordelingJournalforendeEnheter()
        return ResponseEntity(enhetDtoResponse.responseEntity.body, enhetDtoResponse.responseEntity.statusCode)
    }

    @GetMapping(path = ["$ENDPOINT_ENHET_BRUKERE/{enhetsnummer}"])
    @Operation(description = "Hent brukere for bidrag-enhet", security = [SecurityRequirement(name = "bearer-key")])
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Brukere funnet"),
            ApiResponse(
                responseCode = "204",
                description = "Brukere ikke funnet",
                content = [Content(schema = Schema(hidden = true))],
            ),
        ],
    )
    fun hentBidragEnhetBrukere(@PathVariable enhetsnummer: String): ResponseEntity<List<EnhetBrukerDto>> {
        LOGGER.info("request: bidrag-organisasjon{}/{}", ENDPOINT_ENHET_BRUKERE, enhetsnummer)
        val brukere = organisasjonService.hentPersonerEnhet(enhetsnummer)
        return ResponseEntity(brukere, if (brukere.isEmpty()) HttpStatus.NO_CONTENT else HttpStatus.OK)
    }

    @GetMapping(path = ["/enheter/grupper"])
    @Operation(description = "Hent grupper av enheter fra konfigurasjon", security = [SecurityRequirement(name = "bearer-key")])
    fun hentEnheterGrupper(): ResponseEntity<BidragEnheterResponsDto> {
        val grupper = organisasjonService.hentAlleEnheterGrupper()
        return ResponseEntity.ok(grupper)
    }

    companion object {
        const val ENDPOINT_ENHET_INFO = "/enhet/info"
        const val ENDPOINT_ENHET_KONTAKTINFO = "/enhet/kontaktinfo"
        const val ENDPOINT_SAKSBEHANDLERINFO = "/saksbehandler/info"
        const val ENDPOINT_SAKSBEHANDLERENHETER = "/saksbehandler/enhetsliste"
        const val ENDPOINT_ENHET_BRUKERE = "/brukere/enhet"
        const val ENDPOINT_ARBEIDSFORDELING_JF = "/arbeidsfordeling/enhetsliste/journalforende"
        const val ENDPOINT_ARBEIDSFORDELING_GT = "/arbeidsfordeling/enhetsliste/geografisktilknytning"
        private val LOGGER = LoggerFactory.getLogger(OrganisasjonController::class.java)
    }
}
