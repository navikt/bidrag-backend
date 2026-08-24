package no.nav.bidrag.dokument.bestilling.aop

import com.fasterxml.jackson.databind.JsonMappingException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.bestilling.model.FantIkkeEnhetException
import no.nav.bidrag.dokument.bestilling.model.FantIkkePersonException
import no.nav.bidrag.dokument.bestilling.model.ManglerGjelderException
import no.nav.bidrag.dokument.bestilling.model.ProduksjonAvDokumentStottesIkke
import no.nav.bidrag.dokument.bestilling.model.SamhandlerManglerKontaktinformasjon
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

private val LOGGER = KotlinLogging.logger {}

@RestControllerAdvice
class DefaultRestControllerAdvice {
    @ResponseBody
    @ExceptionHandler(
        value = [
            ManglerGjelderException::class,
            FantIkkeEnhetException::class,
            FantIkkePersonException::class,
            SamhandlerManglerKontaktinformasjon::class,
        ],
    )
    fun fantIkkeData(exception: RuntimeException): ResponseEntity<*> {
        LOGGER.warn { exception.message }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(value = [IllegalArgumentException::class, MethodArgumentTypeMismatchException::class, ConversionFailedException::class, HttpMessageNotReadableException::class])
    fun handleInvalidValueExceptions(exception: Exception): ResponseEntity<*> {
        val cause = exception.cause
        val valideringsFeil =
            if (cause is JsonMappingException) {
                createMissingKotlinParameterViolation(
                    cause,
                )
            } else {
                null
            }
        LOGGER.warn(
            exception,
        ) {
            "Forespørselen inneholder ugyldig verdi: ${valideringsFeil ?: "ukjent feil"}"
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(
                HttpHeaders.WARNING,
                "Forespørselen inneholder ugyldig verdi: ${valideringsFeil ?: exception.message}",
            ).build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(ProduksjonAvDokumentStottesIkke::class)
    fun ustottetBrev(exception: ProduksjonAvDokumentStottesIkke): ResponseEntity<*> {
        LOGGER.warn { exception.message }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val errorMessage = getErrorMessage(exception)
        LOGGER.error(exception) { errorMessage }
        return ResponseEntity
            .status(exception.statusCode)
            .header(HttpHeaders.WARNING, errorMessage)
            .build<Any>()
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        errorMessage.append("Det skjedde en feil: ")
        exception.responseHeaders
            ?.get("Warning")
            ?.let { if (it.size > 0) errorMessage.append(it[0]) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(" - ")
            errorMessage.append(exception.statusText)
        }
        return errorMessage.toString()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        LOGGER.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, "Ugyldig eller manglende sikkerhetstoken")
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

    private fun createMissingKotlinParameterViolation(ex: JsonMappingException): String {
        val errorFieldRegex = Regex("\\.([^.]*)\\[\\\"(.*)\"\\]\$")
        val paths =
            ex.path.map { errorFieldRegex.find(it.description)!! }.map {
                val (objectName, field) = it.destructured
                "$objectName.$field"
            }
        return "${paths.joinToString("->")} kan ikke være null"
    }
}
