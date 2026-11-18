package no.nav.bidrag.behandling.scheduling

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.behandling.database.repository.BehandlingRepository
import no.nav.bidrag.behandling.service.forholdsmessigfordeling.ForholdsmessigFordelingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ForholdsmessigFordelingFeilhåndteringScheduler(
    private val forholdsmessigFordelingService: ForholdsmessigFordelingService,
    private val behandlingRepository: BehandlingRepository,
) {
    @Scheduled(cron = "0 */15 * * * *")
    @SchedulerLock(name = "ryddOppFFSøknaderForSlettedeBehandlinger", lockAtLeastFor = "10m")
    fun ryddOppFFSøknaderForSlettedeBehandlinger() {
        val behandlingIder = behandlingRepository.hentIdSlettedeBehandlingerHvorOpprettelseAvFFFeilet()
        log.info { "Fant ${behandlingIder.size} slettede behandlinger hvor opprettelse av FF feilet" }
        behandlingIder.forEach { behandlingId ->
            try {
                forholdsmessigFordelingService.ryddOppFFSøknaderForSlettetBehandling(behandlingId)
                log.info { "Ryddet opp FF-søknader for slettet behandling $behandlingId" }
            } catch (e: Exception) {
                log.error(e) { "Det skjedde en feil ved opprydding av FF-søknader for slettet behandling $behandlingId" }
            }
        }
    }
}
