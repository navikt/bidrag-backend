package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.opprett

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Year

private val LOGGER = KotlinLogging.logger {}

@Component
class OpprettAldersjusteringerBidragScheduler(
    private val opprettAldersjusteringerBidragBatch: OpprettAldersjusteringerBidragBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_OPPRETT_CRON:-}")
    @SchedulerLock(name = "opprettAldersjusteringerBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av opprett aldersjustering bidrag batch" }
        opprettAldersjusteringerBidragBatch.startOpprettAldersjusteringBidragBatch(
            aldersjusteringsdato = null,
            år = Year.now().value.toLong(),
        )
    }
}
