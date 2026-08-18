package no.nav.bidrag.organisasjon.aop

import no.nav.bidrag.organisasjon.exception.ArbeidsfordelingConsumerException
import no.nav.bidrag.organisasjon.exception.EnhetIkkeFunnetException
import no.nav.bidrag.organisasjon.exception.PersonConsumerException
import no.nav.bidrag.organisasjon.exception.SkjermingConsumerException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException

@RestControllerAdvice
class HttpStatusRestControllerAdvice {
    @ResponseBody
    @ExceptionHandler(value = [ArbeidsfordelingConsumerException::class])
    fun handleFunctionalException(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message, exception)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleEnhetNotFoundException(exception: EnhetIkkeFunnetException): ResponseEntity<*> {
        LOGGER.warn(exception.message, exception)
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handlePersonConsumerException(exception: PersonConsumerException): ResponseEntity<*> {
        LOGGER.error(exception.message, exception)
        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleIllegalArgumentException(illegalArgumentException: IllegalArgumentException): ResponseEntity<*> {
        LOGGER.warn(illegalArgumentException.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, illegalArgumentException.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttClientErrorException(httpClientErrorException: HttpClientErrorException): ResponseEntity<*> = ResponseEntity
        .status(httpClientErrorException.statusCode)
        .header(HttpHeaders.WARNING, httpClientErrorException.message ?: "Ukjent feil")
        .build<Any>()

    @ResponseBody
    @ExceptionHandler
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.error(exception.message, exception)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ExceptionHandler
    fun skjermingConsumerException(skjermingConsumerException: SkjermingConsumerException): ResponseEntity<*>? = ResponseEntity
        .status(skjermingConsumerException.httpStatus)
        .header(HttpHeaders.WARNING, warningFrom(skjermingConsumerException))
        .build<Any>()

    private fun warningFrom(runtimeException: RuntimeException): String = "${runtimeException.javaClass.simpleName}: ${runtimeException.message}"

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HttpStatusRestControllerAdvice::class.java)
    }
}
