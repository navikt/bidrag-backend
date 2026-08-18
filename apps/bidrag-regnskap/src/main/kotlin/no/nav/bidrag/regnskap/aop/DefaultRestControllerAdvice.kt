package no.nav.bidrag.regnskap.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClientException
import no.nav.bidrag.regnskap.util.PåløpException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException

private val LOGGER = KotlinLogging.logger {}

@RestControllerAdvice
class DefaultRestControllerAdvice {

    companion object {
        private const val EXTERNAL_SERVICE_ERROR_PREFIX = "Det skjedde en feil ved kall mot ekstern tjeneste: "
    }

    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ProblemDetail {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(exception) { errorMessage }
        return ProblemDetail.forStatusAndDetail(
            exception.statusCode,
            errorMessage,
        ).apply {
            title = "Feil mot ekstern tjeneste"
        }
    }

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

    @ExceptionHandler(MaskinportenClientException::class)
    fun handleMaskinportenClientException(exception: MaskinportenClientException): ProblemDetail {
        LOGGER.error(exception) { "Noe gikk galt ved kall til maskinporten: ${exception.message}" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Noe gikk galt ved kall til maskinporten: ${exception.message}",
        ).apply {
            title = "Maskinporten-feil"
        }
    }

    @ExceptionHandler(PåløpException::class)
    fun handlePåløpException(exception: PåløpException): ProblemDetail {
        LOGGER.error(exception) { "Ble stoppet grunnet påløp: ${exception.message}" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Ble stoppet grunnet påløp: ${exception.message}",
        ).apply {
            title = "Påløp pågår"
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ProblemDetail {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Det skjedde en ukjent feil: ${exception.message}",
        ).apply {
            title = "Ukjent feil"
        }
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        errorMessage.append(EXTERNAL_SERVICE_ERROR_PREFIX)
        exception.responseHeaders?.get(HttpHeaders.WARNING)?.firstOrNull()?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(exception.statusText)
        }
        if (exception.message?.isNotEmpty() == true) {
            errorMessage.append(exception.message)
        }
        return errorMessage.toString()
    }
}
