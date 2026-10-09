package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.ecb.ECBService
import no.nav.bidrag.grunnlag.consumer.ecb.ECBServiceException
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurser.NorgesBankConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.consumer.valutakurser.norgesBankSvar
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

class HentValutakursServiceMockTest {
    private val ecb = Mockito.mock(ECBService::class.java)
    private val norgesBank = Mockito.mock(NorgesBankConsumer::class.java)
    private val service = HentValutakursService(ecb, norgesBank)
    private val januar = LocalDate.of(2025, 1, 1)
    private val desember = LocalDate.of(2024, 12, 31)

    @Test
    fun `desembers ECB-kurs brukes fra januar med én enhet valuta`() {
        Mockito.`when`(ecb.hentValutakurser(listOf("USD"), desember)).thenReturn(mapOf("USD" to Valutakurs("USD", BigDecimal("10.50"), desember)))

        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD)))).hentetValutakursListe.single()

        val kurs = assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, resultat)
        assertEquals(ÅrMånedsperiode("2024-12", "2025-01"), kurs.periode)
        assertEquals(BigDecimal("10.50"), kurs.valutakursSnitt)
        assertEquals(0, kurs.multiplikator)
        assertEquals(ValutakursgrunnlagKilde.ECB, kurs.kilde)
        Mockito.verifyNoInteractions(norgesBank)
    }

    @Test
    fun `Norges Bank brukes når ECB mangler kurs`() {
        Mockito.`when`(ecb.hentValutakurser(listOf("USD"), desember)).thenThrow(ECBServiceException("Mangler kurs"))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember))
            .thenReturn(RestResponse.Success(norgesBankSvar(valuta = "USD", periode = "2024-12", kurs = "1075", multiplikator = "2")))

        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD)))).hentetValutakursListe.single()

        val kurs = assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, resultat)
        assertEquals(BigDecimal("10.75"), kurs.valutakursSnitt)
        assertEquals(ValutakursgrunnlagKilde.NORGES_BANK, kurs.kilde)
    }

    @Test
    fun `feilet kurs bevares i respons når begge kilder svikter`() {
        Mockito.`when`(ecb.hentValutakurser(listOf("USD"), desember)).thenThrow(ECBServiceException("Mangler kurs"))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember))
            .thenReturn(RestResponse.Success(norgesBankSvar(valuta = "EUR")))

        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD)))).hentetValutakursListe.single()

        assertInstanceOf(HentetValutakursResultat.FeiledValutakurs::class.java, resultat)
    }

    @Test
    fun `HTTP-feil fra Norges Bank blir feilresultat når ECB mangler kurs`() {
        Mockito.`when`(ecb.hentValutakurser(listOf("USD"), desember)).thenThrow(ECBServiceException("Mangler kurs"))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember))
            .thenReturn(RestResponse.Failure("Mangler kurs", HttpStatus.NOT_FOUND, IllegalStateException("Mangler kurs")))

        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD)))).hentetValutakursListe.single()

        assertInstanceOf(HentetValutakursResultat.FeiledValutakurs::class.java, resultat)
    }

    @Test
    fun `hver valuta og hvert halvår hentes med sin observasjonsmåned`() {
        val juli = LocalDate.of(2025, 7, 1)
        val juni = LocalDate.of(2025, 6, 30)
        Mockito.`when`(ecb.hentValutakurser(listOf("EUR"), desember)).thenReturn(mapOf("EUR" to Valutakurs("EUR", BigDecimal("11"), desember)))
        Mockito.`when`(ecb.hentValutakurser(listOf("USD"), juni)).thenReturn(mapOf("USD" to Valutakurs("USD", BigDecimal("10"), juni)))

        val resultater = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.EUR), HentValutakurs(juli, Valutakode.USD)))).hentetValutakursListe

        assertEquals(2, resultater.size)
        assertEquals(ÅrMånedsperiode("2025-06", "2025-07"), assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, resultater[1]).periode)
        Mockito.verify(ecb).hentValutakurser(listOf("EUR"), desember)
        Mockito.verify(ecb).hentValutakurser(listOf("USD"), juni)
        Mockito.verifyNoMoreInteractions(ecb)
    }

    @Test
    fun `valutaer grupperes per observasjonsmåned og bare manglende kurs hentes fra Norges Bank`() {
        val juli = LocalDate.of(2025, 7, 1)
        val juni = LocalDate.of(2025, 6, 30)
        Mockito.`when`(ecb.hentValutakurser(listOf("USD", "EUR", "DKK"), desember))
            .thenReturn(mapOf("USD" to Valutakurs("USD", BigDecimal("10"), desember), "EUR" to Valutakurs("EUR", BigDecimal("11"), desember)))
        Mockito.`when`(ecb.hentValutakurser(listOf("USD"), juni))
            .thenReturn(mapOf("USD" to Valutakurs("USD", BigDecimal("12"), juni)))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "DKK", desember))
            .thenReturn(RestResponse.Success(norgesBankSvar(periode = "2024-12")))
        val forespørsler = listOf(
            HentValutakurs(januar, Valutakode.USD),
            HentValutakurs(juli, Valutakode.USD),
            HentValutakurs(januar, Valutakode.EUR),
            HentValutakurs(januar, Valutakode.DKK),
            HentValutakurs(januar, Valutakode.USD),
        )

        val resultat = service.hentValutakurs(HentValutakursRequest(forespørsler)).hentetValutakursListe
            .map { assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, it) }

        assertEquals(forespørsler.map { it.valutakode }, resultat.map { it.basisvaluta })
        assertEquals(listOf(BigDecimal("10"), BigDecimal("12"), BigDecimal("11"), BigDecimal("1.5534"), BigDecimal("10")), resultat.map { it.valutakursSnitt })
        assertEquals(ValutakursgrunnlagKilde.NORGES_BANK, resultat[3].kilde)
        Mockito.verify(ecb).hentValutakurser(listOf("USD", "EUR", "DKK"), desember)
        Mockito.verify(ecb).hentValutakurser(listOf("USD"), juni)
        Mockito.verify(norgesBank).hentValutakurs(Frekvens.MÅNEDLIG, "DKK", desember)
        Mockito.verifyNoMoreInteractions(ecb, norgesBank)
    }

    @Test
    fun `ti halvår med flere valutaer gir ti ECB-kall`() {
        val forespørsler = (0 until 10).flatMap { halvår ->
            val dato = LocalDate.of(2020, 1, 1).plusMonths(halvår * 6L)
            val sisteDag = dato.minusMonths(1).withDayOfMonth(dato.minusMonths(1).lengthOfMonth())
            Mockito.`when`(ecb.hentValutakurser(listOf("USD", "EUR"), sisteDag))
                .thenReturn(
                    mapOf("USD" to Valutakurs("USD", BigDecimal.TEN, sisteDag), "EUR" to Valutakurs("EUR", BigDecimal.TEN, sisteDag)),
                )
            listOf(HentValutakurs(dato, Valutakode.USD), HentValutakurs(dato, Valutakode.EUR))
        }

        assertEquals(20, service.hentValutakurs(HentValutakursRequest(forespørsler)).hentetValutakursListe.size)

        assertEquals(10, Mockito.mockingDetails(ecb).invocations.size)
        Mockito.verifyNoInteractions(norgesBank)
    }

    @Test
    fun `feilet ECB-batch bruker Norges Bank for alle aktive valutaer uten nye ECB-kall`() {
        Mockito.`when`(ecb.hentValutakurser(listOf("USD", "DKK"), desember)).thenThrow(ECBServiceException("Mangler NOK-kurs"))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember))
            .thenReturn(RestResponse.Success(norgesBankSvar(valuta = "USD", periode = "2024-12")))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "DKK", desember))
            .thenReturn(RestResponse.Success(norgesBankSvar(periode = "2024-12")))

        val resultat = service.hentValutakurs(
            HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD), HentValutakurs(januar, Valutakode.DKK))),
        ).hentetValutakursListe

        assertEquals(
            listOf(ValutakursgrunnlagKilde.NORGES_BANK, ValutakursgrunnlagKilde.NORGES_BANK),
            resultat.map { assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, it).kilde },
        )
        Mockito.verify(ecb).hentValutakurser(listOf("USD", "DKK"), desember)
        Mockito.verify(norgesBank).hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember)
        Mockito.verify(norgesBank).hentValutakurs(Frekvens.MÅNEDLIG, "DKK", desember)
        Mockito.verifyNoMoreInteractions(ecb, norgesBank)
    }

    @Test
    fun `utgått valuta får feilresultat uten eksternt kall`() {
        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(LocalDate.of(2026, 1, 1), Valutakode.BGN)))).hentetValutakursListe.single()

        assertInstanceOf(HentetValutakursResultat.FeiledValutakurs::class.java, resultat)
        Mockito.verifyNoInteractions(ecb, norgesBank)
    }

    @Test
    fun `valuta før innføringsdato får feilresultat uten eksternt kall`() {
        val resultat = service.hentValutakurs(
            HentValutakursRequest(listOf(HentValutakurs(LocalDate.of(2016, 1, 1), Valutakode.BYN))),
        ).hentetValutakursListe.single()

        assertInstanceOf(HentetValutakursResultat.FeiledValutakurs::class.java, resultat)
        Mockito.verifyNoInteractions(ecb, norgesBank)
    }

    @Test
    fun `ugyldig halvårsdato og tom forespørsel avvises`() {
        assertThrows<IllegalArgumentException> { service.hentValutakurs(HentValutakursRequest(emptyList())) }
        assertThrows<IllegalArgumentException> {
            service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(LocalDate.of(2025, 3, 1), Valutakode.USD))))
        }
    }
}
