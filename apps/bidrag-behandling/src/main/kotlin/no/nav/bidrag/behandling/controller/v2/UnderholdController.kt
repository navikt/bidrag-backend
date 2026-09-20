package no.nav.bidrag.behandling.controller.v2

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import no.nav.bidrag.behandling.behandlingNotFoundException
import no.nav.bidrag.behandling.database.datamodell.Underholdskostnad
import no.nav.bidrag.behandling.database.repository.BehandlingRepository
import no.nav.bidrag.behandling.dto.v2.underhold.BarnDto
import no.nav.bidrag.behandling.dto.v2.underhold.BeregnetUnderholdskostnad
import no.nav.bidrag.behandling.dto.v2.underhold.OppdatereBegrunnelseRequest
import no.nav.bidrag.behandling.dto.v2.underhold.OppdatereFaktiskTilsynsutgiftRequest
import no.nav.bidrag.behandling.dto.v2.underhold.OppdatereForpleiningRequest
import no.nav.bidrag.behandling.dto.v2.underhold.OppdatereTilleggsstønadRequest
import no.nav.bidrag.behandling.dto.v2.underhold.OppdatereUnderholdResponse
import no.nav.bidrag.behandling.dto.v2.underhold.OpprettUnderholdskostnadBarnResponse
import no.nav.bidrag.behandling.dto.v2.underhold.SletteUnderholdselement
import no.nav.bidrag.behandling.dto.v2.underhold.StønadTilBarnetilsynDto
import no.nav.bidrag.behandling.dto.v2.underhold.UnderholdDto
import no.nav.bidrag.behandling.dto.v2.underhold.Underholdselement
import no.nav.bidrag.behandling.dto.v2.underhold.UnderholdskostnadDto
import no.nav.bidrag.behandling.service.UnderholdService
import no.nav.bidrag.behandling.transformers.Dtomapper
import no.nav.bidrag.behandling.transformers.underhold.henteOgValidereUnderholdskostnad
import no.nav.bidrag.behandling.transformers.underhold.tilStønadTilBarnetilsynDtos
import no.nav.bidrag.behandling.transformers.underhold.valider
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

private val log = KotlinLogging.logger {}

