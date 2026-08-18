package no.nav.bidrag.regnskap.hendelse.schedule.påløp

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT100M", defaultLockAtLeastFor = "PT2M")
class PåløpskjøringScheduler(
    private val persistenceService: PersistenceService,
    private val påløpskjøringService: PåløpskjøringService,
    private val slackService: SlackService,
    @param:Value("\${NAIS_CLUSTER_NAME}") private val clusterName: String,
) {

    @Scheduled(cron = "\${scheduler.påløpkjøring.cron}")
    @SchedulerLock(name = "skedulertPåløpskjøring")
    fun skedulertPåløpskjøring() {
        LockAssert.assertLocked()

        LOGGER.info { "Starter skedulert påløpskjøring.." }
        persistenceService.hentIkkeKjørtePåløp().minByOrNull { it.forPeriode }.let {
            if (it != null) {
                if (it.kjøredato.isBefore(LocalDateTime.now())) {
                    påløpskjøringService.startPåløpskjøringMaskinelt(it)
                } else {
                    LOGGER.info { "Fant ingen påløp som skulle kjøres på dette tidspunkt. Neste påløpskjøring er for periode: ${it.forPeriode} som kjøres: ${it.kjøredato}" }
                }
            } else {
                if (clusterName == "prod-gcp") {
                    slackService.sendMelding("Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!")
                    LOGGER.error { "Det finnes ingen fremtidige planlagte påløp! Påløpsfil kommer ikke til å generes før dette legges inn!" }
                }
            }
        }
    }
}
