package no.nav.bidrag.regnskap.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.regnskap.consumer.BidragReskontroConsumer
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonDto
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonerDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ReskontroServiceTest {

    @MockK
    private lateinit var bidragReskontroConsumer: BidragReskontroConsumer

    @InjectMockKs
    private lateinit var reskontroService: ReskontroService

    @Test
    fun `skal kaste egen feil om ingen transaksjoner er funnet for saken i reskontro`() {
        val sakId = "12345"
        val vedtakId = 1
        val referansekode = UUID.randomUUID().toString()

        val oppdrag = TestData.opprettOppdrag(sakId = sakId)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, delytelseId = 1001)
        val kontering = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "TK1",
            overforingsperiode = "2025-09",
            konteringId = 321,
        )

        val inputKonteringer = mapOf(referansekode to setOf(kontering))
        every { bidragReskontroConsumer.hentTransaksjonerForSak(sakId) } returns null

        val result = reskontroService.sammenlignOversendteKonteringerMedReskontro(inputKonteringer)

        assertEquals(1, result.size)
        assertTrue(result[referansekode]?.any { it.contains("Det finnes ingen transaksjoner i reskontro for sak: $sakId") } == true)
    }

    @Test
    fun `skal returnere feil om kontering ikke har samme transaksjonskode`() {
        val sakId = "12345"
        val vedtakId = 1
        val referansekode = UUID.randomUUID().toString()

        val oppdrag = TestData.opprettOppdrag(sakId = sakId)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, delytelseId = 1001)
        val kontering = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "G1",
            overforingsperiode = "2025-09",
            konteringId = 321,
        )
        val kontering2 = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "G3",
            overforingsperiode = "2025-09",
            konteringId = 321,
        )

        val transaksjonDto = TransaksjonDto(
            delytelsesid = "1001",
            transaksjonskode = "B1",
            periode = Datoperiode("2025-09-01", "2025-10-01"),
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            beløp = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = null,
            saksnummer = null,
            barn = null,
            søknadstype = null,
        )
        val transaksjonDto2 = TransaksjonDto(
            delytelsesid = "1001",
            transaksjonskode = "B3",
            periode = Datoperiode("2025-09-01", "2025-10-01"),
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            beløp = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = null,
            saksnummer = null,
            barn = null,
            søknadstype = null,
        )
        val transaksjonerDto = TransaksjonerDto(transaksjoner = listOf(transaksjonDto, transaksjonDto2))

        val inputKonteringer = mapOf(referansekode to setOf(kontering, kontering2))
        every { bidragReskontroConsumer.hentTransaksjonerForSak(sakId) } returns transaksjonerDto

        val resultat = reskontroService.sammenlignOversendteKonteringerMedReskontro(inputKonteringer)

        assertEquals(1, resultat.size)
        assertTrue(resultat[referansekode]?.any { it.contains("Fant ikke transaksjon i reskontro for sak: $sakId") } == true)
    }

    @Test
    fun `skal returnere feil om kontering ikke har samme dato`() {
        val sakId = "12345"
        val vedtakId = 1
        val referansekode = UUID.randomUUID().toString()

        val oppdrag = TestData.opprettOppdrag(sakId = sakId)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, delytelseId = 1001)
        val kontering = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "B1",
            overforingsperiode = "2025-08",
            konteringId = 321,
        )

        val transaksjonDto = TransaksjonDto(
            delytelsesid = "1001",
            transaksjonskode = "B1",
            periode = Datoperiode("2025-09-01", "2025-10-01"),
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            beløp = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = null,
            saksnummer = null,
            barn = null,
            søknadstype = null,
        )
        val transaksjonerDto = TransaksjonerDto(transaksjoner = listOf(transaksjonDto))

        val inputKonteringer = mapOf(referansekode to setOf(kontering))
        every { bidragReskontroConsumer.hentTransaksjonerForSak(sakId) } returns transaksjonerDto

        val resultat = reskontroService.sammenlignOversendteKonteringerMedReskontro(inputKonteringer)

        assertEquals(1, resultat.size)
        assertTrue(resultat[referansekode]?.any { it.contains("Fant ikke transaksjon i reskontro for sak: $sakId") } == true)
    }

    @Test
    fun `skal ikke returnere feil feil om kontering ikke har samme dato men det er forskudd`() {
        val sakId = "12345"
        val vedtakId = 1
        val referansekode = UUID.randomUUID().toString()

        val oppdrag = TestData.opprettOppdrag(sakId = sakId)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, delytelseId = 1001)
        val kontering = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "A1",
            overforingsperiode = "2025-08",
            konteringId = 321,
        )

        val transaksjonDto = TransaksjonDto(
            delytelsesid = "1001",
            transaksjonskode = "A1",
            periode = Datoperiode("2025-09-01", "2025-10-01"),
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            beløp = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = null,
            saksnummer = null,
            barn = null,
            søknadstype = null,
        )
        val transaksjonerDto = TransaksjonerDto(transaksjoner = listOf(transaksjonDto))

        val inputKonteringer = mapOf(referansekode to setOf(kontering))
        every { bidragReskontroConsumer.hentTransaksjonerForSak(sakId) } returns transaksjonerDto

        val resultat = reskontroService.sammenlignOversendteKonteringerMedReskontro(inputKonteringer)

        assertEquals(0, resultat.size)
    }

    @Test
    fun `skal returnere feil om kontering ikke har samme delytelsesid`() {
        val sakId = "12345"
        val vedtakId = 1
        val referansekode = UUID.randomUUID().toString()

        val oppdrag = TestData.opprettOppdrag(sakId = sakId)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, delytelseId = 1002)
        val kontering = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "B1",
            overforingsperiode = "2025-09",
            konteringId = 321,
        )

        val transaksjonDto = TransaksjonDto(
            delytelsesid = "1001",
            transaksjonskode = "B1",
            periode = Datoperiode("2025-09-01", "2025-10-01"),
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            beløp = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = null,
            saksnummer = null,
            barn = null,
            søknadstype = null,
        )
        val transaksjonerDto = TransaksjonerDto(transaksjoner = listOf(transaksjonDto))

        val inputKonteringer = mapOf(referansekode to setOf(kontering))
        every { bidragReskontroConsumer.hentTransaksjonerForSak(sakId) } returns transaksjonerDto

        val resultat = reskontroService.sammenlignOversendteKonteringerMedReskontro(inputKonteringer)

        assertEquals(1, resultat.size)
        assertTrue(resultat[referansekode]?.any { it.contains("Fant ikke transaksjon i reskontro for sak: $sakId") } == true)
    }

    @Test
    fun `skal returnere tomt map om alle konteringene finnes blant transaksjonene fra reskontro`() {
        val sakId = "12345"
        val vedtakId = 1
        val referansekode = UUID.randomUUID().toString()

        val oppdrag = TestData.opprettOppdrag(sakId = sakId)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, delytelseId = 1001)
        val kontering = TestData.opprettKontering(
            vedtakId = vedtakId,
            oppdragsperiode = oppdragsperiode,
            transaksjonskode = "B1",
            overforingsperiode = "2025-09",
            konteringId = 321,
        )

        val transaksjonDto = TransaksjonDto(
            delytelsesid = "1001",
            transaksjonskode = "B1",
            periode = Datoperiode("2025-09-01", "2025-10-01"),
            transaksjonsid = null,
            beskrivelse = null,
            dato = null,
            skyldner = null,
            mottaker = null,
            beløp = null,
            restBeløp = null,
            beløpIOpprinneligValuta = null,
            valutakode = null,
            saksnummer = null,
            barn = null,
            søknadstype = null,
        )
        val transaksjonerDto = TransaksjonerDto(transaksjoner = listOf(transaksjonDto))

        val inputKonteringer = mapOf(referansekode to setOf(kontering))
        every { bidragReskontroConsumer.hentTransaksjonerForSak(sakId) } returns transaksjonerDto

        val resultat = reskontroService.sammenlignOversendteKonteringerMedReskontro(inputKonteringer)

        assertTrue(resultat.isEmpty())
    }
}
