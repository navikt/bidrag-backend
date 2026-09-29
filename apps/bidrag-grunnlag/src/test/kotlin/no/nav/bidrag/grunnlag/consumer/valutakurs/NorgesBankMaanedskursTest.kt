package no.nav.bidrag.grunnlag.consumer.valutakurs

import no.nav.bidrag.grunnlag.consumer.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.NorgesBankValutakursData
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.NorgesBankValutakursDataSet
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.NorgesBankValutakursMapper.tilValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.NorgesBankValutakursSeries
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.sdmx.SDMXExchangeRateAttributes
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.NorgesBankValutakursMappingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class NorgesBankMaanedskursTest {
    @Test
    fun `månedssvar fra SDMX kan leses og normaliseres`() {
        val xml = """
            <message:GenericData xmlns:message="urn:sdmx:message" xmlns:generic="urn:sdmx:generic">
              <message:DataSet>
                <generic:Series>
                  <generic:SeriesKey><generic:Value id="FREQ" value="M"/><generic:Value id="BASE_CUR" value="DKK"/></generic:SeriesKey>
                  <generic:Attributes><generic:Value id="COLLECTION" value="A"/><generic:Value id="CALCULATED" value="false"/><generic:Value id="UNIT_MULT" value="2"/></generic:Attributes>
                  <generic:Obs><generic:ObsDimension value="2025-06"/><generic:ObsValue value="155.34"/></generic:Obs>
                </generic:Series>
              </message:DataSet>
            </message:GenericData>
        """.trimIndent()

        val data = SDMXValutakursRestKlientConfig().xmlMapper().readValue(xml, NorgesBankValutakursData::class.java)

        assertEquals(BigDecimal("1.5534"), data.tilValutakurs("DKK", Frekvens.MÅNEDLIG, LocalDate.of(2025, 6, 30)).kurs)
    }

    @Test
    fun `månedsmiddel for hundre DKK normaliseres til én DKK`() {
        val kurs = månedsdata().tilValutakurs("DKK", Frekvens.MÅNEDLIG, LocalDate.of(2025, 6, 30))

        assertEquals(BigDecimal("1.5534"), kurs.kurs)
        assertEquals(LocalDate.of(2025, 6, 30), kurs.kursDato)
    }

    @Test
    fun `måned fra feil halvår avvises`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            månedsdata().tilValutakurs("DKK", Frekvens.MÅNEDLIG, LocalDate.of(2025, 12, 31))
        }
    }

    @Test
    fun `ukjent CALCULATED-verdi kan ikke bli behandlet som observert kurs`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            månedsdata(calculated = "ukjent").tilValutakurs("DKK", Frekvens.MÅNEDLIG, LocalDate.of(2025, 6, 30))
        }
    }

    private fun månedsdata(calculated: String = "false") = NorgesBankValutakursData(
        NorgesBankValutakursDataSet(
            NorgesBankValutakursSeries(
                seriesKeys = listOf(SDMXExchangeRateKey("BASE_CUR", "DKK"), SDMXExchangeRateKey("FREQ", "M")),
                attributes = listOf(
                    SDMXExchangeRateAttributes("CALCULATED", calculated),
                    SDMXExchangeRateAttributes("COLLECTION", "A"),
                    SDMXExchangeRateAttributes("UNIT_MULT", "2"),
                ),
                observations = SDMXExchangeRate(SDMXExchangeRateDate("2025-06"), SDMXExchangeRateValue(BigDecimal("155.34"))),
            ),
        ),
    )
}
