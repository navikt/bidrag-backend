package no.nav.bidrag.tilgangskontroll.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.tilgangskontroll.konfigurasjon.Cache
import no.nav.bidrag.tilgangskontroll.konsumer.MicrosoftGraphConsumer
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerEnheterRespons
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerGrupperResponse
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerinformasjonResponse
import no.nav.bidrag.tilgangskontroll.model.graph.Søknadsgruppe
import no.nav.bidrag.tilgangskontroll.model.tilgangsmaskin.TilgangsmaskinBulkResponse
import no.nav.bidrag.tilgangskontroll.tjeneste.CacheService
import no.nav.bidrag.tilgangskontroll.tjeneste.TilgangskontrollService
import no.nav.bidrag.transport.tilgang.Brukertilganger
import no.nav.bidrag.transport.tilgang.TilgangTilPersonRequest
import no.nav.bidrag.transport.tilgang.TilgangTilSakRequest
import no.nav.bidrag.transport.tilgang.TilgangTilTemaRequest
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@RequestMapping("/v2/api/tilgang")
@Tag(
    name = "Tilgangskontroll",
    description = "Endepunkter for å kontrollere saksbehandlers tilgang til personer, saker og fagtemaer i Bidrag",
)
class TilgangControllerV2(
    private val tilgangskontrollService: TilgangskontrollService,
    private val microsoftGraphConsumer: MicrosoftGraphConsumer,
    private val cacheService: CacheService,
) {
    @PostMapping("/sak")
    @Operation(
        summary = "Sjekk tilgang til sak",
        description =
        "Kontrollerer om innlogget saksbehandler har tilgang til en gitt bidragssak. " +
            "Sjekker blant annet enhetstilgang, skjermingsstatus og adressebeskyttelse på involverte parter.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat. " +
                    "Ved avslag inneholder 'detaljer' begrunnelse og opprinnelse.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkTilgangSakV2(
        @RequestBody tilgangTilSakRequest: TilgangTilSakRequest,
    ): ResponseEntity<TilgangskontrollResponse> = ResponseEntity.ok(tilgangskontrollService.sjekkTilgangSak(tilgangTilSakRequest.saksnummer.verdi))

    @PostMapping("/person")
    @Operation(
        summary = "Sjekk tilgang til person",
        description =
        "Kontrollerer om innlogget saksbehandler har tilgang til å se opplysninger om en person. " +
            "Sjekker skjermingsstatus (egen ansatt), adressebeskyttelse (kode 6/7) og diskresjonskoder.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat. " +
                    "Ved avslag inneholder 'detaljer' begrunnelse og opprinnelse.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkTilgangPerson(
        @RequestBody tilgangTilPersonRequest: TilgangTilPersonRequest,
    ): ResponseEntity<TilgangskontrollResponse> = ResponseEntity.ok(tilgangskontrollService.sjekkTilgangPerson(tilgangTilPersonRequest.personident))

    @PostMapping("/tema")
    @Operation(
        summary = "Sjekk tilgang til fagtema",
        description =
        "Kontrollerer om saksbehandler har tilgang til et spesifikt fagtema (f.eks. BID, FAR). " +
            "Validerer at brukerens AD-grupper gir rettigheter til det angitte temaet.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description =
                "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat. " +
                    "Ved avslag inneholder 'detaljer' begrunnelse og opprinnelse.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkTilgangTema(
        @RequestBody tilgangTilTemaRequest: TilgangTilTemaRequest,
    ): ResponseEntity<TilgangskontrollResponse> {
        // Hvis frontend utfører kallet som application/json så vil tema bli sendt med fnutter
        val temaCleaned = tilgangTilTemaRequest.tema.replace("\"", "")
        return ResponseEntity.ok(tilgangskontrollService.sjekkTilgangTema(temaCleaned, tilgangTilTemaRequest.navIdent))
    }

    @PostMapping("/opprettsakutenbm")
    @Operation(
        summary = "Sjekk tilgang til å opprette sak uten bidragsmottaker",
        description =
        "Kontrollerer om innlogget saksbehandler har tilgang til å opprette en bidragssak " +
            "uten bidragsmottaker (BM), altså kun med bidragspliktig (BP) og barn. " +
            "Krever spesifikke AD-gruppemedlemskap.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkTilgangOpprettSakUtenBm(): ResponseEntity<TilgangskontrollResponse> = ResponseEntity.ok(tilgangskontrollService.sjekkTilgangOpprettSakUtenBm())

    @GetMapping("/brukergrupper")
    @Operation(
        summary = "Hent brukerens AD-grupper",
        description =
        "Henter alle Azure AD-grupper som en saksbehandler er medlem av via Microsoft Graph API. " +
            "Brukes for å kartlegge brukerens tilganger og roller.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste over AD-grupper brukeren er medlem av",
                content = [Content(schema = Schema(implementation = BrukerGrupperResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Feil ved oppslag mot Microsoft Graph", content = [Content()]),
        ],
    )
    fun hentBrukergrupper(
        @Parameter(description = "NAV-ident til saksbehandleren", example = "Z999999")
        @RequestParam navident: String,
    ): ResponseEntity<BrukerGrupperResponse>? = ResponseEntity.ok(microsoftGraphConsumer.hentGrupperForBruker(navident))

    @GetMapping("/enhet")
    @Operation(
        summary = "Hent brukere tilknyttet enhet",
        description = "Henter alle saksbehandlere som er tilknyttet en gitt NAV-enhet via Microsoft Graph API.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste over brukere tilknyttet enheten",
                content = [Content(schema = Schema(implementation = BrukerinformasjonResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Feil ved oppslag mot Microsoft Graph", content = [Content()]),
        ],
    )
    fun hentBrukereForEnhet(
        @Parameter(description = "NAV-enhetsnummer", example = "4806")
        @RequestParam enhet: String,
    ): ResponseEntity<BrukerinformasjonResponse>? = ResponseEntity.ok(microsoftGraphConsumer.hentBrukereForEnhet(enhet))

    @GetMapping("/enheter")
    @Operation(
        summary = "Hent enheter for bruker",
        description =
        "Henter alle NAV-enheter som en saksbehandler tilhører. " +
            "Hvis navident ikke oppgis, brukes identiteten fra innlogget token.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste over enhet-IDer brukeren tilhører",
                content = [Content(schema = Schema(implementation = BrukerEnheterRespons::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "NAV-ident kunne ikke utledes fra token eller parameter",
                content = [Content()],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Feil ved oppslag mot Microsoft Graph", content = [Content()]),
        ],
    )
    fun hentEnheterForBruker(
        @Parameter(description = "NAV-ident til saksbehandleren. Valgfri — utledes fra token om ikke oppgitt.", example = "Z999999")
        @RequestParam(required = false) navident: String?,
    ): ResponseEntity<BrukerEnheterRespons> {
        val ident = navident ?: TokenUtils.hentSaksbehandlerIdent()
        if (ident == null) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(microsoftGraphConsumer.hentEnheterForBruker(ident))
    }

    @GetMapping("/brukerinformasjon")
    @Operation(
        summary = "Hent brukerinformasjon",
        description =
        "Henter detaljert brukerinformasjon (navn, e-post, avdeling, jobbtittel) " +
            "for en saksbehandler fra Microsoft Graph API.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Brukerinformasjon for oppgitt NAV-ident",
                content = [Content(schema = Schema(implementation = BrukerinformasjonResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Feil ved oppslag mot Microsoft Graph", content = [Content()]),
        ],
    )
    fun hentBrukerinformasjon(
        @Parameter(description = "NAV-ident til saksbehandleren", example = "Z999999")
        @RequestParam navident: String,
    ): ResponseEntity<BrukerinformasjonResponse> = ResponseEntity.ok(microsoftGraphConsumer.hentBrukerinformasjon(navident))

    @GetMapping("/cache/clear")
    @Operation(
        summary = "Tøm cache",
        description = "Tømmer spesifisert cache-type. Brukes ved feilsøking eller når tilgangsdata må oppdateres umiddelbart.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Cache tømt", content = [Content()]),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil", content = [Content()]),
        ],
    )
    fun tømCache(
        @Parameter(description = "Type cache som skal tømmes")
        @RequestParam cacheType: Cache.CacheType,
    ): ResponseEntity<Any> {
        cacheService.tømCache(cacheType.name)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/soknadsgrupper")
    @Operation(
        summary = "Sjekk tilgang til søknadsgruppe",
        description =
        "Kontrollerer om saksbehandler har tilgang til en spesifikk søknadsgruppe " +
            "(barnebortføring, ektefellebidrag, oppfostringsbidrag eller reiseutgifter). " +
            "Tilgang avgjøres av hvilken enhet saksbehandleren tilhører.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkTilgangSoknadsgrupper(
        @Parameter(description = "Søknadsgruppe det skal sjekkes tilgang for")
        @RequestParam søknadsgruppe: Søknadsgruppe,
        @Parameter(description = "NAV-ident. Valgfri — utledes fra token om ikke oppgitt.", example = "Z999999")
        @RequestParam(required = false) navident: String? = null,
    ): ResponseEntity<TilgangskontrollResponse> = ResponseEntity.ok(tilgangskontrollService.sjekkTilgangSøknadsgruppe(søknadsgruppe, navident))

    @PostMapping("/brukertilganger")
    @Operation(
        summary = "Hent brukertilganger",
        description =
        "Henter en samlet oversikt over alle tilganger for innlogget saksbehandler, " +
            "inkludert Bisys-tilgang, utlandstilgang, lese-/behandletilgang, foreldreskapstilgang " +
            "og tilgjengelige behandlingstemaer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Samlet oversikt over brukerens tilganger",
                content = [Content(schema = Schema(implementation = Brukertilganger::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved henting av tilganger", content = [Content()]),
        ],
    )
    fun hentBrukertilganger(): Brukertilganger = tilgangskontrollService.hentBrukertilganger()

    @PostMapping("/tilgangsmaskin/komplett/bulk")
    fun sjekkTilgangsmaskinresultat(identer: List<String>): TilgangsmaskinBulkResponse {
        return tilgangskontrollService.evaluerKomplettRegelsettForFlereBrukere(identer)
    }
}
