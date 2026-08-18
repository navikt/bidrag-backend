package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.regnskap.service.PersistenceService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger { }

/**
 * Denne scheduled tasken er opprettet for å sørge for at feilede krav forsøkes å sendes over på nytt automatisk.
 * Dette gjøres ved å sette overføringstidspunkt til null, slik at SendKravScheduler plukker opp kravene ved neste kjøring.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
class ResendingAvKravScheduler(
    private val persistenceService: PersistenceService,
    private val sjekkAvBehandlingsstatusScheduler: SjekkAvBehandlingsstatusScheduler,
    private val kravSchedulerUtils: KravSchedulerUtils,
) {

    @Scheduled(cron = "\${scheduler.resendkrav.cron}")
    @SchedulerLock(name = "skedulertResendingAvKrav")
    @Transactional
    fun skedulertResendingAvKrav() {
        LockAssert.assertLocked()
        LOGGER.info { "Starter schedulert resending av alle krav som ikke har fått behandlingsstatus ok." }

        if (kravSchedulerUtils.harAktivtDriftsavvik()) {
            LOGGER.warn { "Det finnes aktive driftsavvik. Starter derfor ikke resending av alle krav." }
            return
        } else if (kravSchedulerUtils.erVedlikeholdsmodusPåslått()) {
            LOGGER.warn { "Vedlikeholdsmodus er påslått! Starter derfor ikke resending av alle krav." }
            return
        }

        // Kjører en sjekk av alle behandlingsstatuser før reset av de som fremdeles feiler.
        // Dette er for å unngå eventuelle nye krav som dukker opp rett før resending.
        sjekkAvBehandlingsstatusScheduler.kjørSjekkAvBehandlingsstatus()

        val konteringerSomIkkeHarFåttGodkjentBehandlingsstatus = persistenceService.hentAlleKonteringerUtenBehandlingsstatusOkUansettOmSendtEllerIkke()

        if (konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.isEmpty()) {
            LOGGER.info { "Det finnes ingen konteringer som ikke har sjekket behandlingsstatus." }
            return
        }

        konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.forEach {
            it.overføringstidspunkt = null
            it.oppdragsperiode?.oppdrag?.harFeiledeKonteringer = false
        }

        persistenceService.konteringRepository.saveAll(konteringerSomIkkeHarFåttGodkjentBehandlingsstatus)

        LOGGER.info {
            "Resatt overføringstidspunkt og harFeiledeKonteringer for totalt " +
                "${konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.size} konteringer."
        }
    }

    @Transactional
    fun resendingAvKravForSak(sakId: String) {
        LOGGER.info { "Starter schedulert resending av alle krav for sak $sakId." }

        // Kjører en sjekk av alle behandlingsstatuser før reset av de som fremdeles feiler.
        // Dette er for å unngå eventuelle nye krav som dukker opp rett før resending.
        sjekkAvBehandlingsstatusScheduler.kjørSjekkAvBehandlingsstatus()

        val konteringerSomIkkeHarFåttGodkjentBehandlingsstatus = persistenceService.hentAlleKonteringerUtenBehandlingsstatusOkUansettOmSendtEllerIkke()

        val konteringerForSak = konteringerSomIkkeHarFåttGodkjentBehandlingsstatus.filter { it.oppdragsperiode?.oppdrag?.sakId == sakId }

        if (konteringerForSak.isEmpty()) {
            LOGGER.info { "Det finnes ingen konteringer for sak $sakId som ikke har sjekket behandlingsstatus." }
            return
        }

        konteringerForSak.forEach {
            it.overføringstidspunkt = null
            it.oppdragsperiode?.oppdrag?.harFeiledeKonteringer = false
        }

        persistenceService.konteringRepository.saveAll(konteringerForSak)
    }
}
