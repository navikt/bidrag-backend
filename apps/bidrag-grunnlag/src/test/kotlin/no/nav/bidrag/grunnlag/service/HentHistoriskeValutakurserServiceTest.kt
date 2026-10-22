package no.nav.bidrag.grunnlag.service

import jakarta.annotation.PostConstruct
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDate

class HentHistoriskeValutakurserServiceTest {
    private val grunnlag = mock<ValutakursgrunnlagService>()
    private val hent = mock<HentValutakursService>()
    private val service = HentHistoriskeValutakurserService(grunnlag, hent)

    @Test
    fun `historisk kursinnhenting starter ikke ved oppstart`() {
        assertTrue(HentHistoriskeValutakurserService::class.java.declaredMethods.none { it.isAnnotationPresent(PostConstruct::class.java) })
    }

    @Test
    fun `januar 2024 til juli 2025 henter nøyaktig tre halvår`() {
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        service.hentHistoriskeValutakurser(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 7, 1))

        val forespørsler = argumentCaptor<HentValutakursRequest>()
        verify(hent, times(3)).hentValutakurs(forespørsler.capture())
        val starter = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1), LocalDate.of(2025, 1, 1))
        assertEquals(starter, forespørsler.allValues.map { it.hentValutakursListe.map { kurs -> kurs.dato }.distinct().single() })
        starter.forEach { dato ->
            verify(grunnlag).opprettValutakursgrunnlag(emptyList(), Datoperiode(dato, dato.plusMonths(6)))
        }
        forespørsler.allValues.forEach { request ->
            val koder = request.hentValutakursListe.map { it.valutakode }
            assertEquals(Valutakode.entries.filter { it != Valutakode.NOK && it.aktiv(request.hentValutakursListe.first().dato) }, koder)
        }
    }

    @Test
    fun `inntil ti halvår tillates for et datointervall`() {
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())
        val fra = LocalDate.of(2019, 7, 1)

        service.hentHistoriskeValutakurser(fra, fra.plusYears(5))

        val forespørsler = argumentCaptor<HentValutakursRequest>()
        verify(hent, times(10)).hentValutakurs(forespørsler.capture())
        assertEquals(
            (0 until 10).map { fra.plusMonths(it * 6L) },
            forespørsler.allValues.map { it.hentValutakursListe.map { kurs -> kurs.dato }.distinct().single() },
        )
    }

    @Test
    fun `elleve halvår og større datointervaller avvises før oppslag og innhenting`() {
        val til = LocalDate.of(2024, 7, 1)
        val starter = listOf(til.minusMonths(66), LocalDate.of(2000, 1, 1))

        starter.forEach { fra ->
            val feil = assertThrows<HttpStatusCodeException> { service.hentHistoriskeValutakurser(fra, til) }

            assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
            assertEquals("Kan hente maksimalt 10 halvårsperioder av gangen", feil.message)
        }
        verifyNoInteractions(hent, grunnlag)
    }

    @Test
    fun `inntil ti halvår tillates ved direkte kall med periodeliste`() {
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())
        val fra = LocalDate.of(2019, 1, 1)
        val perioder = List(10) { indeks ->
            val start = fra.plusMonths(indeks * 6L)
            Datoperiode(start, start.plusMonths(6))
        }

        service.hentHistoriskeValutakurser(perioder)

        verify(hent, times(10)).hentValutakurs(any())
    }

    @Test
    fun `datoer før januar 2000 avvises før datoberegninger og oppslag`() {
        val starter = listOf(LocalDate.MIN, LocalDate.of(1999, 7, 1), LocalDate.of(1999, 12, 31))

        starter.forEach { fra ->
            val feil = assertThrows<HttpStatusCodeException> { service.hentHistoriskeValutakurser(fra, fra.plusMonths(6)) }

            assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
            assertEquals("Fra-dato kan ikke være før 1. januar 2000", feil.message)
        }
        verifyNoInteractions(hent, grunnlag)
    }

    @Test
    fun `periodeliste med eldre dato avvises før noen perioder behandles`() {
        val januar = LocalDate.of(2000, 1, 1)
        val perioder = listOf(
            Datoperiode(januar, januar.plusMonths(6)),
            Datoperiode(LocalDate.MIN, LocalDate.MIN.plusMonths(6)),
        )

        val feil = assertThrows<HttpStatusCodeException> { service.hentHistoriskeValutakurser(perioder) }

        assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
        assertEquals("Fra-dato kan ikke være før 1. januar 2000", feil.message)
        verifyNoInteractions(hent, grunnlag)
    }

    @Test
    fun `januar 2000 tillates som tidligste fra-dato`() {
        val fra = LocalDate.of(2000, 1, 1)
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        service.hentHistoriskeValutakurser(fra, fra.plusMonths(6))

        val request = argumentCaptor<HentValutakursRequest>()
        verify(hent).hentValutakurs(request.capture())
        assertTrue(request.firstValue.hentValutakursListe.isNotEmpty())
        assertTrue(request.firstValue.hentValutakursListe.all { it.dato == fra })
        verify(grunnlag).opprettValutakursgrunnlag(emptyList(), Datoperiode(fra, fra.plusMonths(6)))
    }

    @Test
    fun `elleve halvår avvises ved direkte kall med periodeliste`() {
        val fra = LocalDate.of(2019, 1, 1)
        val perioder = List(11) { indeks ->
            val start = fra.plusMonths(indeks * 6L)
            Datoperiode(start, start.plusMonths(6))
        }

        val feil = assertThrows<HttpStatusCodeException> { service.hentHistoriskeValutakurser(perioder) }

        assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
        assertEquals("Kan hente maksimalt 10 halvårsperioder av gangen", feil.message)
        verifyNoInteractions(hent, grunnlag)
    }

    @Test
    fun `juli til januar henter ett halvår og returnerer lagrede grunnlag`() {
        val fra = LocalDate.of(2024, 7, 1)
        val til = LocalDate.of(2025, 1, 1)
        val kurs = HentetValutakursResultat.FeiledValutakurs(ÅrMånedsperiode("2024-06", "2024-07"), Valutakode.USD, Valutakode.NOK)
        val lagret = Valutakursgrunnlag(basisvaluta = Valutakode.USD, brukFra = fra.atStartOfDay(), brukTil = til.atStartOfDay(), feiletHenting = true)
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(listOf(kurs)))
        whenever(grunnlag.opprettValutakursgrunnlag(listOf(kurs), Datoperiode(fra, til))).thenReturn(listOf(lagret))

        val resultat = service.hentHistoriskeValutakurser(fra, til)

        verify(hent).hentValutakurs(any())
        assertEquals(1, resultat.size)
        assertEquals(fra.atStartOfDay(), resultat.single().brukFra)
        assertEquals(til.atStartOfDay(), resultat.single().brukTil)
        assertTrue(resultat.single().feiletHenting)
    }

    @Test
    fun `inneværende halvår kan hentes selv om sluttdatoen er i fremtiden`() {
        val iDag = LocalDate.of(2026, 10, 6)
        val fra = LocalDate.of(2026, 7, 1)
        val til = LocalDate.of(2027, 1, 1)
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        Mockito.mockStatic(LocalDate::class.java, Mockito.CALLS_REAL_METHODS).use { dato ->
            dato.`when`<LocalDate> { LocalDate.now() }.thenReturn(iDag)

            service.hentHistoriskeValutakurser(fra, til)
        }

        val request = argumentCaptor<HentValutakursRequest>()
        verify(hent).hentValutakurs(request.capture())
        assertTrue(request.firstValue.hentValutakursListe.isNotEmpty())
        assertTrue(request.firstValue.hentValutakursListe.all { it.dato == fra })
        verify(grunnlag).opprettValutakursgrunnlag(emptyList(), Datoperiode(fra, til))
    }

    @Test
    fun `neste halvår kan ikke hentes før det har startet`() {
        val iDag = LocalDate.of(2026, 10, 6)
        val fra = LocalDate.of(2027, 1, 1)
        val til = LocalDate.of(2027, 7, 1)

        Mockito.mockStatic(LocalDate::class.java, Mockito.CALLS_REAL_METHODS).use { dato ->
            dato.`when`<LocalDate> { LocalDate.now() }.thenReturn(iDag)

            val feil = assertThrows<HttpStatusCodeException> { service.hentHistoriskeValutakurser(fra, til) }
            assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
        }
        verifyNoInteractions(hent, grunnlag)
    }

    @Test
    fun `valutaer filtreres etter utgåttdato for hvert halvår`() {
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        service.hentHistoriskeValutakurser(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))

        val forespørsler = argumentCaptor<HentValutakursRequest>()
        verify(hent, times(2)).hentValutakurs(forespørsler.capture())
        assertTrue(forespørsler.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.ANG })
        assertFalse(forespørsler.secondValue.hentValutakursListe.any { it.valutakode == Valutakode.ANG })
        assertTrue(forespørsler.allValues.all { it.hentValutakursListe.any { kurs -> kurs.valutakode == Valutakode.BGN } })
        assertTrue(forespørsler.allValues.all { it.hentValutakursListe.none { kurs -> kurs.valutakode == Valutakode.HRK } })
        verify(grunnlag, never()).hentValutakursgrunnlag(eq(Valutakode.HRK), any())
        verify(grunnlag).hentValutakursgrunnlag(Valutakode.ANG, LocalDate.of(2025, 1, 1))
        verify(grunnlag, never()).hentValutakursgrunnlag(Valutakode.ANG, LocalDate.of(2025, 7, 1))
    }

    @Test
    fun `valuta som utgår på periodestart hentes ikke`() {
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        service.hentHistoriskeValutakurser(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 7, 1))

        val request = argumentCaptor<HentValutakursRequest>()
        verify(hent).hentValutakurs(request.capture())
        assertFalse(request.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.HRK })
        verify(grunnlag, never()).hentValutakursgrunnlag(eq(Valutakode.HRK), any())
    }

    @Test
    fun `BYN hentes først fra halvåret der koden ble innført`() {
        val januar = LocalDate.of(2016, 1, 1)
        val juli = LocalDate.of(2016, 7, 1)
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        service.hentHistoriskeValutakurser(januar, LocalDate.of(2017, 1, 1))

        val forespørsler = argumentCaptor<HentValutakursRequest>()
        verify(hent, times(2)).hentValutakurs(forespørsler.capture())
        assertFalse(forespørsler.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.BYN })
        assertTrue(forespørsler.secondValue.hentValutakursListe.any { it.valutakode == Valutakode.BYN })
        verify(grunnlag, never()).hentValutakursgrunnlag(Valutakode.BYN, januar)
        verify(grunnlag).hentValutakursgrunnlag(Valutakode.BYN, juli)
    }

    @Test
    fun `valuta er aktiv fra og med innføringsdato og til men ikke med utløpsdato`() {
        val bynFra = LocalDate.of(2016, 7, 1)
        assertEquals(bynFra, Valutakode.BYN.gyldigFra)
        assertFalse(Valutakode.BYN.aktiv(bynFra.minusDays(1)))
        assertFalse(Valutakode.BYN.aktiv(LocalDate.MIN))
        assertTrue(Valutakode.BYN.aktiv(bynFra))
        assertTrue(Valutakode.BYN.aktiv(bynFra.plusDays(1)))

        val bgnTil = LocalDate.of(2026, 1, 1)
        assertTrue(Valutakode.BGN.aktiv(bgnTil.minusDays(1)))
        assertFalse(Valutakode.BGN.aktiv(bgnTil))
        assertFalse(Valutakode.BGN.aktiv(bgnTil.plusDays(1)))
        assertTrue(Valutakode.USD.aktiv(LocalDate.of(2016, 1, 1)))
    }

    @Test
    fun `eksisterende grunnlag hentes ikke på nytt`() {
        val fra = LocalDate.of(2024, 1, 1)
        whenever(grunnlag.hentValutakursgrunnlag(any(), eq(fra))).thenReturn(ValutakursgrunnlagBo(basisvaluta = Valutakode.USD, kurs = null, multiplikator = null))

        assertTrue(service.hentHistoriskeValutakurser(fra, fra.plusMonths(6)).isEmpty())

        verifyNoInteractions(hent)
        verify(grunnlag, never()).opprettValutakursgrunnlag(any(), any())
    }

    @Test
    fun `bare valutaer uten grunnlag hentes i en delvis lagret periode`() {
        val fra = LocalDate.of(2024, 1, 1)
        whenever(grunnlag.hentValutakursgrunnlag(eq(Valutakode.USD), eq(fra)))
            .thenReturn(ValutakursgrunnlagBo(basisvaluta = Valutakode.USD, kurs = null, multiplikator = null))
        whenever(hent.hentValutakurs(any())).thenReturn(HentValutakursResponse(emptyList()))
        whenever(grunnlag.opprettValutakursgrunnlag(any(), any())).thenReturn(emptyList())

        service.hentHistoriskeValutakurser(fra, fra.plusMonths(6))

        val request = argumentCaptor<HentValutakursRequest>()
        verify(hent).hentValutakurs(request.capture())
        assertFalse(request.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.USD })
        assertTrue(request.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.EUR })
    }

    @Test
    fun `ugyldige grenser og datorekkefølge avvises før innhenting`() {
        val januar = LocalDate.of(2024, 1, 1)
        val juli = LocalDate.of(2024, 7, 1)
        val ugyldige = listOf(
            januar.plusDays(1) to juli,
            januar.plusMonths(1) to juli,
            januar to juli.plusDays(1),
            januar to juli.minusMonths(1),
            januar to januar,
            juli to januar,
            LocalDate.of(LocalDate.now().year + 1, 1, 1) to LocalDate.of(LocalDate.now().year + 1, 7, 1),
            januar to LocalDate.of(LocalDate.now().year + 2, 1, 1),
        )

        ugyldige.forEach { (fra, til) ->
            val feil = assertThrows<HttpStatusCodeException> { service.hentHistoriskeValutakurser(fra, til) }
            assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
        }
        verifyNoInteractions(hent, grunnlag)
    }

    @Test
    fun `innhentingsfeil propageres uten lagring`() {
        whenever(hent.hentValutakurs(any())).thenThrow(IllegalStateException("Kilde utilgjengelig"))

        assertThrows<IllegalStateException> { service.hentHistoriskeValutakurser(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1)) }
        verify(grunnlag, never()).opprettValutakursgrunnlag(any(), any())
    }
}
