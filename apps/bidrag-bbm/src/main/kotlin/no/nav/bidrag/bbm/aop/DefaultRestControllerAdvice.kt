package no.nav.bidrag.bbm.aop

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.ifTrue
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

private val LOGGER = KotlinLogging.logger {}

@RestControllerAdvice
class DefaultRestControllerAdvice {
    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        LOGGER.warn(exception) { "Unyielding eller manglende sikkerhetstoken" }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, "Ugyldig eller manglende sikkerhetstoken")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(
        value = [
            MethodArgumentNotValidException::class, InvalidFormatException::class, IllegalArgumentException::class,
            MethodArgumentTypeMismatchException::class, ConversionFailedException::class, HttpMessageNotReadableException::class,
        ],
    )
    fun handleInvalidValueExceptions(exception: Exception): ResponseEntity<*> {
        val cause = exception.cause ?: exception
        val validationError =
            when (cause) {
                is JsonMappingException -> createMissingKotlinParameterViolation(cause)
                is MethodArgumentNotValidException -> parseMethodArgumentNotValidException(cause)
                else -> null
            }
        val feilmelding =
            validationError?.fieldErrors?.joinToString(", ") { "${it.field}: ${it.message}" }
                ?: exception.message

        LOGGER.error(exception) { "$feilmelding" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.CONTENT_TYPE, "${MediaType.TEXT_PLAIN_VALUE}; charset=UTF-8")
            .header(
                HttpHeaders.WARNING,
                feilmelding ?: "Ukjent feilmelding",
            ).body(feilmelding)
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil: ${exception.message}")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val feilmelding = getErrorMessage(exception)
        val payloadFeilmelding =
            exception.responseBodyAsString.isEmpty().ifTrue { exception.message }
                ?: exception.responseBodyAsString
        LOGGER.warn(exception) { feilmelding }
        secureLogger.warn(exception) { "Feilmelding: $feilmelding. Innhold: $payloadFeilmelding" }
        return ResponseEntity
            .status(exception.statusCode)
            .header(HttpHeaders.WARNING, feilmelding)
            .body(payloadFeilmelding)
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()

        exception.responseHeaders
            ?.get(HttpHeaders.WARNING)
            ?.firstOrNull()
            ?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            if (exception.statusCode == HttpStatus.BAD_REQUEST) {
                errorMessage.append("Validering feilet - ")
            }
            errorMessage.append(exception.statusText)
        }
        return errorMessage.toString()
    }

    private fun createMissingKotlinParameterViolation(ex: JsonMappingException): Error {
        val error = Error(HttpStatus.BAD_REQUEST.value(), "validation error")
        ex.path.filter { it.fieldName != null }.forEach {
            error.addFieldError(it.from.toString(), it.fieldName, ex.message.toString())
        }
        return error
    }

    private fun parseMethodArgumentNotValidException(ex: MethodArgumentNotValidException): Error {
        val error = Error(HttpStatus.BAD_REQUEST.value(), "validation error")
        ex.fieldErrors.forEach {
            val message: String =
                if (it.defaultMessage == null) it.toString() else it.defaultMessage!!
            error.addFieldError(it.objectName, it.field, message)
        }
        return error
    }

    data class Error(
        val status: Int,
        val message: String,
        val fieldErrors: MutableList<CustomFieldError> = mutableListOf(),
    ) {
        fun addFieldError(
            objectName: String,
            field: String,
            message: String,
        ) {
            val error = CustomFieldError(objectName, field, message)
            fieldErrors.add(error)
        }
    }

    data class CustomFieldError(
        val objectName: String,
        val field: String,
        val message: String,
    )
}
