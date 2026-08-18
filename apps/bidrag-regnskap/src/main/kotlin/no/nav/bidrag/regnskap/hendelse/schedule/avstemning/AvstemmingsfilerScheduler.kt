package no.nav.bidrag.regnskap.hendelse.schedule.avstemning

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.AvstemmingService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class AvstemmingsfilerScheduler(
    private val avstemmingService: AvstemmingService,
) {

    @Scheduled(cron = "\${scheduler.avstemning.cron}")
    @SchedulerLock(name = "skedulertOpprettelseAvAvstemmingsfiler")
    fun skedulertOpprettelseAvAvstemmingsfiler() {
        LockAssert.assertLocked()

        val dato = LocalDate.now().minusDays(1)
        LOGGER.info { "Starter schedulert generering av avstemmingsfiler for $dato." }

        avstemmingService.startAvstemming(dato)
    }
}
