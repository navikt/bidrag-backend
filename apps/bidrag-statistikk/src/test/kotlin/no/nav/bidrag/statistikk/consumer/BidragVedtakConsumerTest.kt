package no.nav.bidrag.statistikk.consumer

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

@ExtendWith(MockitoExtension::class)
@DisplayName("BidragVedtakConsumerTest")
internal class BidragVedtakConsumerTest {

    @InjectMocks
    private val bidragVedtakConsumer: BidragVedtakConsumer? = null

    @Mock
    private val restTemplateMock: HttpHeaderRestTemplate? = null

/*    @Test
    fun `Sjekk at ok respons fra BidragVedtak-endepunkt mappes korrekt`() {

        Mockito.`when`(
            restTemplateMock?.exchange(
                eq(BIDRAG_VEDTAK_CONTEXT),
                eq(HttpMethod.POST),
                eq(initHttpEntity(1)),
                any<Class<VedtakDto>>(),
            ),
        )
            .thenReturn(ResponseEntity(TestUtil.byggVedtakDto(), HttpStatus.OK))

        when (val restResponseBidragVedtak = bidragVedtakConsumer!!.hentVedtak(1)) {
            is RestResponse.Success -> {
                val hentVedtakResponse = restResponseBidragVedtak.body
                assertAll(
                    Executable { assertThat(hentVedtakResponse).isNotNull },
                    Executable { assertThat(hentVedtakResponse.perioder.size).isEqualTo(2) },
                    Executable { assertThat(hentVedtakResponse.perioder[0].stønadstype).isEqualTo(BisysStønadstype.UTVIDET) },
                    Executable { assertThat(hentVedtakResponse.perioder[0].fomMåned).isEqualTo(YearMonth.parse("2021-01")) },
                    Executable { assertThat(hentVedtakResponse.perioder[0].tomMåned).isEqualTo(YearMonth.parse("2021-12")) },
                    Executable { assertThat(hentVedtakResponse.perioder[0].beløp).isEqualTo(1000.11) },
                    Executable { assertThat(hentVedtakResponse.perioder[0].manueltBeregnet).isFalse() },
                    Executable { assertThat(hentVedtakResponse.perioder[0].deltBosted).isFalse() },
                    Executable { assertThat(hentVedtakResponse.perioder[1].stønadstype).isEqualTo(BisysStønadstype.UTVIDET) },
                    Executable { assertThat(hentVedtakResponse.perioder[1].fomMåned).isEqualTo(YearMonth.parse("2022-01")) },
                    Executable { assertThat(hentVedtakResponse.perioder[1].tomMåned).isEqualTo(YearMonth.parse("2022-12")) },
                    Executable { assertThat(hentVedtakResponse.perioder[1].beløp).isEqualTo(2000.22) },
                    Executable { assertThat(hentVedtakResponse.perioder[1].manueltBeregnet).isFalse() },
                    Executable { assertThat(hentVedtakResponse.perioder[1].deltBosted).isFalse() },
                )
            }
            else -> {
                fail("Test returnerte med RestResponse.Failure, som ikke var forventet")
            }
        }
    }

    @Test
    fun `Sjekk at exception fra bidrag-vedtak endepunkt håndteres korrekt`() {

        Mockito.`when`(
            restTemplateMock?.exchange(
                eq(BIDRAG_VEDTAK_CONTEXT),
                eq(HttpMethod.POST),
                eq(initHttpEntity(request)),
                any<Class<VedtakDto>>(),
            ),
        )
            .thenThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST))

        when (val restResponseBidragVedtak = bidragVedtakConsumer!!.hentVedtak(1)) {
            is RestResponse.Failure -> {
                assertAll(
                    Executable { assertThat(restResponseBidragVedtak.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                    Executable { assertThat(restResponseBidragVedtak.restClientException).isInstanceOf(HttpClientErrorException::class.java) },
                )
            }
            else -> {
                fail("Test returnerte med RestResponse.Success, som ikke var forventet")
            }
        }
    }*/

    fun <T : Any> initHttpEntity(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, httpHeaders)
    }

    companion object {
        private const val BIDRAG_VEDTAK_CONTEXT = "/vedtak/{vedtaksid}"
    }
}
