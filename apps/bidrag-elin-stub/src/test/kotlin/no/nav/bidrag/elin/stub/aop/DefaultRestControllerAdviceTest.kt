package no.nav.bidrag.elin.stub.aop

import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultRestControllerAdviceTest {

    private val advice = DefaultRestControllerAdvice()

    @Test
    fun `skal returnere 401 ProblemDetail ved JwtTokenUnauthorizedException`() {
        val exception = JwtTokenUnauthorizedException("Token expired")

        val result = advice.handleUnauthorizedException(exception)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.status)
        assertTrue(result.detail!!.contains("Ugyldig eller manglende sikkerhetstoken"))
        assertEquals("Autentiseringsfeil", result.title)
    }

    @Test
    fun `skal returnere 500 ProblemDetail ved ukjent exception`() {
        val exception = IllegalStateException("Noe gikk galt")

        val result = advice.handleOtherExceptions(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.status)
        assertTrue(result.detail!!.contains("Noe gikk galt"))
        assertEquals("Ukjent feil", result.title)
    }
}
