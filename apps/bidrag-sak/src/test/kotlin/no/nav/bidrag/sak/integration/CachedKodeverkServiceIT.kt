package no.nav.bidrag.sak.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.integration.kodeverk.KodeverkClient
import no.nav.bidrag.sak.integration.kodeverk.dto.BeskrivelseDto
import no.nav.bidrag.sak.integration.kodeverk.dto.BetydningDto
import no.nav.bidrag.sak.integration.kodeverk.dto.KodeverkDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.RestTemplateBuilder
import java.net.URI
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CachedKodeverkServiceIT : SpringTestRunner() {
    lateinit var wiremockServer: WireMockServer

    private lateinit var cachedKodeverkService: CachedKodeverkService

    @BeforeAll
    fun initClass() {
        wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServer.start()
        val kodeverkClient = KodeverkClient(URI.create(wiremockServer.baseUrl()), RestTemplateBuilder())
        cachedKodeverkService = CachedKodeverkService(kodeverkClient)
    }

    @AfterAll
    fun tearDown() {
        wiremockServer.stop()
    }

    @Test
    fun hentPostnummer() {
        val beskrivelseFoo = BeskrivelseDto("Foo", "")
        val betydningFoo = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf("nb" to beskrivelseFoo))
        val beskrivelseBar = BeskrivelseDto("Bar", "")
        val betydningBar = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf("nb" to beskrivelseBar))
        val kodeverk = KodeverkDto(mapOf("FOO" to listOf(betydningFoo), "BAR" to listOf(betydningBar)))

        wiremockServer.stubFor(
            WireMock.get(KODEVERK_POSTNUMMER).willReturn(
                WireMock
                    .aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(commonObjectmapper.writeValueAsString(kodeverk)),
            ),
        )

        val postnummer = cachedKodeverkService.hentPostnummer()

        postnummer shouldBe mapOf("FOO" to "Foo", "BAR" to "Bar")
    }

    @Test
    fun hentLandkoder() {
        val beskrivelseFoo = BeskrivelseDto("Foo", "")
        val betydningFoo = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf("nb" to beskrivelseFoo))
        val beskrivelseBar = BeskrivelseDto("Bar", "")
        val betydningBar = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf("nb" to beskrivelseBar))
        val kodeverk = KodeverkDto(mapOf("FOO" to listOf(betydningFoo), "BAR" to listOf(betydningBar)))

        wiremockServer.stubFor(
            WireMock.get(KODEVERK_LANDKODER).willReturn(
                WireMock
                    .aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(commonObjectmapper.writeValueAsString(kodeverk)),
            ),
        )

        val landkoder = cachedKodeverkService.hentLandkoder()

        landkoder shouldBe mapOf(Landkode("FOO") to "Foo", Landkode("BAR") to "Bar")
    }

    @Test
    fun hentLandkoderISO2() {
        val beskrivelseFoo = BeskrivelseDto("Foo", "")
        val betydningFoo = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf("nb" to beskrivelseFoo))
        val beskrivelseBar = BeskrivelseDto("Bar", "")
        val betydningBar = BetydningDto(LocalDate.now(), LocalDate.now(), mapOf("nb" to beskrivelseBar))
        val kodeverk = KodeverkDto(mapOf("FOO" to listOf(betydningFoo), "BAR" to listOf(betydningBar)))

        wiremockServer.stubFor(
            WireMock.get(KODEVERK_LANDKODER_ISO2).willReturn(
                WireMock
                    .aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(commonObjectmapper.writeValueAsString(kodeverk)),
            ),
        )

        val landkoderISO2 = cachedKodeverkService.hentLandkoderISO2()

        landkoderISO2 shouldBe mapOf(Landkode("FOO") to "Foo", Landkode("BAR") to "Bar")
    }

    companion object {
        private const val KODEVERK_POSTNUMMER =
            "/kodeverk/Postnummer"
        private const val KODEVERK_LANDKODER =
            "/kodeverk/Landkoder"
        private const val KODEVERK_LANDKODER_ISO2 =
            "/kodeverk/LandkoderISO2"
    }
}
