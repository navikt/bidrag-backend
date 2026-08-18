package no.nav.bidrag.regnskap.hendelse.schedule.krav

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockkStatic
import net.javacrumbs.shedlock.core.LockAssert
import no.nav.bidrag.regnskap.service.PersistenceService
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class ResendingAvKravSchedulerTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var scheduler: SjekkAvBehandlingsstatusScheduler

    @MockK(relaxed = true)
    private lateinit var kravSchedulerUtils: KravSchedulerUtils

    @InjectMockKs
    private lateinit var resendingAvKravScheduler: ResendingAvKravScheduler

    @BeforeEach
    fun setup() {
        mockkStatic(LockAssert::class)
        every { LockAssert.assertLocked() } just Runs
        every { kravSchedulerUtils.harAktivtDriftsavvik() } returns false
        every { kravSchedulerUtils.erVedlikeholdsmodusPåslått() } returns false
    }

    @Test
    fun `skal resette overføringstidspunkt og har feilede konteringer`() {
        val oppdrag = TestData.opprettOppdrag(harFeiledeKonteringer = true)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering1 = TestData.opprettKontering(
            konteringId = 1,
            oppdragsperiode = oppdragsperiode,
            behandlingsstatusOkTidspunkt = LocalDateTime.now(),
            overforingstidspunkt = LocalDateTime.now().minusMinutes(1),
            sisteReferansekode = "DUMMY-UID",
        )
        val kontering2 = TestData.opprettKontering(
            konteringId = 2,
            oppdragsperiode = oppdragsperiode,
            behandlingsstatusOkTidspunkt = null,
            overforingstidspunkt = LocalDateTime.now().minusMinutes(1),
            sisteReferansekode = "DUMMY-UID2",
        )

        oppdragsperiode.konteringer = listOf(kontering1, kontering2)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)

        every { persistenceService.hentAlleKonteringerUtenBehandlingsstatusOkUansettOmSendtEllerIkke() } returns listOf(kontering2)

        resendingAvKravScheduler.skedulertResendingAvKrav()

        kontering1.overføringstidspunkt shouldNotBe null
        kontering2.overføringstidspunkt shouldBe null
        kontering1.oppdragsperiode?.oppdrag?.harFeiledeKonteringer shouldBe false
        kontering2.oppdragsperiode?.oppdrag?.harFeiledeKonteringer shouldBe false
    }
}
