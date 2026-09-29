package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.ecb.ECBService
import no.nav.bidrag.grunnlag.consumer.ecb.ECBServiceException
import no.nav.bidrag.grunnlag.consumer.valutakurs.NorgesBankValutakursRestKlient
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.IngenValutakursException
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate

class HentValutakursServiceMockTest {
    private val ecb = Mockito.mock(ECBService::class.java)
    private val norgesBank = Mockito.mock(NorgesBankValutakursRestKlient::class.java)
    private val service = HentValutakursService(ecb, norgesBank)
    private val januar = LocalDate.of(2025, 1, 1)
    private val desember = LocalDate.of(2024, 12, 31)

    @Test
    fun `desembers ECB-kurs brukes fra januar med én enhet valuta`() {
        Mockito.`when`(ecb.hentValutakurs("USD", desember)).thenReturn(Valutakurs("USD", BigDecimal("10.50"), desember))

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
        Mockito.`when`(ecb.hentValutakurs("USD", desember)).thenThrow(ECBServiceException("Mangler kurs"))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember))
            .thenReturn(Valutakurs("USD", BigDecimal("10.75"), desember))

        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD)))).hentetValutakursListe.single()

        val kurs = assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, resultat)
        assertEquals(BigDecimal("10.75"), kurs.valutakursSnitt)
        assertEquals(ValutakursgrunnlagKilde.NORGES_BANK, kurs.kilde)
    }

    @Test
    fun `feilet kurs bevares i respons når begge kilder svikter`() {
        Mockito.`when`(ecb.hentValutakurs("USD", desember)).thenThrow(ECBServiceException("Mangler kurs"))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", desember))
            .thenThrow(IngenValutakursException("Mangler kurs", null))

        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.USD)))).hentetValutakursListe.single()

        assertInstanceOf(HentetValutakursResultat.FeiledValutakurs::class.java, resultat)
    }

    @Test
    fun `hver valuta og hvert halvår hentes med sin observasjonsmåned`() {
        val juli = LocalDate.of(2025, 7, 1)
        val juni = LocalDate.of(2025, 6, 30)
        Mockito.`when`(ecb.hentValutakurs("EUR", desember)).thenReturn(Valutakurs("EUR", BigDecimal("11"), desember))
        Mockito.`when`(ecb.hentValutakurs("USD", juni)).thenReturn(Valutakurs("USD", BigDecimal("10"), juni))

        val resultater = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(januar, Valutakode.EUR), HentValutakurs(juli, Valutakode.USD)))).hentetValutakursListe

        assertEquals(2, resultater.size)
        assertEquals(ÅrMånedsperiode("2025-06", "2025-07"), assertInstanceOf(HentetValutakursResultat.HentetValutakurs::class.java, resultater[1]).periode)
        Mockito.verify(ecb).hentValutakurs("EUR", desember)
        Mockito.verify(ecb).hentValutakurs("USD", juni)
    }

    @Test
    fun `utgått valuta får feilresultat uten eksternt kall`() {
        val resultat = service.hentValutakurs(HentValutakursRequest(listOf(HentValutakurs(LocalDate.of(2026, 1, 1), Valutakode.BGN)))).hentetValutakursListe.single()

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
