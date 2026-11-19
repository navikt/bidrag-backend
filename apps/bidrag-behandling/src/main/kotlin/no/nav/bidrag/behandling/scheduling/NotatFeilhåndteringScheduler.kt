package no.nav.bidrag.behandling.scheduling

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.behandling.database.datamodell.extensions.BehandlingMetadataDo.Companion.lesNotatbestillinger
import no.nav.bidrag.behandling.database.repository.BehandlingRepository
import no.nav.bidrag.behandling.service.NotatOpplysningerService
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}
private const val BATCHSTØRRELSE = 100
private const val VENTETID_FØR_NOTATRETRY_MINUTTER = 1L

@Component
class NotatFeilhåndteringScheduler(
    private val notatService: NotatOpplysningerService,
    private val behandlingRepository: BehandlingRepository,
) {
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "opprettNotatHvisFeilet", lockAtLeastFor = "10m")
    fun oppdaterStatusPaFerdigstilteDokumenterSkeduler() {
        val nå = LocalDateTime.now()
        var etterId = 0L
        SikkerhetsKontekst.medApplikasjonKontekst {
            while (true) {
                val batch = behandlingRepository.hentBehandlingerSomManglerNotater(
                    etterDato = nå.minusMonths(6),
                    førDato = nå.minusMinutes(VENTETID_FØR_NOTATRETRY_MINUTTER),
                    etterId = etterId,
                    batchstørrelse = BATCHSTØRRELSE,
                )
                if (batch.isEmpty()) break
                batch.forEach { behandling ->
                    val saksnumre = lesNotatbestillinger(behandling.notatbestillinger)
                        ?.filter { it.journalpostId == null }
                        ?.map { it.saksnummer }
                        ?: listOf(null)
                    saksnumre.forEach { saksnummer ->
                        try {
                            notatService.opprettNotat(behandling.id, saksnummer = saksnummer)
                        } catch (e: Exception) {
                            log.error(e) { "Det skjedde en feil ved opprettelse av notat for behandling ${behandling.id}" }
                        }
                    }
                }
                if (batch.size < BATCHSTØRRELSE) break
                etterId = batch.last().id
            }
        }
    }
}
