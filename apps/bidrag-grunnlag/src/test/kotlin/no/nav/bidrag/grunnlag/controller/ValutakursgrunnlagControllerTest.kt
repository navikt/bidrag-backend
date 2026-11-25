package no.nav.bidrag.grunnlag.controller

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.aop.DefaultExceptionHandler
import no.nav.bidrag.grunnlag.bo.ValutakursgrunnlagBo
import no.nav.bidrag.grunnlag.consumer.TilgangskontrollConsumer
import no.nav.bidrag.grunnlag.hendelse.schedule.valuta.HentValutaScheduler
import no.nav.bidrag.grunnlag.hendelse.schedule.valuta.VALUTAKURS_TIDSSONE
import no.nav.bidrag.grunnlag.hendelse.schedule.valuta.ValutakursKjøring
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
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
    private val hentValutaScheduler = Mockito.mock(HentValutaScheduler::class.java)
    private val tilgangskontrollConsumer = Mockito.mock(TilgangskontrollConsumer::class.java)
    private val controller = ValutakursgrunnlagController(service, historiskeValutakurserService, hentValutaScheduler, tilgangskontrollConsumer)
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
    fun `innhenting avvises uten administrasjonstilgang`() {
        Mockito.doThrow(ResponseStatusException(HttpStatus.FORBIDDEN))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()
        val feil = assertThrows<ResponseStatusException> {
            controller.innhent(InnhentValutakursgrunnlagRequest(Valutakode.USD, dato))
        }

        assertEquals(HttpStatus.FORBIDDEN, feil.statusCode)
        Mockito.verifyNoInteractions(service, historiskeValutakurserService)
    }

    @Test
    fun `planlagt valutainnhenting bruker seneste halvår uten år og måned`() {
        val datoFør = LocalDate.now(VALUTAKURS_TIDSSONE)

        mockMvc.perform(post("/valutakursgrunnlag/planlagt-innhenting"))
            .andExpect(status().isOk)

        val dato = argumentCaptor<LocalDate>()
        Mockito.verify(hentValutaScheduler).hentValutakurs(dato.capture(), eq(ValutakursKjøring.MANUELL))
        assertEquals(
            if (datoFør.monthValue >= 7) LocalDate.of(datoFør.year, 7, 1) else LocalDate.of(datoFør.year, 1, 1),
            dato.firstValue,
        )
    }

    @Test
    fun `planlagt valutainnhenting bruker valgt halvår`() {
        mockMvc.perform(
            post("/valutakursgrunnlag/planlagt-innhenting")
                .param("aar", "2025")
                .param("maaned", "7"),
        )
            .andExpect(status().isOk)

        val rekkefølge = Mockito.inOrder(tilgangskontrollConsumer, hentValutaScheduler)
        rekkefølge.verify(tilgangskontrollConsumer).krevAdministrasjonstilgang()
        rekkefølge.verify(hentValutaScheduler).hentValutakurs(LocalDate.of(2025, 7, 1), ValutakursKjøring.MANUELL)
    }

    @ParameterizedTest
    @ValueSource(ints = [403, 503])
    fun `skal stoppe planlagt innhenting uten periode ved avslag eller feil i tilgangskontroll`(statuskode: Int) {
        Mockito.doThrow(ResponseStatusException(HttpStatus.valueOf(statuskode), "Kunne ikke godkjenne tilgang"))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()

        mockMvc.perform(post("/valutakursgrunnlag/planlagt-innhenting"))
            .andExpect(status().`is`(statuskode))

        Mockito.verifyNoInteractions(hentValutaScheduler, service, historiskeValutakurserService)
    }

    @ParameterizedTest
    @ValueSource(ints = [403, 503])
    fun `skal stoppe planlagt innhenting med valgt periode ved avslag eller feil i tilgangskontroll`(statuskode: Int) {
        Mockito.doThrow(ResponseStatusException(HttpStatus.valueOf(statuskode), "Kunne ikke godkjenne tilgang"))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()

        mockMvc.perform(
            post("/valutakursgrunnlag/planlagt-innhenting")
                .param("aar", "2025")
                .param("maaned", "7"),
        ).andExpect(status().`is`(statuskode))

        Mockito.verifyNoInteractions(hentValutaScheduler, service, historiskeValutakurserService)
    }

    @Test
    fun `planlagt valutainnhenting avviser måned utenfor januar og juli`() {
        mockMvc.perform(
            post("/valutakursgrunnlag/planlagt-innhenting")
                .param("aar", "2025")
                .param("maaned", "6"),
        )
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(hentValutaScheduler)
    }

    @Test
    fun `planlagt valutainnhenting avviser fremtidig halvårsstart`() {
        val nesteÅr = LocalDate.now(VALUTAKURS_TIDSSONE).year + 1

        mockMvc.perform(
            post("/valutakursgrunnlag/planlagt-innhenting")
                .param("aar", "$nesteÅr")
                .param("maaned", "1"),
        )
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(hentValutaScheduler)
    }

    @Test
    fun `planlagt valutainnhenting krever både år og måned`() {
        listOf("aar" to "2025", "maaned" to "7").forEach { (parameter, verdi) ->
            mockMvc.perform(post("/valutakursgrunnlag/planlagt-innhenting").param(parameter, verdi))
                .andExpect(status().isBadRequest)
        }

        Mockito.verifyNoInteractions(hentValutaScheduler)
    }

    @ParameterizedTest
    @ValueSource(ints = [403, 503])
    fun `skal stoppe overstyring ved avslag eller feil i tilgangskontroll`(statuskode: Int) {
        Mockito.doThrow(ResponseStatusException(HttpStatus.valueOf(statuskode), "Kunne ikke godkjenne tilgang"))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()

        val feil = assertThrows<ResponseStatusException> {
            controller.overstyr(1, OverstyrValutakursgrunnlagRequest(BigDecimal("10.5")))
        }

        feil.statusCode.value() shouldBe statuskode
        Mockito.verifyNoInteractions(service, historiskeValutakurserService)
    }

    @ParameterizedTest
    @ValueSource(ints = [403, 503])
    fun `skal returnere tilgangsfeil ved HTTP-overstyring uten sideeffekter`(statuskode: Int) {
        Mockito.doThrow(ResponseStatusException(HttpStatus.valueOf(statuskode), "Kunne ikke godkjenne tilgang"))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()

        mockMvc.perform(
            put("/valutakursgrunnlag/1/kurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"kurs":11}"""),
        ).andExpect(status().`is`(statuskode))

        Mockito.verifyNoInteractions(service, historiskeValutakurserService)
    }

    @Test
    fun `skal tillate overstyring og innhenting for administrator uten lokalt skriveflagg`() {
        val request = InnhentValutakursgrunnlagRequest(Valutakode.USD, LocalDate.of(2025, 7, 1))
        val grunnlag = ValutakursgrunnlagBo(kurs = BigDecimal("10.5"), multiplikator = 0, basisvaluta = Valutakode.USD)
        Mockito.`when`(service.innhent(request.valutakode, request.gyldigFra)).thenReturn(grunnlag)
        Mockito.`when`(service.overstyr(1, BigDecimal("11"))).thenReturn(grunnlag)

        assertSame(grunnlag, controller.innhent(request))
        assertSame(grunnlag, controller.overstyr(1, OverstyrValutakursgrunnlagRequest(BigDecimal("11"))))
        Mockito.verify(tilgangskontrollConsumer, Mockito.times(2)).krevAdministrasjonstilgang()
    }

    @Test
    fun `skal tillate begge innhentinger for administrator uten lokalt skriveflagg`() {
        val grunnlag = ValutakursgrunnlagBo(kurs = BigDecimal("10.5"), multiplikator = 0, basisvaluta = Valutakode.USD)
        val fra = LocalDate.of(2025, 1, 1)
        val til = LocalDate.of(2025, 7, 1)
        Mockito.`when`(service.innhent(Valutakode.USD, fra)).thenReturn(grunnlag)
        Mockito.`when`(historiskeValutakurserService.hentHistoriskeValutakurser(fra, til)).thenReturn(listOf(grunnlag))

        controller.innhent(InnhentValutakursgrunnlagRequest(Valutakode.USD, fra)) shouldBe grunnlag
        controller.innhentHistoriskeValutakursgrunnlag(InnhentHistoriskeRequest(fra, til)) shouldBe listOf(grunnlag)
        Mockito.verify(tilgangskontrollConsumer, Mockito.times(2)).krevAdministrasjonstilgang()
    }

    @ParameterizedTest
    @ValueSource(ints = [403, 503])
    fun `skal stoppe begge innhentinger ved avslag eller feil i tilgangskontroll`(statuskode: Int) {
        Mockito.doThrow(ResponseStatusException(HttpStatus.valueOf(statuskode), "Kunne ikke godkjenne tilgang"))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()

        assertThrows<ResponseStatusException> {
            controller.innhent(InnhentValutakursgrunnlagRequest(Valutakode.USD, LocalDate.of(2025, 1, 1)))
        }.statusCode.value() shouldBe statuskode
        assertThrows<ResponseStatusException> {
            controller.innhentHistoriskeValutakursgrunnlag(InnhentHistoriskeRequest(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 1)))
        }.statusCode.value() shouldBe statuskode
        Mockito.verifyNoInteractions(service, historiskeValutakurserService)
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
    fun `skal returnere 403 for gyldig HTTP-forespørsel uten administrasjonstilgang`() {
        Mockito.doThrow(ResponseStatusException(HttpStatus.FORBIDDEN))
            .`when`(tilgangskontrollConsumer).krevAdministrasjonstilgang()
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
