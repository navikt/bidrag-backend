package no.nav.bidrag.automatiskjobb.batch.utils.forsendelse.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

/**
 * Opprett forsendelse-batchen brukes av flere andre batcher som derfor kan
 * trigge batchen automatisk via sitt eget cron-uttrykk, uavhengig av hverandre.
 */
@Component
class OpprettForsendelseBatchScheduler(
    private val opprettForsendelseBatch: OpprettForsendelseBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_OPPRETT_FORSENDELSE_CRON:-}")
    @SchedulerLock(name = "opprettForsendelseAldersjustering", lockAtMostFor = "PT4H")
    fun kjørForAldersjustering() {
        LOGGER.info { "Starter schedulert kjøring av opprett forsendelse batch (aldersjustering)" }
        opprettForsendelseBatch.start()
    }

    @Scheduled(cron = $$"${REVURDER_FORSKUDD_OPPRETT_FORSENDELSE_CRON:-}")
    @SchedulerLock(name = "opprettForsendelseRevurderForskudd", lockAtMostFor = "PT4H")
    fun kjørForRevurderForskudd() {
        LOGGER.info { "Starter schedulert kjøring av opprett forsendelse batch (revurder forskudd)" }
        opprettForsendelseBatch.start()
    }
}
