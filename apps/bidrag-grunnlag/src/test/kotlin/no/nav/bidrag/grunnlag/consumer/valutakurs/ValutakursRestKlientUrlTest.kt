package no.nav.bidrag.grunnlag.consumer.valutakurs

import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurser.ECBConsumer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

class ValutakursRestKlientUrlTest {
    private val kursDato = LocalDate.of(2025, 6, 30)

    @Test
    fun `ECB-URL inneholder API-stien kun én gang`() {
        val forventet = URI.create("https://data-api.ecb.europa.eu/service/data/EXR/M.NOK+USD.EUR.SP00.A?startPeriod=2025-06&endPeriod=2025-06")

        for (baseUrl in listOf("https://data-api.ecb.europa.eu", "https://data-api.ecb.europa.eu/service/data/EXR/")) {
            val klient = ECBConsumer(URI.create(baseUrl), RestTemplate(), GrunnlagConsumer())

            assertEquals(forventet, klient.lagECBURI(Frequency.Monthly, listOf("NOK", "USD"), kursDato))
        }
    }

    @Test
    fun `Norges Bank-URL inneholder API-stien kun én gang`() {
        val forventet = URI.create("https://data.norges-bank.no/api/data/EXR/M.USD.NOK.SP?format=sdmx-generic-2.1&startPeriod=2025-06&endPeriod=2025-06")

        for (baseUrl in listOf("https://data.norges-bank.no", "https://data.norges-bank.no/api/data/EXR/")) {
            val klient = NorgesBankValutakursRestKlient(RestClient.create(), baseUrl)

            assertEquals(forventet, klient.lagNorgesBankURI(Frekvens.MÅNEDLIG, "USD", kursDato))
        }
    }
}
