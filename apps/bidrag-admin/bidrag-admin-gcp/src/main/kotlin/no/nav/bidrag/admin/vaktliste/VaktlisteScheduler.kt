package no.nav.bidrag.admin.vaktliste

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.admin.service.VaktlisteService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class VaktlisteScheduler(
    private val vaktlisteService: VaktlisteService,
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
        vaktlisteService.roterVakthavende()
    }
}
