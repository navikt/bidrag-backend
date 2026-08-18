package no.nav.bidrag.regnskap.aop

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClientException
import no.nav.bidrag.regnskap.util.PåløpException
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
        result.title shouldBe "Feil mot ekstern tjeneste"
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
    fun `skal returnere 500 ProblemDetail ved MaskinportenClientException`() {
        val exception = MaskinportenClientException("Token ugyldig")

        val result = advice.handleMaskinportenClientException(exception)

        result.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        result.detail shouldContain "Token ugyldig"
        result.title shouldBe "Maskinporten-feil"
    }

    @Test
    fun `skal returnere 503 ProblemDetail ved PåløpException`() {
        val exception = PåløpException("Påløp kjører allerede")

        val result = advice.handlePåløpException(exception)

        result.status shouldBe HttpStatus.SERVICE_UNAVAILABLE.value()
        result.detail shouldContain "Påløp kjører allerede"
        result.title shouldBe "Påløp pågår"
    }

    @Test
    fun `skal returnere 500 ProblemDetail ved ukjent exception`() {
        val exception = IllegalStateException("Noe gikk galt")

        val result = advice.handleException(exception)

        result.status shouldBe HttpStatus.INTERNAL_SERVER_ERROR.value()
        result.detail shouldContain "Noe gikk galt"
        result.title shouldBe "Ukjent feil"
    }
}
