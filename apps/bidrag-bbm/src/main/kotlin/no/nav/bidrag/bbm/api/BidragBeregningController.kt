package no.nav.bidrag.bbm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import no.nav.bidrag.bbm.bo.SammenknyttSøknaderRequest
import no.nav.bidrag.bbm.bo.SlettHovedsøknadRequest
import no.nav.bidrag.bbm.bo.SlettSammenknytningForSøknadRequest
import no.nav.bidrag.bbm.model.BidragBeregningSaksnummerRequest
import no.nav.bidrag.bbm.service.BBMService
import no.nav.bidrag.bbm.service.BisysService
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.FeilregistrerSøknadsBarnRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentBPsÅpneSøknaderRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.LeggTilBarnIFFSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlerenhetRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlingsidRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterReferanseGebyrRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadRequest
import no.nav.bidrag.transport.søknad.FinnSammenknytningerHovedsøknadRequest
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/beregning")
@Protected
class BidragBeregningController(
    val bbmService: BBMService,
    val bisysService: BisysService,
) {
    @PostMapping
    @Operation(
        description = "Henter alle samæværsklasser og beregnede og faktiske bidrag for saksummer, personidentBarn og datoSøknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hentet forespurt data. Vil returnere tom liste hvis ingen samværsklasser og perider funnet for vedtak",
            ),
            ApiResponse(responseCode = "404", description = "Vedtak finnes ikke"),
        ],
    )
    fun hentBeregning(
        @Valid @RequestBody request: BidragBeregningRequestDto,
    ) = bbmService.hentSisteBidragOgSamvær(request)

    @PostMapping("/saksnummer")
    @Operation(
        tags = ["debugging"],
        description = "Henter alle samæværsklasser og beregnede og faktiske bidrag for saksummer",
        summary = "Brukes for debugging og for å sjekke om saken har bidrag perioder",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hentet forespurt data. Vil returnere tom liste hvis ingen samværsklasser og perioder funnet for sak",
            ),
            ApiResponse(responseCode = "404", description = "Saksnummer finnes ikke"),
        ],
    )
    fun hentAlleBeregningerOgSamværForSaksnummer(
        @Valid @RequestBody request: BidragBeregningSaksnummerRequest,
    ) = bbmService.hentAllePeriodeBidragOgSamværsklasseForSaksnummer(request.saksnummerListe.map { it.verdi })

    @PostMapping("/apnesoknader")
    @Operation(
        description = "Henter alle åpne søknader i saker der angitt person er bidragspliktig",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hentet forespurt data. Vil returnere tom liste hvis det ikke finnes noen åpne søknader for person",
            ),
        ],
    )
    fun hentAlleÅpneSøknaderForPerson(
        @Valid @RequestBody request: HentBPsÅpneSøknaderRequest,
    ) = bisysService.hentÅpneSøknaderForPerson(request.personidentBidragspliktig)

    @PostMapping("/opprettsoknad")
    @Operation(
        description = "Oppretter søknader",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppretter søknader i Bisys",
            ),
        ],
    )
    fun opprettSøknader(
        @Valid @RequestBody request: OpprettSøknadRequest,
    ) = bisysService.opprettSøknader(request)

    @PostMapping("/settbehandlingsid")
    @Operation(
        description = "Lagrer behandlingsid på søknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppdaterer søknad i Bisys med behandlingsid",
            ),
        ],
    )
    fun lagreBehandlingsid(
        @Valid @RequestBody request: OppdaterBehandlingsidRequest,
    ) = bisysService.oppdaterBehandlingsid(request)

    @PostMapping("/oppdaterbehandlerenhet")
    @Operation(
        description = "Oppdaterer behandlerenhet på søknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppdaterer søknad i Bisys med behandlerenhet",
            ),
        ],
    )
    fun oppdaterBehandlerenhet(
        @Valid @RequestBody request: OppdaterBehandlerenhetRequest,
    ) = bisysService.oppdaterBehandlerenhet(request)

    @PostMapping("/feilregistrersoknad")
    @Operation(
        description = "Feilregister søknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Feilregistrer søknad",
            ),
        ],
    )
    fun feilregistrerSøknad(
        @Valid @RequestBody request: FeilregistrerSøknadRequest,
    ) = bisysService.feilregistrerSøknad(request)

    @PostMapping("/feilregistrersoknadsbarn")
    @Operation(
        description = "Feilregister søknadsbarn",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Feilregistrer søknad",
            ),
        ],
    )
    fun feilregistrerSøknadsbarn(
        @Valid @RequestBody request: FeilregistrerSøknadsBarnRequest,
    ) = bisysService.feilregistrerSøknadsbarn(request)

    @PostMapping("/leggtilbarniffsoknad")
    @Operation(
        description = "Legg til barn i eksisterende søknad om  forholdsmessig fordeling",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Barn lagt til i søknad",
            ),
        ],
    )
    fun leggTilBarnISøknad(
        @Valid @RequestBody request: LeggTilBarnIFFSøknadRequest,
    ) = bisysService.leggTilBarnIFFSøknad(request)

    @PostMapping("/hentsoknad")
    @Operation(
        description = "Henter angitt søknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Søknad hentet",
            ),
        ],
    )
    fun hentSøknad(
        @Valid @RequestBody request: HentSøknadRequest,
    ) = bisysService.hentSøknad(request)

    @PostMapping("/oppdaterreferansegebyr")
    @Operation(
        description = "Oppdaterer referanse på tilknyttet gebyrsøknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppdaterer søknad i Bisys med referanse gebyr",
            ),
        ],
    )
    fun oppdaterReferanseGebyr(
        @Valid @RequestBody request: OppdaterReferanseGebyrRequest,
    ) = bisysService.oppdaterReferanseGebyr(request)

    @PostMapping("/alleberegningerogsamvar")
    @Operation(
        description = "Henter alle samæværsklasser og beregnede og faktiske bidrag for saksummer, personidentBarn og datoSøknad",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hentet forespurt data. Vil returnere tom liste hvis ingen samværsklasser og perider funnet for vedtak",
            ),
            ApiResponse(responseCode = "404", description = "Vedtak finnes ikke"),
        ],
    )
    fun hentAlleBeregninger(
        @Valid @RequestBody request: BidragBeregningRequestDto,
    ) = bbmService.hentAlleBeregningerOgSamværForVedtak(request)

    @PostMapping("/sammenknyttsoknader")
    @Operation(
        description = "Lag knytning mellom angitte søknader",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Søknader er knyttet sammen",
            ),
        ],
    )
    fun sammenknyttSøknader(
        @Valid @RequestBody request: SammenknyttSøknaderRequest,
    ) = bisysService.sammenknyttSøknader(request)

    @PostMapping("/slettsammenknytningsoknad")
    @Operation(
        description = "Slett sammenknytning for angitt søknad.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Sammenknytning er slettet",
            ),
        ],
    )
    fun slettSammenknytningForReferertSøknad(
        @Valid @RequestBody request: SlettSammenknytningForSøknadRequest,
    ) = bisysService.slettSammenknytningReferertSøknad(request)

    @PostMapping("/endresammenknytningsoknad")
    @Operation(
        description = "Endrer eksisterende sammenknytning for referert søknad ",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Sammenknytning er endret",
            ),
        ],
    )
    fun endreSammenknytningerForSøknad(
        @Valid @RequestBody request: SammenknyttSøknaderRequest,
    ) = bisysService.endreSammenknytningSøknad(request)

    @PostMapping("/sletthovedsoknad")
    @Operation(
        description = "Sletter sammenknytninger for hovedsøknad ",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Sammenknytninger er slettet",
            ),
        ],
    )
    fun slettSammenknytningerForHovedsøknad(
        @Valid @RequestBody request: SlettHovedsøknadRequest,
    ) = bisysService.slettSammenknytningerHovedsøknad(request)

    @PostMapping("/finnsammenknytningerhovedsoknad")
    @Operation(
        description = "Finner sammenknytninger for hovedsøknad ",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Sammenknytninger er hentet",
            ),
        ],
    )
    fun finnSammenknytningerForHovedsøknad(
        @Valid @RequestBody request: FinnSammenknytningerHovedsøknadRequest,
    ) = bisysService.finnSammenknytningerHovedsøknad(request)
}
