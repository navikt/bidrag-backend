package no.nav.bidrag.grunnlag.consumer.ecb

import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurser.ECBConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDataSet
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimension
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSeries
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxValue
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.toExchangeRates
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.math.BigDecimal
import java.time.LocalDate

class ECBServiceMaanedskursTest {
    private val klient = Mockito.mock(ECBConsumer::class.java)
    private val service = ECBService(klient)
    private val sisteJuni = LocalDate.of(2025, 6, 30)

    @Test
    fun `månedssvar fra ECB leses som juni og ikke juli`() {
        val json = """
            {
              "dataSets": [{"series": {
                "0:0": {"observations": {"0": [11.584133333333332, 0, null]}},
                "1:0": {"observations": {"0": [1.1518, 0, null]}}
              }}],
              "structure": {"dimensions": {
                "series": [
                  {"id": "CURRENCY", "values": [{"id": "NOK"}, {"id": "USD"}]},
                  {"id": "FREQ", "values": [{"id": "M"}]}
                ],
                "observation": [{"id": "TIME_PERIOD", "values": [{"id": "2025-06"}]}]
              }}
            }
        """.trimIndent()

        val data = JsonMapper.builder().addModule(KotlinModule.Builder().build()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()
            .readValue(json, SdmxData::class.java)
        val kurser = data.toExchangeRates()

        assertEquals(listOf("NOK", "USD"), kurser.map { it.valuta })
        assertEquals(sisteJuni, kurser.first().kursDato)
        assertEquals(BigDecimal("11.584133333333332"), kurser.first().kurs)
    }

    @Test
    fun `ECB krysskurs beregnes som NOK per USD`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(respons("NOK" to "11.584133333333332", "USD" to "1.1518"))
        val kurs = service.hentValutakurs("USD", sisteJuni)

        assertEquals(BigDecimal("10.0574173757"), kurs.kurs)
        assertEquals("USD", kurs.valuta)
        assertEquals(sisteJuni, kurs.kursDato)
    }

    @Test
    fun `EUR bruker bare NOK-serien`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK"), sisteJuni))
            .thenReturn(respons("NOK" to "11.5841"))
        assertEquals(BigDecimal("11.5841"), service.hentValutakurs("EUR", sisteJuni).kurs)
    }

    @Test
    fun `ECB-kurs hentes på nytt ved neste forespørsel`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK"), sisteJuni))
            .thenReturn(
                respons("NOK" to "11.5"),
                respons("NOK" to "11.6"),
            )

        assertEquals(BigDecimal("11.5"), service.hentValutakurs("EUR", sisteJuni).kurs)
        assertEquals(BigDecimal("11.6"), service.hentValutakurs("EUR", sisteJuni).kurs)
        Mockito.verify(klient, Mockito.times(2)).hentValutakurs(Frequency.Monthly, listOf("NOK"), sisteJuni)
    }

    @Test
    fun `manglende og ugyldige kilder avvises`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(respons("NOK" to "11", "USD" to "0"))

        assertThrows<ECBServiceException> { service.hentValutakurs("USD", sisteJuni) }
    }

    @Test
    fun `manglende valutaserie avvises selv om ECB svarer med NOK`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(respons("NOK" to "11"))

        assertThrows<ECBServiceException> { service.hentValutakurs("USD", sisteJuni) }
    }

    @Test
    fun `HTTP-feil fra consumer utløser fallback i tjenesten`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(RestResponse.Failure("Fant ikke kurs", HttpStatus.NOT_FOUND, HttpClientErrorException(HttpStatus.NOT_FOUND)))

        assertThrows<ECBServiceException> { service.hentValutakurs("USD", sisteJuni) }
    }

    private fun respons(vararg kurser: Pair<String, String>): RestResponse<SdmxData> = RestResponse.Success(
        SdmxData(
            listOf(
                SdmxDataSet(
                    kurser.mapIndexed { indeks, (_, kurs) -> "$indeks:0" to SdmxSeries(mapOf("0" to listOf(kurs))) }.toMap(),
                ),
            ),
            SdmxStructure(
                SdmxDimensions(
                    series = listOf(
                        SdmxDimension("CURRENCY", kurser.map { SdmxValue(it.first) }),
                        SdmxDimension("FREQ", listOf(SdmxValue("M"))),
                    ),
                    observation = listOf(SdmxDimension("TIME_PERIOD", listOf(SdmxValue("2025-06")))),
                ),
            ),
        ),
    )
}
