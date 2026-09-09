package no.nav.bidrag.henvendelse.aop

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

/**
 * Det viktigste her er ikke statuskodene, men at `detail` ikke inneholder noe fra
 * exception-meldingen. Meldingene fra AbstractRestClient inneholder den kallede URL-en, og
 * hos oss har hver URL `?aktorid=...` - det skal ikke ut til nettleseren.
 *
 * At handlerne faktisk blir *valgt* av Spring for de exceptionene AbstractRestClient kaster,
 * dekkes i [no.nav.bidrag.henvendelse.HenvendelseIntegrasjonTest] - det kan ikke testes ved
 * å kalle metodene direkte, slik denne klassen gjør.
 */
class DefaultRestControllerAdviceTest {
    private val advice = DefaultRestControllerAdvice()

    @Test
    fun `skal gi 502 og ikke lekke aktørid ved feilstatus fra ekstern tjeneste`() {
        val exception = HttpClientErrorException(
            HttpStatus.FORBIDDEN,
            "403 Forbidden on GET request for \"$URL_MED_AKTØRID\": [Machine token authorization not sufficient]",
        )

        val problem = advice.handleRestClientResponseException(exception)

        problem.status shouldBe HttpStatus.BAD_GATEWAY.value()
        problem.title shouldBe "Feil ved kall mot tjeneste"
        problem.detail!!.utenLekkasje()
    }

    @Test
    fun `skal gi 502 når tjenesten svarer med serverfeil`() {
        val problem = advice.handleRestClientResponseException(HttpServerErrorException(HttpStatus.BAD_GATEWAY))

        problem.status shouldBe HttpStatus.BAD_GATEWAY.value()
    }

    @Test
    fun `skal gi 502 og ikke lekke aktørid ved timeout`() {
        val exception = ResourceAccessException("I/O error on GET request for \"$URL_MED_AKTØRID\": timeout")

        val problem = advice.handleResourceAccessException(exception)

        problem.status shouldBe HttpStatus.BAD_GATEWAY.value()
        problem.title shouldBe "Tjenesten svarte ikke"
        problem.detail!!.utenLekkasje()
    }

    @Test
    fun `skal pakke ut ResourceAccessException som AbstractRestClient har pakket i en RuntimeException`() {
        // Slik AbstractRestClient.executeMedMetrics pakker alt som ikke er en responsfeil.
        val exception = RuntimeException(
            "Feil ved kall mot uri=$URL_MED_AKTØRID",
            ResourceAccessException("I/O error: timeout"),
        )

        val problem = advice.handleUkjentFeil(exception)

        problem.status shouldBe HttpStatus.BAD_GATEWAY.value()
        problem.title shouldBe "Tjenesten svarte ikke"
        problem.detail!!.utenLekkasje()
    }

    @Test
    fun `skal pakke ut feilstatus som ligger nedpakket i årsakskjeden`() {
        val exception = RuntimeException(
            "Feil ved kall mot uri=$URL_MED_AKTØRID",
            IllegalStateException("mellomledd", HttpClientErrorException(HttpStatus.FORBIDDEN)),
        )

        val problem = advice.handleUkjentFeil(exception)

        problem.status shouldBe HttpStatus.BAD_GATEWAY.value()
        problem.title shouldBe "Feil ved kall mot tjeneste"
    }

    @Test
    fun `skal gi 500 for feil som ikke skyldes en ekstern tjeneste`() {
        val problem = advice.handleUkjentFeil(IllegalStateException("noe gikk galt internt"))

        problem.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        problem.title shouldBe "Ukjent feil"
        problem.detail!! shouldNotContain "noe gikk galt internt"
    }

    @Test
    fun `skal tåle syklisk årsakskjede`() {
        val ytre = RuntimeException("ytre")
        val indre = RuntimeException("indre", ytre)
        ytre.initCause(indre)

        val problem = advice.handleUkjentFeil(ytre)

        problem.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
    }

    @Test
    fun `skal gi 400 ved ugyldig ident`() {
        val problem = advice.handleUgyldigIdent(UgyldigIdentException())

        problem.status shouldBe HttpStatus.BAD_REQUEST.value()
        problem.title shouldBe "Ugyldig ident"
        problem.detail!!.utenLekkasje()
    }

    @Test
    fun `skal gi 401 ved manglende token`() {
        val problem = advice.handleUnauthorizedException(JwtTokenUnauthorizedException("Token expired"))

        problem.status shouldBe HttpStatus.UNAUTHORIZED.value()
        problem.title shouldBe "Autentiseringsfeil"
    }

    private fun String.utenLekkasje() {
        this shouldNotContain AKTØRID
        this shouldNotContain "aktorid"
        this shouldNotContain "http"
    }

    companion object {
        private const val AKTØRID = "2000012345678"
        private const val URL_MED_AKTØRID =
            "https://sf-henvendelse.intern.dev.nav.no/henvendelseinfo/henvendelseliste?aktorid=$AKTØRID"
    }
}
