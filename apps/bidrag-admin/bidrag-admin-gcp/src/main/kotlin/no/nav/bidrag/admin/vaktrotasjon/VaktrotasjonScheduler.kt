package no.nav.bidrag.admin.vaktrotasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class VaktrotasjonScheduler(
    private val vaktrotasjonService: VaktrotasjonService,
    @param:Value($$"${NAIS_CLUSTER_NAME}") private val clusterName: String,
) {
    @Scheduled(cron = $$"${VAKTHAVENDE_ROTASJON_CRON}")
    @SchedulerLock(name = "vaktrotasjon")
    fun rotertVaktUkentlig() {
        LockAssert.assertLocked()
        if (clusterName != "prod-gcp") {
            LOGGER.info { "Kjører ikke skedulert vaktrotasjon i cluster $clusterName." }
            return
        }
        LOGGER.info { "Starter skedulert vaktrotasjon.." }
        vaktrotasjonService.kjørRotasjon()
    }
}
