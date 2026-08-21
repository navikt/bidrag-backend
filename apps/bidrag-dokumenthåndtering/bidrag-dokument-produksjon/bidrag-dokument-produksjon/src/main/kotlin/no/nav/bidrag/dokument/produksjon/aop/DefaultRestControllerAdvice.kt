package no.nav.bidrag.dokument.produksjon.aop

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.io.IOException

val LOGGER = KotlinLogging.logger { }

@RestControllerAdvice
class DefaultRestControllerAdvice {
    @ResponseBody
    @ExceptionHandler(
        value = [
            IllegalArgumentException::class, MethodArgumentTypeMismatchException::class,
            ConversionFailedException::class, HttpMessageNotReadableException::class,
        ],
    )
    fun handleInvalidValueExceptions(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<*> {
        val cause = exception.cause
        val valideringsFeil =
            if (cause is MismatchedInputException) {
                createMissingKotlinParameterViolation(
                    cause,
                )
            } else {
                null
            }
        LOGGER.warn(
            exception,
        ) { "Forespørselen inneholder ugyldig verdi: ${valideringsFeil ?: "ukjent feil"}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(
                HttpHeaders.WARNING,
                "Forespørselen inneholder ugyldig verdi: ${valideringsFeil ?: exception.message}",
            ).build<Any>()
    }

    private fun createMissingKotlinParameterViolation(ex: MismatchedInputException): String {
        val errorFieldRegex = Regex("\\.([^.]*)\\[\\\"(.*)\"\\]\$")
        val paths =
            ex.path.map { errorFieldRegex.find(it.description) }.map {
                if (it == null) return "???"
                val (objectName, field) = it.destructured
                "$objectName.$field"
            }
        return "${paths.joinToString("->")} kan ikke være null"
    }

    @ResponseBody
    @ExceptionHandler(IOException::class)
    fun handleRequestNotUsableException(exception: IOException): ResponseEntity<*> {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil: ${exception.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil: ${exception.message}")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.error(exception) { "Det skjedde en ukjent feil: ${exception.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil: ${exception.message}")
            .build<Any>()
    }
}
