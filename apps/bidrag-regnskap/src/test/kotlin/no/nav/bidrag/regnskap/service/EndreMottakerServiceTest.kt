package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.BidragReskontroConsumer
import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class EndreMottakerServiceTest {

    @MockK
    private lateinit var persistenceService: PersistenceService

    @MockK
    private lateinit var bidragReskontroConsumer: BidragReskontroConsumer

    @MockK
    private lateinit var kravService: KravService

    @MockK(relaxed = true)
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    @InjectMockKs
    private lateinit var endreMottakerService: EndreMottakerService

    private val id = 1L
    private val sakId = "123456"
    private val barnIdent = "11111111111"
    private val nyMottakerIdent = "22222222222"

    private fun endreMottaker(godkjent: LocalDateTime? = null, overført: LocalDateTime? = null) = EndreMottaker(
        id = id,
        vedtakId = 1,
        saksnummer = sakId,
        barnIdent = barnIdent,
        nyMottakerIdent = nyMottakerIdent,
        overførtTilSkattTidspunkt = overført,
        godkjentAvSkattTidspunkt = godkjent,
    )

    @Test
    fun `skal lagre pending rad og publisere event ved oppretting`() {
        every { persistenceService.lagreEndreMottaker(any()) } returns endreMottaker()

        endreMottakerService.opprettEndreMottaker(1, sakId, barnIdent, nyMottakerIdent)

        verify(exactly = 1) { persistenceService.lagreEndreMottaker(match { it.godkjentAvSkattTidspunkt == null }) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(EndreMottakerOpprettetEvent(id)) }
        verify(exactly = 0) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
    }

    @Test
    fun `skal overfoere og sette alle felt konsistent ved suksess`() {
        val lagret = slot<EndreMottaker>()
        every { persistenceService.hentEndreMottaker(id) } returns endreMottaker()
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
        every { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) } just Runs
        every { persistenceService.lagreEndreMottaker(capture(lagret)) } answers { firstArg() }

        endreMottakerService.overførEndreMottaker(id)

        verify(exactly = 1) {
            bidragReskontroConsumer.endreRmForSak(
                Saksnummer(sakId),
                Personident(barnIdent),
                Personident(nyMottakerIdent),
            )
        }
        lagret.captured.overførtTilSkattTidspunkt shouldNotBe null
        lagret.captured.godkjentAvSkattTidspunkt shouldNotBe null
        lagret.captured.feilmeldingFraSkatt shouldBe null
    }

    @Test
    fun `skal registrere feilmelding og ikke godkjenne ved feil under overfoering`() {
        val lagret = slot<EndreMottaker>()
        every { persistenceService.hentEndreMottaker(id) } returns endreMottaker()
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
        every { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) } throws
            HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
        every { persistenceService.lagreEndreMottaker(capture(lagret)) } answers { firstArg() }

        endreMottakerService.overførEndreMottaker(id)

        lagret.captured.overførtTilSkattTidspunkt shouldNotBe null
        lagret.captured.godkjentAvSkattTidspunkt shouldBe null
        lagret.captured.feilmeldingFraSkatt shouldNotBe null
    }

    @Test
    fun `skal ikke overfoere naar blokkert av vedlikeholdsmodus`() {
        every { persistenceService.hentEndreMottaker(id) } returns endreMottaker()
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns true

        endreMottakerService.overførEndreMottaker(id)

        verify(exactly = 0) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
        verify(exactly = 0) { persistenceService.lagreEndreMottaker(any()) }
    }

    @Test
    fun `skal ikke overfoere naar raden allerede er godkjent`() {
        every { persistenceService.hentEndreMottaker(id) } returns endreMottaker(godkjent = LocalDateTime.now())

        endreMottakerService.overførEndreMottaker(id)

        verify(exactly = 0) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
        verify(exactly = 0) { persistenceService.lagreEndreMottaker(any()) }
    }

    @Test
    fun `skal ikke overfoere naar raden ikke finnes`() {
        every { persistenceService.hentEndreMottaker(id) } returns null

        endreMottakerService.overførEndreMottaker(id)

        verify(exactly = 0) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
        verify(exactly = 0) { persistenceService.lagreEndreMottaker(any()) }
    }
}
