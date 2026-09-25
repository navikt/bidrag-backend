package no.nav.bidrag.vedtak.exception

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.transport.felles.ifTrue
import no.nav.bidrag.vedtak.exception.custom.PreconditionFailedException
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
@Component
@Suppress("unused")
class RestExceptionHandler {
    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    @ResponseBody
    @ExceptionHandler(HttpClientErrorException::class, HttpServerErrorException::class)
    protected fun handleHttpClientErrorException(e: HttpStatusCodeException): ResponseEntity<*> {
        LOGGER.warn(e) { "Det skjedde en feil ${e.message?.sanitizeForLog()}" }
        val payloadFeilmelding =
            e.responseBodyAsString.isEmpty().ifTrue { e.message }
                ?: e.responseBodyAsString
        return ResponseEntity.status(e.statusCode)
            .header(HttpHeaders.WARNING, e.message ?: "")
            .body(payloadFeilmelding)
    }

    @ResponseBody
    @ExceptionHandler(
        value = [
            IllegalArgumentException::class, MethodArgumentTypeMismatchException::class, ConversionFailedException::class,
            HttpMessageNotReadableException::class,
        ],
    )
    fun handleInvalidValueExceptions(exception: Exception): ResponseEntity<*> {
        val cause = exception.cause
        val valideringsFeil = if (cause is MismatchedInputException) createMissingKotlinParameterViolation(cause) else null
        LOGGER.error(exception) { "Forespørselen inneholder ugyldig verdi: ${(valideringsFeil ?: "ukjent feil").sanitizeForLog()}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, "Forespørselen inneholder ugyldig verdi: ${valideringsFeil ?: exception.message}")
            .build<Any>()
    }

    private fun createMissingKotlinParameterViolation(ex: MismatchedInputException): String {
        val errorFieldRegex = Regex("\\.([^.]*)\\[\\\"(.*)\"\\]\$")
        val paths = ex.path.map { errorFieldRegex.find(it.description)!! }.map {
            val (objectName, field) = it.destructured
            "$objectName.$field"
        }
        return "${paths.joinToString("->")} kan ikke være null"
    }

    @ResponseBody
    @ExceptionHandler(PreconditionFailedException::class)
    protected fun handleConflictException(e: PreconditionFailedException): ResponseEntity<*> {
        val feilmelding = "Feil, angitt sisteVedtaksid er ikke det nyeste vedtaket for stønaden: ${e.message}"
        return ResponseEntity
            .status(HttpStatus.PRECONDITION_FAILED)
            .header(HttpHeaders.WARNING, feilmelding)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    protected fun handleOtherExceptions(e: Exception): ResponseEntity<*> {
        val feilmelding = "Det skjedde en feil: ${e.message}"
        LOGGER.error(e) { feilmelding.sanitizeForLog() }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, feilmelding)
            .build<Any>()
    }
}
