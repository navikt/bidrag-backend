package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.fattvedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.automatiskjobb.persistence.entity.enums.Behandlingstype
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class FattVedtakOmAldersjusteringerBidragScheduler(
    private val fattVedtakOmAldersjusteringerBidragBatch: FattVedtakOmAldersjusteringerBidragBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_FATT_VEDTAK_CRON:-}")
    @SchedulerLock(name = "fattVedtakOmAldersjusteringerBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av fatt vedtak om aldersjustering bidrag batch" }
        fattVedtakOmAldersjusteringerBidragBatch.startFattVedtakOmAldersjusteringBidragBatch(
            barnId = null,
            simuler = false,
            behandlingstyper = listOf(Behandlingstype.MANUELL, Behandlingstype.FATTET_FORSLAG, Behandlingstype.INGEN),
            kunRedusertBidrag = false,
        )
    }
}
