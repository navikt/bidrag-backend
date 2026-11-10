package no.nav.bidrag.grunnlag.controller

import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.aop.DefaultExceptionHandler
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.service.HentHistoriskeValutakurserService
import no.nav.bidrag.grunnlag.service.Valutaberegning
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.env.MockEnvironment
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursgrunnlagControllerTest {
    private val service = Mockito.mock(ValutakursgrunnlagService::class.java)
    private val historiskeValutakurserService = Mockito.mock(HentHistoriskeValutakurserService::class.java)
    private val controller = ValutakursgrunnlagController(service, historiskeValutakurserService, MockEnvironment())
    private val mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(DefaultExceptionHandler())
        .build()
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

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, feil.statusCode)
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
        val testController = ValutakursgrunnlagController(service, historiskeValutakurserService, MockEnvironment(), true)

        val feil = assertThrows<ResponseStatusException> {
            testController.overstyr(1, OverstyrValutakursgrunnlagRequest(BigDecimal("10.5")))
        }

        assertEquals(HttpStatus.FORBIDDEN, feil.statusCode)
    }

    @Test
    fun `lokal innhenting og overstyring kan testes med eksplisitt aktivering`() {
        val lokal = ValutakursgrunnlagController(service, historiskeValutakurserService, MockEnvironment().apply { setActiveProfiles("local") }, true)
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

    @Test
    fun `skal returnere 400 uten brukerinput ved ukjent valuta i JSON`() {
        sjekkBindingsfeil<HttpMessageNotReadableException>(
            post("/valutakursgrunnlag/innhentinger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"valutakode":"UKJENT_VALUTA","gyldigFra":"2025-07-01"}"""),
        )
    }

    @Test
    fun `skal returnere 400 når påkrevde JSON-felt mangler eller er null`() {
        listOf(
            """{"gyldigFra":"2025-07-01"}""",
            """{"valutakode":"USD"}""",
            """{"valutakode":null,"gyldigFra":"2025-07-01"}""",
            """{"valutakode":"USD","gyldigFra":null}""",
        ).forEach { json ->
            sjekkBindingsfeil<HttpMessageNotReadableException>(
                post("/valutakursgrunnlag/innhentinger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json),
            )
        }
    }

    @Test
    fun `skal returnere 400 uten brukerinput ved ugyldig dato i JSON`() {
        sjekkBindingsfeil<HttpMessageNotReadableException>(
            post("/valutakursgrunnlag/innhentinger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"valutakode":"USD","gyldigFra":"ugyldig-dato"}"""),
        )
    }

    @Test
    fun `skal returnere 400 ved ugyldig JSON eller manglende body`() {
        listOf("""{"valutakode":""", "").forEach { json ->
            sjekkBindingsfeil<HttpMessageNotReadableException>(
                post("/valutakursgrunnlag/innhentinger")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json),
            )
        }
    }

    @Test
    fun `skal returnere 400 uten brukerinput ved ukjent valuta i query`() {
        sjekkBindingsfeil<MethodArgumentTypeMismatchException>(
            get("/valutakursgrunnlag").param("valutakode", "UKJENT_VALUTA").param("dato", "2025-07-01"),
        )
    }

    @Test
    fun `skal returnere 400 uten brukerinput ved ugyldig dato i query`() {
        sjekkBindingsfeil<MethodArgumentTypeMismatchException>(
            get("/valutakursgrunnlag").param("valutakode", "USD").param("dato", "ugyldig-dato"),
        )
    }

    @Test
    fun `skal returnere 400 uten brukerinput når dato mangler i query`() {
        sjekkBindingsfeil<UnsatisfiedServletRequestParameterException>(
            get("/valutakursgrunnlag").param("valutakode", "USD"),
        )
    }

    @Test
    fun `skal returnere 400 uten brukerinput når valutakode mangler i query`() {
        sjekkBindingsfeil<UnsatisfiedServletRequestParameterException>(
            get("/valutakursgrunnlag").param("dato", "2025-07-01"),
        )
    }

    @Test
    fun `skal returnere 400 når alle queryparametere mangler`() {
        sjekkBindingsfeil<UnsatisfiedServletRequestParameterException>(get("/valutakursgrunnlag"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["HENTET", "OVERSTYRT", "UKJENT_STATUS"])
    fun `skal returnere 400 uten brukerinput ved status som ikke støttes`(status: String) {
        sjekkBindingsfeil<UnsatisfiedServletRequestParameterException>(
            get("/valutakursgrunnlag").param("status", status),
        )
    }

    @Test
    fun `skal returnere 400 uten brukerinput ved ugyldig ID i path`() {
        sjekkBindingsfeil<MethodArgumentTypeMismatchException>(
            put("/valutakursgrunnlag/ugyldig-id/kurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"kurs":10.5}"""),
        )
    }

    @Test
    fun `skal beholde 404 ved HTTP-oppslag av manglende kursgrunnlag`() {
        mockMvc.perform(get("/valutakursgrunnlag").param("valutakode", "USD").param("dato", "2025-07-01"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `skal beholde 403 for gyldig HTTP-forespørsel uten lokal skrivetilgang`() {
        mockMvc.perform(
            post("/valutakursgrunnlag/innhentinger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"valutakode":"USD","gyldigFra":"2025-07-01"}"""),
        ).andExpect(status().isForbidden)
    }

    private inline fun <reified T : Exception> sjekkBindingsfeil(request: MockHttpServletRequestBuilder) {
        val resultat = mockMvc.perform(request)
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Ugyldig forespørsel"))
            .andExpect(header().string(HttpHeaders.WARNING, "Ugyldig forespørsel"))
            .andReturn()

        resultat.resolvedException.shouldBeInstanceOf<T>()
        Mockito.verifyNoInteractions(service, historiskeValutakurserService)
    }
}
