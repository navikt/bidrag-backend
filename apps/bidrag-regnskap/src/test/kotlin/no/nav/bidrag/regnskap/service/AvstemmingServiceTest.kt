package no.nav.bidrag.regnskap.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.regnskap.fil.avstemning.AvstemmingsfilGenerator
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.regnskap.avstemning.SumPrSakResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class AvstemmingServiceTest {

    private val avstemmingsfilGenerator: AvstemmingsfilGenerator = mockk(relaxed = true)
    private val persistenceService: PersistenceService = mockk()
    private val filoverføringTilElinKlient: FiloverføringTilElinKlient = mockk(relaxed = true)

    private val avstemmingService = AvstemmingService(
        avstemmingsfilGenerator,
        persistenceService,
        filoverføringTilElinKlient,
    )

    @Test
    fun `skal starte avstemming for en gitt dato`() {
        val testDato = LocalDate.now()
        val konteringer = listOf(TestData.opprettKontering(behandlingsstatusOkTidspunkt = LocalDateTime.now()))

        every { persistenceService.hentAlleKonteringerForDato(testDato) } returns konteringer

        avstemmingService.startAvstemming(testDato)

        verify { avstemmingsfilGenerator.skrivAvstemmingsfil(konteringer, testDato) }
    }

    @Test
    fun `skal starte avstemming for dato og tidsintervall`() {
        val testDato = LocalDate.now()
        val fomTidspunkt = LocalDateTime.of(2023, 9, 1, 0, 0)
        val tomTidspunkt = LocalDateTime.of(2023, 9, 30, 23, 59)
        val konteringer = listOf(TestData.opprettKontering(behandlingsstatusOkTidspunkt = LocalDateTime.now()))

        every {
            persistenceService.hentAlleKonteringerForDato(
                testDato,
                fomTidspunkt,
                tomTidspunkt,
            )
        } returns konteringer

        avstemmingService.startAvstemming(testDato, fomTidspunkt, tomTidspunkt)

        verify { avstemmingsfilGenerator.skrivAvstemmingsfil(konteringer, testDato) }
    }

    @Test
    fun `skal starte manuell overføring av avstemmingsfiler`() {
        val testDato = LocalDate.now()
        val formattedDate = testDato.format(DateTimeFormatter.ofPattern("yyMMdd"))
        val avstemmingMappe = "avstemning/"
        val avstemmingKonteringFilnavn = "avstdet_D$formattedDate.xml"
        val avstemmingSummeringFilnavn = "avstsum_D$formattedDate.xml"

        avstemmingService.startManuellOverføringAvstemingTilSftpFraGcpBucket(testDato)

        verify {
            filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemmingMappe, avstemmingKonteringFilnavn)
            filoverføringTilElinKlient.lastOppFilTilFilsluse(avstemmingMappe, avstemmingSummeringFilnavn)
        }
    }

    @Test
    fun `skal hente summering for stønadstype og periode`() {
        val stønadstype = Stønadstype.BIDRAG
        val periode = YearMonth.of(2023, 9)
        val expectedResponse = SumPrSakResponse(emptyList())

        every { persistenceService.hentSakSumForStønadOgMåned(stønadstype, periode) } returns emptyList()

        val response = avstemmingService.hentSumForSaker(stønadstype, periode)

        assertThat(response).isEqualTo(expectedResponse)
        verify { persistenceService.hentSakSumForStønadOgMåned(stønadstype, periode) }
    }
}
