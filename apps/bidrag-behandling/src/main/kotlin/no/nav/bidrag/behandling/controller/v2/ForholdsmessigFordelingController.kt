package no.nav.bidrag.behandling.controller.v2

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling.OpprettFFRequest
import no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling.SjekkForholdmessigFordelingResponse
import no.nav.bidrag.behandling.service.forholdsmessigfordeling.ForholdsmessigFordelingService
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.security.utils.TokenUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

@BehandlingRestControllerV2
@RequestMapping("/api/v2/behandling/forholdsmessigfordeling")
class ForholdsmessigFordelingController(
    private val forholdsmessigFordelingService: ForholdsmessigFordelingService,
) {
    @PostMapping("/{behandlingsid}")
    fun opprettForholdsmessigFordeling(
        @PathVariable behandlingsid: Long,
        @RequestBody(required = false) request: OpprettFFRequest?,
    ) {
        val saksbehandlerIdent = TokenUtils.hentSaksbehandlerIdent()
        SikkerhetsKontekst.medApplikasjonKontekst {
            forholdsmessigFordelingService.opprettEllerOppdaterForholdsmessigFordeling(behandlingsid, request = request, opprettetAvSaksbehandler = saksbehandlerIdent)
        }
    }

    @PostMapping("/nyeopplysninger/{behandlingsid}")
    fun skalLeggeTilBarnFraAndreSøknaderEllerBehandlinger(
        @PathVariable behandlingsid: Long,
    ): Boolean = forholdsmessigFordelingService.skalLeggeTilBarnFraAndreSøknaderEllerBehandlinger(behandlingsid)

    @PostMapping("/sjekk/{behandlingsid}")
    fun kanOppretteForholdsmessigFordeling(
        @PathVariable behandlingsid: Long,
        @RequestBody(required = false) request: OpprettFFRequest?,
    ): SjekkForholdmessigFordelingResponse = forholdsmessigFordelingService.sjekkSkalOppretteForholdsmessigFordeling(behandlingsid, request?.opprettetAvEnhet)

    @GetMapping("/kanendresoknadstatus/{soknadsid}")
    @Operation(
        description =
            "Sjekker om status på søknad kan endres. For klage/omgjøring kan status kun endres hvis søknaden er opprettet etter hovedsøknaden. Returnerer alltid true ellers.",
    )
    fun kanEndreSøknadStatus(
        @PathVariable soknadsid: Long,
    ): Boolean = forholdsmessigFordelingService.kanEndreSøknadStatus(soknadsid)
}
