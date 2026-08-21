package no.nav.bidrag.automatiskjobb.batch.aldersjustering.bidrag.oppgave.opprettoppgave

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class OppgaveAldersjusteringBidragScheduler(
    private val oppgaveAldersjusteringBidragBatch: OppgaveAldersjusteringBidragBatch,
) {
    @Scheduled(cron = $$"${ALDERSJUSTERING_BIDRAG_OPPRETT_OPPGAVE_CRON:-}")
    @SchedulerLock(name = "oppgaveAldersjusteringBidrag", lockAtMostFor = "PT4H")
    fun kjør() {
        LOGGER.info { "Starter schedulert kjøring av opprett oppgave for aldersjustering bidrag batch" }
        oppgaveAldersjusteringBidragBatch.startOppgaveAldersjusteringBidragBatch(barnId = null)
    }
}
