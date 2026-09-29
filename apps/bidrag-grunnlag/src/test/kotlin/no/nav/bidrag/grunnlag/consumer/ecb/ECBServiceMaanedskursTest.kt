package no.nav.bidrag.grunnlag.consumer.ecb

import no.nav.bidrag.grunnlag.consumer.valutakurs.ECBValutakursRestKlient
import no.nav.bidrag.grunnlag.consumer.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.ECBValutakursData
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.toExchangeRates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate

class ECBServiceMaanedskursTest {
    private val klient = Mockito.mock(ECBValutakursRestKlient::class.java)
    private val service = ECBService(klient)
    private val sisteJuni = LocalDate.of(2025, 6, 30)

    @Test
    fun `månedssvar fra ECB leses som juni og ikke juli`() {
        val xml = """
            <message:GenericData xmlns:message="urn:sdmx:message" xmlns:generic="urn:sdmx:generic">
              <message:DataSet>
                <generic:Series>
                  <generic:SeriesKey><generic:Value id="CURRENCY" value="NOK"/><generic:Value id="FREQ" value="M"/></generic:SeriesKey>
                  <generic:Attributes><generic:Value id="UNIT_MULT" value="0"/></generic:Attributes>
                  <generic:Obs><generic:ObsDimension value="2025-06"/><generic:ObsValue value="11.584133333333332"/></generic:Obs>
                </generic:Series>
              </message:DataSet>
            </message:GenericData>
        """.trimIndent()

        val kurser = SDMXValutakursRestKlientConfig().xmlMapper().readValue(xml, ECBValutakursData::class.java).toExchangeRates()

        assertEquals(sisteJuni, kurser.single().kursDato)
        assertEquals(BigDecimal("11.584133333333332"), kurser.single().kurs)
    }

    @Test
    fun `ECB krysskurs beregnes som NOK per USD`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni)).thenReturn(
            listOf(
                Valutakurs("NOK", BigDecimal("11.584133333333332"), sisteJuni),
                Valutakurs("USD", BigDecimal("1.1518"), sisteJuni),
            ),
        )
        val kurs = service.hentValutakurs("USD", sisteJuni)

        assertEquals(BigDecimal("10.0574173757"), kurs.kurs)
        assertEquals("USD", kurs.valuta)
        assertEquals(sisteJuni, kurs.kursDato)
    }

    @Test
    fun `EUR bruker bare NOK-serien`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK"), sisteJuni))
            .thenReturn(listOf(Valutakurs("NOK", BigDecimal("11.5841"), sisteJuni)))
        assertEquals(BigDecimal("11.5841"), service.hentValutakurs("EUR", sisteJuni).kurs)
    }

    @Test
    fun `ECB-kurs hentes på nytt ved neste forespørsel`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK"), sisteJuni))
            .thenReturn(
                listOf(Valutakurs("NOK", BigDecimal("11.5"), sisteJuni)),
                listOf(Valutakurs("NOK", BigDecimal("11.6"), sisteJuni)),
            )

        assertEquals(BigDecimal("11.5"), service.hentValutakurs("EUR", sisteJuni).kurs)
        assertEquals(BigDecimal("11.6"), service.hentValutakurs("EUR", sisteJuni).kurs)
        Mockito.verify(klient, Mockito.times(2)).hentValutakurs(Frequency.Monthly, listOf("NOK"), sisteJuni)
    }

    @Test
    fun `manglende og ugyldige kilder avvises`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(listOf(Valutakurs("NOK", BigDecimal("11"), sisteJuni), Valutakurs("USD", BigDecimal.ZERO, sisteJuni)))

        assertThrows<ECBServiceException> { service.hentValutakurs("USD", sisteJuni) }
    }
}
