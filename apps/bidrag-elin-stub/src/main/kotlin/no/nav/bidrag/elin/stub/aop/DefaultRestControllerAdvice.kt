package no.nav.bidrag.elin.stub.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val LOGGER = KotlinLogging.logger { }

@RestControllerAdvice
class DefaultRestControllerAdvice {

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ProblemDetail {
        LOGGER.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Ugyldig eller manglende sikkerhetstoken",
        ).apply {
            title = "Autentiseringsfeil"
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ProblemDetail {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Det skjedde en ukjent feil: ${exception.message}",
        ).apply {
            title = "Ukjent feil"
        }
    }
}
