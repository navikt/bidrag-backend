package no.nav.bidrag.aktoerregister.aop

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.server.ResponseStatusException

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
    fun `skal returnere ProblemDetail med riktig status ved ResponseStatusException`() {
        val exception = ResponseStatusException(HttpStatus.NOT_FOUND, "Aktør ikke funnet")

        val result = advice.handlerResponseStatusException(exception)

        result.status shouldBe HttpStatus.NOT_FOUND.value()
        result.detail shouldContain "Aktør ikke funnet"
        result.title shouldBe "Forespørselsfeil"
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
