package no.nav.bidrag.belopshistorikk.exception

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.rest.GlobalRestControllerAdvice
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
@Component
@Import(GlobalRestControllerAdvice::class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class RestExceptionHandler {
    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    @ExceptionHandler(
        value = [
            IllegalArgumentException::class, MethodArgumentTypeMismatchException::class, ConversionFailedException::class,
            HttpMessageNotReadableException::class,
        ],
    )
    fun handleInvalidValueExceptions(exception: Exception): ProblemDetail {
        val cause = exception.cause
        val valideringsFeil = if (cause is MismatchedInputException) createMissingKotlinParameterViolation(cause) else null
        LOGGER.warn { "Forespørselen inneholder ugyldig verdi" }

        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Forespørselen inneholder en ugyldig verdi: ${valideringsFeil ?: "ukjent feil"}",
        )
    }

    private fun createMissingKotlinParameterViolation(ex: MismatchedInputException): String {
        val errorFieldRegex = Regex("\\.([^.]*)\\[\"(.*)\"]$")
        val paths =
            ex.path.map { errorFieldRegex.find(it.description)!! }.map {
                val (objectName, field) = it.destructured
                "$objectName.$field"
            }
        return "${paths.joinToString("->")} kan ikke være null"
    }
}