@BehandlingRestControllerV2
class UnderholdController(
    private val behandlingRepository: BehandlingRepository,
    private val underholdService: UnderholdService,
    private val dtomapper: Dtomapper,
) {
    @DeleteMapping("/behandling/{behandlingsid}/underhold")
    @Operation(
        description =
        "Sletter fra underholdskostnad i behandling. Returnerer oppdaterte underholdsobjekt. Objektet " +
            " vil være null dersom barn slettes.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun sletteFraUnderhold(
        @PathVariable behandlingsid: Long,
        @Valid @RequestBody(required = true) request: SletteUnderholdselement,
    ): OppdatereUnderholdResponse {
        log.info { "Sletter fra underholdskostnad i behandling $behandlingsid" }
        secureLogger.info { "Sletter fra underholdskostnad i behandling $behandlingsid med forespørsel $request" }

        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        val underholdskostnad = henteOgValidereUnderholdskostnad(behandling!!, request.idUnderhold)

        underholdService.sletteFraUnderhold(behandling, request)

        return if (request.type == Underholdselement.BARN) {
            val beregnet = dtomapper.run { behandling.tilBeregnetUnderholdskostnad() }
            OppdatereUnderholdResponse(
                beregnetUnderholdskostnader = beregnet,
                valideringsfeil = behandling.underholdskostnader.valider(beregnet.perioderForUnderhold()),
                underholdId = request.idUnderhold,
            )
        } else {
            underholdskostnad.tilRespons()
        }
    }

    @PutMapping("/behandling/{behandlingsid}/underhold/{underholdsid}/barnetilsyn")
    @Operation(
        description =
        "Oppdatere stønad til barnetilsyn for underholdskostnad i behandling. Returnerer oppdatert element.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppdatereStønadTilBarnetilsyn(
        @PathVariable behandlingsid: Long,
        @PathVariable underholdsid: Long,
        @Valid @RequestBody(required = true) request: StønadTilBarnetilsynDto,
    ): OppdatereUnderholdResponse? {
        log.info { "Oppdaterer stønad til barnetilsyn for behandling $behandlingsid" }
        secureLogger.info { "Oppdaterer stønad til barnetilsyn for behandling $behandlingsid med forespørsel $request" }

        val behandling =
            try {
                behandlingRepository
                    .findBehandlingById(behandlingsid)
                    .orElseThrow { behandlingNotFoundException(behandlingsid) }
            } catch (exception: Exception) {
                exception.printStackTrace()
                null
            }

        val underholdskostnad = henteOgValidereUnderholdskostnad(behandling!!, underholdsid)

        underholdService.oppdatereStønadTilBarnetilsynManuelt(underholdskostnad, request)
        return underholdskostnad.tilRespons()
    }

    @PutMapping("/behandling/{behandlingsid}/underhold/{underholdsid}/faktisk_tilsynsutgift")
    @Operation(
        description =
        "Oppdatere faktisk tilsynsutgift for underholdskostnad i behandling. Returnerer oppdatert " +
            "element.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppdatereFaktiskTilsynsutgift(
        @PathVariable behandlingsid: Long,
        @PathVariable underholdsid: Long,
        @Valid @RequestBody(required = true) request: OppdatereFaktiskTilsynsutgiftRequest,
    ): OppdatereUnderholdResponse {
        log.info { "Oppdaterer faktisk tilsynsutgift for behandling $behandlingsid" }
        secureLogger.info { "Oppdaterer faktisk tilsynsutgift  for behandling $behandlingsid med forespørsel $request" }

        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        val underholdskostnad = henteOgValidereUnderholdskostnad(behandling, underholdsid)

        underholdService.oppdatereFaktiskeTilsynsutgifter(underholdskostnad, request)
        return underholdskostnad.tilRespons()
    }

    @PutMapping("/behandling/{behandlingsid}/underhold/{underholdsid}/tilleggsstonad")
    @Operation(
        description =
        "Oppdatere tilleggsstønad for underholdskostnad i behandling. Returnerer oppdatert element.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppdatereTilleggsstønad(
        @PathVariable behandlingsid: Long,
        @PathVariable underholdsid: Long,
        @Valid @RequestBody(required = true) request: OppdatereTilleggsstønadRequest,
    ): OppdatereUnderholdResponse {
        log.info { "Oppdaterer tilleggsstønad for behandling $behandlingsid" }
        secureLogger.info { "Oppdaterer tilleggsstønad for behandling $behandlingsid med forespørsel $request" }

        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        val underholdskostnad = henteOgValidereUnderholdskostnad(behandling, underholdsid)

        underholdService.oppdatereTilleggsstønad(underholdskostnad, request)
        return underholdskostnad.tilRespons()
    }

    @PutMapping("/behandling/{behandlingsid}/underhold/{underholdsid}/forpleining")
    @Operation(
        description =
        "Oppdatere forpleining for underholdskostnad i behandling. Returnerer oppdatert element.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppdatereForpleining(
        @PathVariable behandlingsid: Long,
        @PathVariable underholdsid: Long,
        @Valid @RequestBody(required = true) request: OppdatereForpleiningRequest,
    ): OppdatereUnderholdResponse {
        log.info { "Oppdaterer forpleining for behandling $behandlingsid" }
        secureLogger.info { "Oppdaterer forpleining for behandling $behandlingsid med forespørsel $request" }

        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        val underholdskostnad = henteOgValidereUnderholdskostnad(behandling, underholdsid)

        underholdService.oppdatereForpleining(underholdskostnad, request)
        return underholdskostnad.tilRespons()
    }

    @PutMapping("/behandling/{behandlingsid}/underhold/begrunnelse")
    @Operation(
        description = "Oppdatere begrunnelse for underhold relatert til søknadsbarn eller andre barn.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppdatereBegrunnelse(
        @PathVariable behandlingsid: Long,
        @RequestBody(required = true) request: OppdatereBegrunnelseRequest,
    ): Set<UnderholdDto> {
        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        underholdService.oppdatereBegrunnelse(behandling, request)

        return dtomapper.run { tilUnderholdskostnadDto(behandling, emptyList(), false) }
    }

    @PutMapping("/behandling/{behandlingsid}/underhold/{underholdsid}/tilsynsordning")
    @Operation(
        description = "Angir om søknadsbarn har tilsynsordning.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppdatereTilsynsordning(
        @PathVariable behandlingsid: Long,
        @PathVariable underholdsid: Long,
        @RequestParam(required = true) harTilsynsordning: Boolean,
    ): OppdatereUnderholdResponse {
        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        val underholdskostnad = henteOgValidereUnderholdskostnad(behandling, underholdsid)

        underholdService.oppdatereTilsynsordning(underholdskostnad, harTilsynsordning)
        return underholdskostnad.tilRespons()
    }

    @PostMapping("/behandling/{behandlingsid}/underhold/opprette")
    @Operation(
        description = "Oppretter underholdselement med faktiske utgifter for BMs andre barn. Legges manuelt inn av saksbehandler.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    fun oppretteUnderholdForBarn(
        @PathVariable behandlingsid: Long,
        @RequestBody(required = true) gjelderBarn: BarnDto,
    ): OpprettUnderholdskostnadBarnResponse {
        val behandling =
            behandlingRepository
                .findBehandlingById(behandlingsid)
                .orElseThrow { behandlingNotFoundException(behandlingsid) }

        val underholdskostnad = underholdService.oppretteUnderholdskostnad(behandling, gjelderBarn)
        val beregnet = dtomapper.run { behandling.tilBeregnetUnderholdskostnad() }
        return OpprettUnderholdskostnadBarnResponse(
            underholdskostnad = dtomapper.tilUnderholdDto(underholdskostnad),
            beregnetUnderholdskostnader = beregnet,
            valideringsfeil = behandling.underholdskostnader.valider(beregnet.perioderForUnderhold()),
        )
    }

    private fun Underholdskostnad.tilRespons() = dtomapper.run {
        val beregnet = behandling.tilBeregnetUnderholdskostnad()
        OppdatereUnderholdResponse(
            faktiskTilsynsutgift = faktiskeTilsynsutgifter.tilFaktiskeTilsynsutgiftDtos(),
            stønadTilBarnetilsyn = barnetilsyn.tilStønadTilBarnetilsynDtos(),
            tilleggsstønad = tilleggsstønad.tilTilleggsstønadDtos(),
            forpleining = forpleining.tilForpleiningDtos(),
            beregnetUnderholdskostnader = beregnet,
            valideringsfeil = behandling.underholdskostnader.valider(beregnet.perioderForUnderhold()),
            underholdId = id!!,
        )
    }

    /** Knytter hvert barns beregnede perioder til riktig underholdskostnad, på ident og stønadstype. */
    private fun Set<BeregnetUnderholdskostnad>.perioderForUnderhold(): (Underholdskostnad) -> Set<UnderholdskostnadDto> = { u ->
        dtomapper.run { perioderForBarn(u.personIdent, u.rolle?.stønadstype) }
    }
}
