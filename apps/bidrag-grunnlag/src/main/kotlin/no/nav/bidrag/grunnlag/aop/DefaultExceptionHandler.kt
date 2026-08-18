package no.nav.bidrag.grunnlag.aop

import com.fasterxml.jackson.databind.JsonMappingException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import org.hibernate.HibernateException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.server.ResponseStatusException

fun <T> Boolean?.ifTrue(block: (Boolean) -> T?): T? = if (this == true) block(this) else null
private val LOGGER = KotlinLogging.logger {}

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Suppress("unused")
class DefaultExceptionHandler {

    @ResponseBody
    @ExceptionHandler(HibernateException::class)
    fun handleHibernateException(e: HibernateException): ResponseEntity<*> {
        LOGGER.error(e) { "Feil ved kommunikasjon med databasen. ${e.message}" }
        return ResponseEntity("Feil ved kommunikasjon med databasen. ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR)
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
        return ResponseEntity.status(exception.statusCode)
            .header(HttpHeaders.WARNING, feilmelding)
            .body(payloadFeilmelding)
    }

    @ResponseBody
    @ExceptionHandler(
        ResponseStatusException::class,
    )
    fun handleResponseStatusExceptions(e: ResponseStatusException): ResponseEntity<*> {
        LOGGER.warn { "Feil ved hent av grunnlag: ${e.message}" }
        val feilmelding = "Hent av grunnlag feilet!"
        val headers = HttpHeaders()
        headers.add(HttpHeaders.WARNING, feilmelding)
        return ResponseEntity.status(e.statusCode).body(ResponseEntity(e.message, headers, e.statusCode))
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.error(exception) { "Det skjedde en ukjent feil: ${exception.message}" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .body(exception.message ?: "Ukjent feil")
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()

        exception.responseHeaders?.get(HttpHeaders.WARNING)?.firstOrNull()
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

    data class Error(val status: Int, val message: String, val fieldErrors: MutableList<CustomFieldError> = mutableListOf()) {
        fun addFieldError(objectName: String, field: String, message: String) {
            val error = CustomFieldError(objectName, field, message)
            fieldErrors.add(error)
        }
    }

    data class CustomFieldError(val objectName: String, val field: String, val message: String)
}
