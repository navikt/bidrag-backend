package no.nav.bidrag.grunnlag.controller

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.service.Valutaberegning
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.mock.env.MockEnvironment
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursgrunnlagControllerTest {
    private val service = Mockito.mock(ValutakursgrunnlagService::class.java)
    private val controller = ValutakursgrunnlagController(service, MockEnvironment())
    private val dato = LocalDate.of(2026, 7, 1)

    @Test
    fun `henter gyldig kursgrunnlag`() {
        val grunnlag = ValutakursgrunnlagBo(
            kurs = BigDecimal("10.5"),
            multiplikator = 0,
            basisvaluta = Valutakode.USD,
        )
        Mockito.`when`(service.hentValutakursgrunnlag(Valutakode.USD, dato)).thenReturn(grunnlag)

        assertSame(grunnlag, controller.hent(Valutakode.USD, dato))
    }

    @Test
    fun `manglende kursgrunnlag gir 404`() {
        val feil = assertThrows<ResponseStatusException> { controller.hent(Valutakode.USD, dato) }

        assertEquals(HttpStatus.NOT_FOUND, feil.statusCode)
    }

    @Test
    fun `feilet henting kan ikke brukes som kurs`() {
        Mockito.`when`(service.hentValutakursgrunnlag(Valutakode.USD, dato))
            .thenReturn(ValutakursgrunnlagBo(kurs = null, multiplikator = null, basisvaluta = Valutakode.USD, feiletHenting = true))

        val feil = assertThrows<ResponseStatusException> { controller.hent(Valutakode.USD, dato) }

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, feil.statusCode)
    }

    @Test
    fun `feilede kursgrunnlag kan listes`() {
        val pageable = PageRequest.of(0, 50)
        val feilGrunnlag = ValutakursgrunnlagBo(kurs = null, multiplikator = null, basisvaluta = Valutakode.USD, feiletHenting = true)
        Mockito.`when`(service.hentFeiledeValutakursgrunnlag(pageable)).thenReturn(PageImpl(listOf(feilGrunnlag)))

        assertEquals(listOf(feilGrunnlag), controller.hentFeilede(pageable).content)
    }

    @Test
    fun `feilliste avviser for stor side`() {
        val feil = assertThrows<ResponseStatusException> { controller.hentFeilede(PageRequest.of(0, 101)) }

        assertEquals(HttpStatus.BAD_REQUEST, feil.statusCode)
    }

    @Test
    fun `innhenting avvises inntil rettighet er avklart`() {
        val feil = assertThrows<ResponseStatusException> {
            controller.innhent(InnhentValutakursgrunnlagRequest(Valutakode.USD, dato))
        }

        assertEquals(HttpStatus.FORBIDDEN, feil.statusCode)
    }

    @Test
    fun `overstyring avvises inntil rettighet er avklart`() {
        val feil = assertThrows<ResponseStatusException> {
            controller.overstyr(1, OverstyrValutakursgrunnlagRequest(BigDecimal("10.5")))
        }

        assertEquals(HttpStatus.FORBIDDEN, feil.statusCode)
    }

    @Test
    fun `testflagget alene gir ikke skrivetilgang uten local-profil`() {
        val testController = ValutakursgrunnlagController(service, MockEnvironment(), true)

        val feil = assertThrows<ResponseStatusException> {
            testController.overstyr(1, OverstyrValutakursgrunnlagRequest(BigDecimal("10.5")))
        }

        assertEquals(HttpStatus.FORBIDDEN, feil.statusCode)
    }

    @Test
    fun `lokal innhenting og overstyring kan testes med eksplisitt aktivering`() {
        val lokal = ValutakursgrunnlagController(service, MockEnvironment().apply { setActiveProfiles("local") }, true)
        val request = InnhentValutakursgrunnlagRequest(Valutakode.USD, LocalDate.of(2025, 7, 1))
        val grunnlag = ValutakursgrunnlagBo(kurs = BigDecimal("10.5"), multiplikator = 0, basisvaluta = Valutakode.USD)
        Mockito.`when`(service.innhent(request.valutakode, request.gyldigFra)).thenReturn(grunnlag)
        Mockito.`when`(service.overstyr(1, BigDecimal("11"))).thenReturn(grunnlag)

        assertSame(grunnlag, lokal.innhent(request))
        assertSame(grunnlag, lokal.overstyr(1, OverstyrValutakursgrunnlagRequest(BigDecimal("11"))))
    }

    @Test
    fun `kalkulator returnerer beregning`() {
        val request = BeregnValutaRequest(BigDecimal("100"), Valutakode.USD, Valutakode.NOK, dato)
        val svar = Valutaberegning(BigDecimal("1050.0000"), BigDecimal("10.5"), null, Valutakode.USD, Valutakode.NOK, dato)
        Mockito.`when`(service.beregn(request.beløp, request.fraValuta, request.tilValuta, request.dato)).thenReturn(svar)

        assertSame(svar, controller.beregn(request))
    }
}
