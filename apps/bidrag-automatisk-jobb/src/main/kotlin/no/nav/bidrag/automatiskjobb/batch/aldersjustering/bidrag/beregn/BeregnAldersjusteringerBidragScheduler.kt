package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.beregn

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Status
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class BeregnAldersjusteringerBidragScheduler(
    private val beregnAldersjusteringerBidragBatch: BeregnAldersjusteringerBidragBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_BEREGN_CRON:-}")
    @SchedulerLock(name = "beregnAldersjusteringerBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av beregn aldersjustering bidrag batch" }
        beregnAldersjusteringerBidragBatch.startBeregnAldersjusteringBidragBatch(
            simuler = false,
            statuser = listOf(Status.UBEHANDLET, Status.FEILET, Status.SIMULERT),
            barn = null,
        )
    }
}
