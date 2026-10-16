package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.NorgesBankValutakursMappingException
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxAttributes
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDataSet
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimension
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSeries
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSimplified
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxValue
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.tilValutakurs
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate

internal fun norgesBankSvar(
    valuta: String = "DKK",
    periode: String = "2025-06",
    calculated: String = "false",
    kurs: String = "155.34",
    multiplikator: String = "2",
    tenor: String = "SP",
): SdmxSimplified = SdmxSimplified(
    SdmxData(
        listOf(SdmxDataSet(mapOf("0:0:0:0" to SdmxSeries(mapOf("0" to listOf(kurs)), listOf(0, 0, 0))))),
        SdmxStructure(
            SdmxDimensions(
                listOf(
                    SdmxDimension("FREQ", listOf(SdmxValue("M"))),
                    SdmxDimension("BASE_CUR", listOf(SdmxValue(valuta))),
                    SdmxDimension("QUOTE_CUR", listOf(SdmxValue("NOK"))),
                    SdmxDimension("TENOR", listOf(SdmxValue(tenor))),
                ),
                listOf(SdmxDimension("TIME_PERIOD", listOf(SdmxValue(periode)))),
            ),
            SdmxAttributes(
                listOf(
                    SdmxDimension("CALCULATED", listOf(SdmxValue(calculated))),
                    SdmxDimension("UNIT_MULT", listOf(SdmxValue(multiplikator))),
                    SdmxDimension("COLLECTION", listOf(SdmxValue("A"))),
                ),
            ),
        ),
    ),
)

class NorgesBankMaanedskursTest {
    private val dato = LocalDate.of(2025, 6, 30)

    @Test
    fun `JSON fra Norges Bank leses via felles consumer og kurs per enhet normaliseres`() {
        val json = """
            {
              "data": {
                "dataSets": [{"series": {"0:0:0:0": {"attributes": [0,0,0,0], "observations": {"0": ["155.34"]}}}}],
                "structure": {
                  "dimensions": {"series": [
                    {"id": "FREQ", "values": [{"id": "M"}]},
                    {"id": "BASE_CUR", "values": [{"id": "DKK"}]},
                    {"id": "QUOTE_CUR", "values": [{"id": "NOK"}]},
                    {"id": "TENOR", "values": [{"id": "SP"}]}
                  ], "observation": [{"id": "TIME_PERIOD", "values": [{"id": "2025-06"}]}]},
                  "attributes": {"series": [
                    {"id": "DECIMALS", "values": [{"id": "2"}]},
                    {"id": "CALCULATED", "values": [{"id": "false"}]},
                    {"id": "UNIT_MULT", "values": [{"id": "2"}]},
                    {"id": "COLLECTION", "values": [{"id": "A"}]}
                  ]}
                }
              }
            }
        """.trimIndent()
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = NorgesBankConsumer(URI.create("https://data.norges-bank.no"), restTemplate, GrunnlagConsumer())
        server.expect(requestTo(consumer.lagNorgesBankURI(Frekvens.MÅNEDLIG, "DKK", dato)))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val resultat = consumer.hentValutakurs(Frekvens.MÅNEDLIG, "DKK", dato)

        val svar = assertInstanceOf(SdmxSimplified::class.java, assertInstanceOf(RestResponse.Success::class.java, resultat).body)
        val kurs = svar.tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        assertEquals(BigDecimal("1.5534"), kurs.kurs)
        assertEquals(dato, kurs.kursDato)
        server.verify()
    }

    @Test
    fun `HTTP-feil gir feilresultat`() {
        val restTemplate = RestTemplate()
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = NorgesBankConsumer(URI.create("https://data.norges-bank.no"), restTemplate, GrunnlagConsumer())
        server.expect(requestTo(consumer.lagNorgesBankURI(Frekvens.MÅNEDLIG, "DKK", dato))).andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertEquals(HttpStatus.NOT_FOUND, assertInstanceOf(RestResponse.Failure::class.java, consumer.hentValutakurs(Frekvens.MÅNEDLIG, "DKK", dato)).statusCode)
        server.verify()
    }

    @Test
    fun `måned fra feil halvår avvises`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            norgesBankSvar().tilValutakurs("DKK", Frekvens.MÅNEDLIG, LocalDate.of(2025, 12, 31))
        }
    }

    @Test
    fun `kalkulert kurs avvises`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            norgesBankSvar(calculated = "true").tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
    }

    @Test
    fun `annen tenor enn spot avvises`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            norgesBankSvar(tenor = "1M").tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
    }

    @Test
    fun `tenor valideres fra seriens indeks selv om spot finnes i dimensjonen`() {
        val svar = norgesBankSvar()
        val struktur = svar.data.structure
        val dimensjoner = struktur.dimensions.copy(
            series = struktur.dimensions.series.map {
                if (it.id == "TENOR") it.copy(values = listOf(SdmxValue("SP"), SdmxValue("1M"))) else it
            },
        )
        val feilTenor = svar.copy(
            data = svar.data.copy(
                dataSets = listOf(SdmxDataSet(mapOf("0:0:0:1" to svar.data.dataSets.single().series.values.single()))),
                structure = struktur.copy(dimensions = dimensjoner),
            ),
        )

        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            feilTenor.tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
    }

    @Test
    fun `manglende tenordimensjon avvises`() {
        val svar = norgesBankSvar()
        val struktur = svar.data.structure
        val utenTenor = svar.copy(
            data = svar.data.copy(
                structure = struktur.copy(dimensions = struktur.dimensions.copy(series = struktur.dimensions.series.filter { it.id != "TENOR" })),
            ),
        )

        assertThrows<NorgesBankValutakursMappingException.ManglerFelt> {
            utenTenor.tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
    }

    @Test
    fun `feil valuta og ugyldig multiplikator avvises`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            norgesBankSvar(valuta = "USD").tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            norgesBankSvar(multiplikator = "100").tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
    }

    @Test
    fun `manglende kurs og observasjon avvises`() {
        assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
            norgesBankSvar(kurs = "0").tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
        assertThrows<NorgesBankValutakursMappingException.UventetAntall> {
            norgesBankSvar().copy(data = norgesBankSvar().data.copy(dataSets = emptyList())).tilValutakurs("DKK", Frekvens.MÅNEDLIG, dato)
        }
    }
}
