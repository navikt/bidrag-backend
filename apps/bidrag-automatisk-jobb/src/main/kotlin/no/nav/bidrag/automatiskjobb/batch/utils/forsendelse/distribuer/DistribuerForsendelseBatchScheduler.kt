package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.distribuer

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

/**
 * Distribuer forsendelse-batchen brukes av flere andre batcher som derfor kan
 * trigge batchen automatisk via sitt eget cron-uttrykk, uavhengig av hverandre.
 */
@Component
class DistribuerForsendelseBatchScheduler(
    private val distribuerForsendelseBidragBatch: DistribuerForsendelseBidragBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_DISTRIBUER_FORSENDELSE_CRON:-}")
    @SchedulerLock(name = "distribuerForsendelseAldersjustering", lockAtMostFor = "PT4H")
    fun kjørForAldersjustering() {
        LOGGER.info { "Starter schedulert kjøring av distribuer forsendelse batch (aldersjustering)" }
        distribuerForsendelseBidragBatch.start()
    }

    @Scheduled(cron = $$"${REVURDER_FORSKUDD_DISTRUBUER_FORSENDELSE_CRON:-}")
    @SchedulerLock(name = "distribuerForsendelseRevurderForskudd", lockAtMostFor = "PT4H")
    fun kjørForRevurderForskudd() {
        LOGGER.info { "Starter schedulert kjøring av distribuer forsendelse batch (revurder forskudd)" }
        distribuerForsendelseBidragBatch.start()
    }
}
