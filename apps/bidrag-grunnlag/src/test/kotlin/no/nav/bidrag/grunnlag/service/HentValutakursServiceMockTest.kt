package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.valutakurser.NorgesBankConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDataSet
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimension
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSeries
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSimplified
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxValue
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatusCode
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class HentValutakursServiceMockTest {

    @InjectMocks
    private lateinit var hentValutakursService: HentValutakursService

    @Mock
    private lateinit var norgesBankConsumerMock: NorgesBankConsumer

    @Test
    fun `Skal returnere valutakurs når consumer-response er SUCCESS`() {
        val mockResponse = SdmxSimplified(
            data = SdmxData(
                dataSets = listOf(
                    SdmxDataSet(
                        series = mapOf(
                            "0:0:0:0" to SdmxSeries(
                                observations = mapOf("0" to listOf("10.5")),
                            ),
                        ),
                    ),
                ),
                structure = SdmxStructure(
                    dimensions = SdmxDimensions(
                        series = listOf(
                            SdmxDimension("BASE_CUR", listOf(SdmxValue("ISK"))),
                        ),
                    ),
                ),
            ),
        )

        Mockito.`when`(norgesBankConsumerMock.hentValutakurs(any(), any())).thenReturn(RestResponse.Success(mockResponse))

        val request = HentValutakursRequest(listOf(HentValutakurs(LocalDate.now(), Valutakode.ISK)))
        val response = hentValutakursService.hentValutakurs(request).hentetValutakursListe.first()

        Mockito.verify(norgesBankConsumerMock, Mockito.times(1)).hentValutakurs(any(), any())

        assertAll(
            { assertThat(response).isNotNull() },
            { assertThat(response.periode).isEqualTo(ÅrMånedsperiode(LocalDate.now().minusMonths(1), LocalDate.now())) },
            { assertThat(response.valutakursSnitt).isEqualTo(BigDecimal.valueOf(10.5)) },
        )
    }

    @Test
    fun `Skal kaste exception når consumer-response er FAILURE`() {
        Mockito.`when`(norgesBankConsumerMock.hentValutakurs(any(), any())).thenReturn(
            RestResponse.Failure(
                message = "Feil ved oppslag",
                statusCode = HttpStatusCode.valueOf(500),
                restClientException = RuntimeException("Internal Server Error"),
            ),
        )

        val request = HentValutakursRequest(listOf(HentValutakurs(LocalDate.now(), Valutakode.ISK)))

        val exception = assertThrows<Exception> {
            hentValutakursService.hentValutakurs(request)
        }

        Mockito.verify(norgesBankConsumerMock, Mockito.times(1)).hentValutakurs(any(), any())

        assertAll(
            { assertThat(exception).isNotNull() },
            { assertThat(exception.message).isEqualTo("Feil ved henting av valutakurs: Feil ved oppslag") },
        )
    }

    @Test
    fun `Skal håndtere tom liste i responsen`() {
        val mockResponse = SdmxSimplified(
            data = SdmxData(
                dataSets = emptyList(),
                structure = SdmxStructure(
                    dimensions = SdmxDimensions(series = emptyList()),
                ),
            ),
        )

        Mockito.`when`(norgesBankConsumerMock.hentValutakurs(any(), any())).thenReturn(RestResponse.Success(mockResponse))

        val request = HentValutakursRequest(listOf(HentValutakurs(LocalDate.now(), Valutakode.ISK)))

        assertThrows<NoSuchElementException> {
            hentValutakursService.hentValutakurs(request)
        }

        Mockito.verify(norgesBankConsumerMock, Mockito.times(1)).hentValutakurs(any(), any())
    }
}
