package no.nav.bidrag.arbeidsflyt.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.arbeidsflyt.service.BehandlingSchedulerService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@RequestMapping("/admin")
class AdminController(
    private val behandlingSchedulerService: BehandlingSchedulerService,
) {
    @PostMapping("/behandling/{behandlingId}/rekjor")
    @Operation(
        description =
            "Rekjører siste lagrede behandlingshendelse for en behandling med oppdaterte data fra bidrag-behandling. " +
                "Oppretter, oppdaterer og ferdigstiller oppgaver slik at de stemmer med behandlingen, " +
                "og lukker åpne oppgaver hvis behandlingen ikke lenger er åpen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun rekjørBehandlingHendelser(@PathVariable behandlingId: Long) {
        behandlingSchedulerService.behandleOgOppdaterStatusSjekket(behandlingId)
    }
}
