package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.BidragReskontroConsumer
import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

@ExtendWith(MockKExtension::class)
class EndreMottakerServiceTest {

    @MockK
    private lateinit var persistenceService: PersistenceService

    @MockK
    private lateinit var bidragReskontroConsumer: BidragReskontroConsumer

    @MockK
    private lateinit var kravService: KravService

    @InjectMockKs
    private lateinit var endreMottakerService: EndreMottakerService

    private val sakId = "123456"
    private val barnIdent = "11111111111"
    private val nyMottakerIdent = "22222222222"

    @Test
    fun `skal lagre og overfoere endring av mottaker naar overfoering ikke er blokkert`() {
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.lagreEndreMottaker(any()) } answers { firstArg() }
        every { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) } just Runs

        endreMottakerService.opprettOgOverførEndreMottaker(1, sakId, barnIdent, nyMottakerIdent)

        verify(exactly = 1) {
            bidragReskontroConsumer.endreRmForSak(
                Saksnummer(sakId),
                Personident(barnIdent),
                Personident(nyMottakerIdent),
            )
        }
        // Lagres to ganger: en gang ved oppretting og en gang etter overføring.
        verify(exactly = 2) { persistenceService.lagreEndreMottaker(match { it.godkjentAvSkattTidspunkt != null }) }
    }

    @Test
    fun `skal lagre men ikke overfoere naar vedlikeholdsmodus er paa`() {
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns true
        every { persistenceService.lagreEndreMottaker(any()) } answers { firstArg() }

        endreMottakerService.opprettOgOverførEndreMottaker(1, sakId, barnIdent, nyMottakerIdent)

        verify(exactly = 1) { persistenceService.lagreEndreMottaker(any()) }
        verify(exactly = 0) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
    }

    @Test
    fun `skal registrere feilmelding fra skatt naar overfoering feiler`() {
        val lagrede = mutableListOf<EndreMottaker>()
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.lagreEndreMottaker(capture(lagrede)) } answers { firstArg() }
        every { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) } throws
            HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        endreMottakerService.opprettOgOverførEndreMottaker(1, sakId, barnIdent, nyMottakerIdent)

        val sisteLagrede = lagrede.last()
        sisteLagrede.godkjentAvSkattTidspunkt shouldBe null
        sisteLagrede.overførtTilSkattTidspunkt shouldNotBe null
        sisteLagrede.feilmeldingFraSkatt shouldNotBe null
    }

    @Test
    fun `skal resende kun nyeste ikke-godkjente endringer per sak og barn`() {
        val endreMottaker = EndreMottaker(
            id = 1,
            vedtakId = 1,
            saksnummer = sakId,
            barnIdent = barnIdent,
            nyMottakerIdent = nyMottakerIdent,
        )
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns false
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
        every { persistenceService.hentNyesteIkkeGodkjenteEndreMottakerPerSakOgBarn() } returns listOf(endreMottaker)
        every { persistenceService.lagreEndreMottaker(any()) } answers { firstArg() }
        every { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) } just Runs

        endreMottakerService.resendIkkeGodkjenteEndringer()

        verify(exactly = 1) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
        endreMottaker.godkjentAvSkattTidspunkt shouldNotBe null
    }

    @Test
    fun `skal ikke resende naar overfoering er blokkert av driftsavvik`() {
        every { persistenceService.harAktivtDriftsavvik(erInnlesing = false) } returns true

        endreMottakerService.resendIkkeGodkjenteEndringer()

        verify(exactly = 0) { persistenceService.hentNyesteIkkeGodkjenteEndreMottakerPerSakOgBarn() }
        verify(exactly = 0) { bidragReskontroConsumer.endreRmForSak(any(), any(), any()) }
    }
}
