package no.nav.bidrag.kodeverk.controller

import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.service.KodeverkKoderBetydningerResponse
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.kodeverk.SpringTestRunner
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus

class KodeverkControllerTest : SpringTestRunner() {
    @Test
    fun `Skal hente kodeverk`() {
        val httpEntity =
            HttpEntity(
                Personident("22496818540"),
            )
        stubUtils.stubKodeverkResponse(KodeverkKoderBetydningerResponse(mapOf("test" to listOf())))

        val response =
            httpHeaderTestRestTemplate
                .getForEntity<KodeverkKoderBetydningerResponse>("${rootUri()}/kodeverk/test")

        response.statusCode shouldBe HttpStatus.OK
    }
}
