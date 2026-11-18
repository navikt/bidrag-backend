package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import net.javacrumbs.shedlock.core.LockAssert
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.regnskap.UnleashFeatures
import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import no.nav.bidrag.regnskap.service.EndreMottakerService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EndreMottakerSchedulerTest {
    private val endreMottakerService = mockk<EndreMottakerService>()
    private val slackService = mockk<SlackService>(relaxed = true)
    private val scheduler = EndreMottakerScheduler(endreMottakerService, slackService, "test")

    @BeforeEach
    fun setup() {
        mockkStatic(LockAssert::class)
        every { LockAssert.assertLocked() } just Runs
        mockkObject(UnleashFeaturesProvider)
        every { UnleashFeaturesProvider.isEnabled(UnleashFeatures.ENDRE_MOTTAKER.featureName, false, false) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(UnleashFeaturesProvider)
        unmockkStatic(LockAssert::class)
    }

    @Test
    fun `skal ikke resende eller varsle naar funksjonen er deaktivert`() {
        scheduler.skedulertResendingAvEndringAvMottaker()
        scheduler.dagligVarslingOmFeiledeEndringer()

        verify(exactly = 0) { endreMottakerService.hentIkkeGodkjenteEndringer() }
        verify(exactly = 0) { endreMottakerService.hentFeiledeOverføringer() }
        verify(exactly = 0) { slackService.sendMelding(any()) }
    }

    @Test
    fun `skal resende og varsle naar funksjonen er aktivert`() {
        every { UnleashFeaturesProvider.isEnabled(UnleashFeatures.ENDRE_MOTTAKER.featureName, false, false) } returns true
        val endring = EndreMottaker(id = 1, vedtakId = 2, saksnummer = "123", barnIdent = "11111111111", nyMottakerIdent = "22222222222")
        every { endreMottakerService.hentIkkeGodkjenteEndringer() } returns listOf(endring)
        every { endreMottakerService.hentFeiledeOverføringer() } returns listOf(endring)
        every { endreMottakerService.overførEndreMottaker(1) } just Runs
        scheduler.skedulertResendingAvEndringAvMottaker()
        scheduler.dagligVarslingOmFeiledeEndringer()

        verify(exactly = 1) { endreMottakerService.overførEndreMottaker(1) }
        verify(exactly = 1) { slackService.sendMelding(match { it.contains("123") }) }
    }
}
