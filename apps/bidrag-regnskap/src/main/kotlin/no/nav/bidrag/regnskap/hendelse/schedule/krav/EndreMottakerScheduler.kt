package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.bidrag.regnskap.service.EndreMottakerService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger { }

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class EndreMottakerScheduler(
    private val endreMottakerService: EndreMottakerService,
    private val slackService: SlackService,
    @param:Value($$"${NAIS_CLIENT_ID}") private val clientId: String,
) {

    @Scheduled(cron = $$"${scheduler.endremottaker.cron}")
    @SchedulerLock(name = "skedulertResendingAvEndringAvMottaker")
    @Transactional
    fun skedulertResendingAvEndringAvMottaker() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter skedulert resending av endringer av mottaker som ikke er godkjent av skatt." }
        endreMottakerService.resendIkkeGodkjenteEndringer()
    }

    @Scheduled(cron = $$"${scheduler.endremottakervarsling.cron}")
    @SchedulerLock(name = "dagligVarslingOmFeiledeEndringerAvMottaker")
    fun dagligVarslingOmFeiledeEndringer() {
        LockAssert.assertLocked()
        val feilede = endreMottakerService.hentFeiledeOverføringer()

        if (feilede.isEmpty()) {
            LOGGER.info { "Det finnes ingen feilede endringer av mottaker å varsle om." }
            return
        }

        val sakerMedFeil = feilede.joinToString("\n") { "• Sak ${it.saksnummer} (vedtak ${it.vedtakId})" }
        LOGGER.warn { "Det finnes ${feilede.size} feilede endringer av mottaker som ikke er godkjent av skatt." }
        slackService.sendMelding(
            ":ohno: ${feilede.size} endring(er) av mottaker feiler mot skatt i $clientId:\n$sakerMedFeil",
        )
    }
}
