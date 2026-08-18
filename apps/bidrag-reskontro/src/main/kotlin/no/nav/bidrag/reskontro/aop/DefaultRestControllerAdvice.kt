package no.nav.bidrag.reskontro.aop

import no.nav.bidrag.commons.security.maskinporten.MaskinportenClientException
import no.nav.bidrag.reskontro.exceptions.FeilMotSkattException
import no.nav.bidrag.reskontro.exceptions.IngenDataFraSkattException
import no.nav.bidrag.reskontro.exceptions.TimeoutFraSkattException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException

@RestControllerAdvice
class DefaultRestControllerAdvice {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(DefaultRestControllerAdvice::class.java)
    }

    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ProblemDetail {
        LOGGER.warn("Det skjedde en feil ved kall mot ekstern tjeneste: ${exception.statusText}", exception)
        return ProblemDetail.forStatusAndDetail(
            exception.statusCode,
            "Feil ved kall mot tjeneste: ${exception.statusText}",
        ).apply {
            title = "Feil ved kall mot tjeneste"
        }
    }

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ProblemDetail {
        LOGGER.warn("Ugyldig eller manglende sikkerhetstoken", exception)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Ugyldig eller manglende sikkerhetstoken",
        ).apply {
            title = "Autentiseringsfeil"
        }
    }

    @ExceptionHandler(IngenDataFraSkattException::class)
    fun handleIngenDataFraSkattException(exception: IngenDataFraSkattException): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        "${exception.message}",
    ).apply {
        title = "Ingen data fra Skatteetaten"
    }

    @ExceptionHandler(MaskinportenClientException::class)
    fun handleMaskinportenClientException(exception: MaskinportenClientException): ProblemDetail {
        LOGGER.error("Feil i maskinportentoken benyttet mot skatt: ${exception.message}", exception)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Feil i maskinportentoken benyttet mot skatt: ${exception.message}",
        ).apply {
            title = "Maskinporten-feil"
        }
    }

    @ExceptionHandler(TimeoutFraSkattException::class)
    fun handleTimeoutFraSkattException(exception: TimeoutFraSkattException): ProblemDetail {
        LOGGER.warn("Timeout mot skatt: ${exception.message}", exception)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "${exception.message}",
        ).apply {
            title = "Timeout ved kall mot Skatteetaten"
        }
    }

    @ExceptionHandler(FeilMotSkattException::class)
    fun handleFeilMotSkattException(exception: FeilMotSkattException): ProblemDetail {
        LOGGER.error("Feil ved kall mot skatt: ${exception.message}", exception)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "${exception.message}",
        ).apply {
            title = "Feil ved kall mot Skatteetaten"
            exception.cause?.let { setProperty("cause", it.javaClass.simpleName) }
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ProblemDetail {
        LOGGER.warn("Det skjedde en ukjent feil: ${exception.message}", exception)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Det skjedde en ukjent feil: ${exception.message}",
        ).apply {
            title = "Ukjent feil"
        }
    }
}
