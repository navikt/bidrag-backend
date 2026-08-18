package no.nav.bidrag.person.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.util.DefaultUriBuilderFactory
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdlConsumerTest {
    private lateinit var pdlConsumer: PDLConsumer
    private lateinit var wiremockServer: WireMockServer

    @BeforeAll
    fun initClass() {
        wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServer.start()

        pdlConsumer =
            PDLConsumer(
                HttpHeaderRestTemplate().apply {
                    uriTemplateHandler = DefaultUriBuilderFactory(wiremockServer.baseUrl())
                },
            )
    }

    @AfterAll
    fun tearDown() {
        wiremockServer.stop()
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @Test
    fun `henthentFødselsdatoer returnerer map med identer og fødselsdatoer fra pdl`() {
        wiremockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .willReturn(WireMock.okJson(readFile("pd_personFødselsdatoResponse.json"))),
        )

        val hentFødselsdatoer = pdlConsumer.hentFødselsdatoer(setOf(Personident("1"), Personident("2"), Personident("3")))

        hentFødselsdatoer.size shouldBe 3
        hentFødselsdatoer.values.find { it.tilFødselsdato() == LocalDate.of(1984, 8, 25) } shouldNotBe null
        hentFødselsdatoer.values.find { it.tilFødselsdato() == LocalDate.of(1986, 11, 9) } shouldNotBe null
        hentFødselsdatoer.values.find { it.tilFødselsdato() == LocalDate.of(1977, 7, 18) } shouldNotBe null
    }

    @Test
    fun `henthentGraderinger returnerer map med identer og graderinger fra pdl`() {
        wiremockServer.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .willReturn(WireMock.okJson(readFile("pd_personGraderingResponse.json"))),
        )

        val hentGraderinger = pdlConsumer.hentGraderinger(setOf(Personident("1"), Personident("2"), Personident("3")))

        hentGraderinger.size shouldBe 4
        hentGraderinger.values.find { it.tilGradering() == Gradering.FORTROLIG }
        hentGraderinger.values.find { it.tilGradering() == Gradering.UGRADERT }
        hentGraderinger.values.find { it.tilGradering() == Gradering.STRENGT_FORTROLIG }
        hentGraderinger.values.find { it.tilGradering() == Gradering.STRENGT_FORTROLIG_UTLAND }
    }

    private fun readFile(filnavn: String): String {
        println("__files/pdl/$filnavn")
        return this::class.java.getResource("/__files/pdl/$filnavn").readText()
    }
}
