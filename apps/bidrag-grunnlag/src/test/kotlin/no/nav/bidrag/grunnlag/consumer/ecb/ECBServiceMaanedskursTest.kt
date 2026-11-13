package no.nav.bidrag.grunnlag.consumer.ecb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurser.ECBConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.NorgesBankConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDataSet
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimension
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSeries
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxValue
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.toExchangeRates
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.consumer.valutakurser.norgesBankSvar
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import no.nav.bidrag.grunnlag.service.HentValutakursService
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
import java.time.YearMonth

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
        val kurser = data.toExchangeRates(Frequency.Monthly)

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
    fun `skal avvise dagskurser ved innhenting av en månedskurs`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(respons("NOK" to "12", "USD" to "2", frekvens = Frequency.Daily))

        assertThrows<ECBServiceException> { service.hentValutakurs("USD", sisteJuni) }
    }

    @Test
    fun `skal lese dagskurser når forespurt frekvens er daglig`() {
        val data = respons("NOK" to "12", frekvens = Frequency.Daily).body

        val kurs = data.toExchangeRates(Frequency.Daily).single()

        kurs.valuta shouldBe "NOK"
        kurs.kurs shouldBe BigDecimal("12")
        kurs.kursDato shouldBe sisteJuni
    }

    @Test
    fun `skal avvise dagskurser ved innhenting av månedskurser i batch`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(respons("NOK" to "12", "USD" to "2", frekvens = Frequency.Daily))

        assertThrows<ECBServiceException> { service.hentValutakurser(listOf("USD"), sisteJuni) }
    }

    @Test
    fun `skal bruke Norges Bank når ECB returnerer dagskurser i et månedssvar`() {
        val norgesBank = Mockito.mock(NorgesBankConsumer::class.java)
        val hentValutakursService = HentValutakursService(service, norgesBank)
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(respons("NOK" to "12", "USD" to "2", frekvens = Frequency.Daily))
        Mockito.`when`(norgesBank.hentValutakurs(Frekvens.MÅNEDLIG, "USD", sisteJuni))
            .thenReturn(RestResponse.Success(norgesBankSvar(valuta = "USD", periode = "2025-06", kurs = "1075", multiplikator = "-2")))

        val resultat = hentValutakursService.hentValutakurs(
            HentValutakursRequest(listOf(HentValutakurs(LocalDate.of(2025, 7, 1), Valutakode.USD))),
        ).hentetValutakursListe.single()

        val kurs = resultat.shouldBeInstanceOf<HentetValutakursResultat.HentetValutakurs>()
        kurs.kilde shouldBe ValutakursgrunnlagKilde.NORGES_BANK
        kurs.valutakursSnitt shouldBe BigDecimal("10.75")
        kurs.periode shouldBe ÅrMånedsperiode("2025-06", "2025-07")
        Mockito.verify(norgesBank).hentValutakurs(Frekvens.MÅNEDLIG, "USD", sisteJuni)
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

    @Test
    fun `batch bruker én NOK-serie og beholder kurser når en valuta mangler`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD", "DKK"), sisteJuni))
            .thenReturn(respons("NOK" to "12", "USD" to "2"))

        val kurser = service.hentValutakurser(listOf("USD", "EUR", "DKK", "USD"), sisteJuni)

        assertEquals(setOf("USD", "EUR"), kurser.keys)
        assertEquals(BigDecimal("6.0000000000"), kurser.getValue("USD").kurs)
        assertEquals(BigDecimal("12"), kurser.getValue("EUR").kurs)
        Mockito.verify(klient).hentValutakurs(Frequency.Monthly, listOf("NOK", "USD", "DKK"), sisteJuni)
        Mockito.verifyNoMoreInteractions(klient)
    }

    @Test
    fun `batch utelater ugyldig enkeltkurs uten å forkaste gyldige kurser`() {
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD", "DKK"), sisteJuni))
            .thenReturn(respons("NOK" to "12", "USD" to "2", "DKK" to "0"))

        assertEquals(setOf("USD"), service.hentValutakurser(listOf("USD", "DKK"), sisteJuni).keys)
    }

    @Test
    fun `skal beholde gyldige batchkurser når en valuta har null tom eller uleselig observasjon`() {
        val svar = respons("NOK" to "12", "USD" to "2", "DKK" to "1")
        val observasjoner = listOf(null, emptyList(), listOf(""), listOf("ugyldig"))

        observasjoner.forEach { observasjon ->
            val data = svar.body.copy(
                dataSets = listOf(
                    SdmxDataSet(svar.body.dataSets.single().series + ("2:0" to SdmxSeries(mapOf("0" to observasjon)))),
                ),
            )
            Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD", "DKK"), sisteJuni))
                .thenReturn(RestResponse.Success(data))

            val kurser = service.hentValutakurser(listOf("USD", "EUR", "DKK"), sisteJuni)

            kurser.keys shouldBe setOf("USD", "EUR")
            kurser.getValue("USD").kurs shouldBe BigDecimal("6.0000000000")
            kurser.getValue("EUR").kurs shouldBe BigDecimal("12")
        }
    }

    @Test
    fun `skal avvise batch når NOK har null tom eller uleselig observasjon`() {
        val svar = respons("NOK" to "12", "USD" to "2")
        val observasjoner = listOf(null, emptyList(), listOf(""), listOf("ugyldig"))

        observasjoner.forEach { observasjon ->
            val data = svar.body.copy(
                dataSets = listOf(
                    SdmxDataSet(svar.body.dataSets.single().series + ("0:0" to SdmxSeries(mapOf("0" to observasjon)))),
                ),
            )
            Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
                .thenReturn(RestResponse.Success(data))

            val feil = assertThrows<ECBServiceException> { service.hentValutakurser(listOf("USD"), sisteJuni) }

            feil.message shouldBe "ECB-svaret mangler en gyldig NOK-kurs for $sisteJuni"
        }
    }

    @Test
    fun `batch avviser manglende NOK duplisert NOK feil måned og uventet valuta`() {
        val feilMåned = respons("NOK" to "12", "USD" to "2").body.let { data ->
            data.copy(structure = data.structure.copy(dimensions = data.structure.dimensions.copy(observation = listOf(SdmxDimension("TIME_PERIOD", listOf(SdmxValue("2025-05")))))))
        }
        Mockito.`when`(klient.hentValutakurs(Frequency.Monthly, listOf("NOK", "USD"), sisteJuni))
            .thenReturn(
                respons("USD" to "2"),
                respons("NOK" to "12", "NOK" to "13", "USD" to "2"),
                RestResponse.Success(feilMåned),
                respons("NOK" to "12", "USD" to "2", "DKK" to "1"),
            )

        repeat(4) {
            assertThrows<ECBServiceException> { service.hentValutakurser(listOf("USD"), sisteJuni) }
        }
    }

    private fun respons(vararg kurser: Pair<String, String>, frekvens: Frequency = Frequency.Monthly): RestResponse.Success<SdmxData> = RestResponse.Success(
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
                        SdmxDimension("FREQ", listOf(SdmxValue(frekvens.toFrequencyParam()))),
                    ),
                    observation = listOf(
                        SdmxDimension(
                            "TIME_PERIOD",
                            listOf(SdmxValue(if (frekvens == Frequency.Monthly) YearMonth.from(sisteJuni).toString() else sisteJuni.toString())),
                        ),
                    ),
                ),
            ),
        ),
    )
}
