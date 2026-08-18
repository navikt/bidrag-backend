package no.nav.bidrag.reskontro.aop

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClientException
import no.nav.bidrag.reskontro.exceptions.FeilMotSkattException
import no.nav.bidrag.reskontro.exceptions.IngenDataFraSkattException
import no.nav.bidrag.reskontro.exceptions.TimeoutFraSkattException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class DefaultRestControllerAdviceTest {

    private val advice = DefaultRestControllerAdvice()

    @Test
    fun `skal returnere ProblemDetail med riktig status ved HttpStatusCodeException`() {
        val exception = HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway")

        val result = advice.handleHttpClientErrorException(exception)

        result.status shouldBe HttpStatus.BAD_GATEWAY.value()
        result.detail shouldContain "Bad Gateway"
        result.title shouldBe "Feil ved kall mot tjeneste"
    }

    @Test
    fun `skal returnere 401 ProblemDetail ved JwtTokenUnauthorizedException`() {
        val exception = JwtTokenUnauthorizedException("Token expired")

        val result = advice.handleUnauthorizedException(exception)

        result.status shouldBe HttpStatus.UNAUTHORIZED.value()
        result.detail shouldContain "Ugyldig eller manglende sikkerhetstoken"
        result.title shouldBe "Autentiseringsfeil"
    }

    @Test
    fun `skal returnere 404 ProblemDetail ved IngenDataFraSkattException`() {
        val exception = IngenDataFraSkattException("Ingen treff for saksnummer 123")

        val result = advice.handleIngenDataFraSkattException(exception)

        result.status shouldBe HttpStatus.NOT_FOUND.value()
        result.detail shouldContain "Ingen treff for saksnummer 123"
        result.title shouldBe "Ingen data fra Skatteetaten"
    }

    @Test
    fun `skal returnere 401 ProblemDetail ved MaskinportenClientException`() {
        val exception = MaskinportenClientException("Token ugyldig")

        val result = advice.handleMaskinportenClientException(exception)

        result.status shouldBe HttpStatus.UNAUTHORIZED.value()
        result.detail shouldContain "Token ugyldig"
        result.title shouldBe "Maskinporten-feil"
    }

    @Test
    fun `skal returnere 502 ProblemDetail ved TimeoutFraSkattException`() {
        val exception = TimeoutFraSkattException("Timed out etter 30s")

        val result = advice.handleTimeoutFraSkattException(exception)

        result.status shouldBe HttpStatus.BAD_GATEWAY.value()
        result.detail shouldContain "Timed out etter 30s"
        result.title shouldBe "Timeout ved kall mot Skatteetaten"
    }

    @Test
    fun `skal returnere 500 ProblemDetail med cause ved FeilMotSkattException`() {
        val cause = RuntimeException("Connection refused")
        val exception = FeilMotSkattException("Kall feilet", cause)

        val result = advice.handleFeilMotSkattException(exception)

        result.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        result.detail shouldContain "Kall feilet"
        result.title shouldBe "Feil ved kall mot Skatteetaten"
    }

    @Test
    fun `skal returnere 500 ProblemDetail uten cause ved FeilMotSkattException uten cause`() {
        val exception = FeilMotSkattException("Ukjent feil", null)

        val result = advice.handleFeilMotSkattException(exception)

        result.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        result.properties shouldBe null
    }

    @Test
    fun `skal returnere 500 ProblemDetail ved ukjent exception`() {
        val exception = IllegalStateException("Noe gikk galt")

        val result = advice.handleOtherExceptions(exception)

        result.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        result.detail shouldContain "Noe gikk galt"
        result.title shouldBe "Ukjent feil"
    }
}
