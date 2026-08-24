package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.lagreb4

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Year

private val LOGGER = KotlinLogging.logger {}

@Component
class LagreB4InformasjonBidragScheduler(
    private val lagreB4InformasjonBidragBatch: LagreB4InformasjonBidragBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_LAGRE_B4_CRON:-}")
    @SchedulerLock(name = "lagreB4InformasjonBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av lagre B4-informasjon bidrag batch" }
        lagreB4InformasjonBidragBatch.startLagreB4InformasjonBidragBatch(
            fattetÅr = Year.now().value.toLong(),
            barn = null,
        )
    }
}
