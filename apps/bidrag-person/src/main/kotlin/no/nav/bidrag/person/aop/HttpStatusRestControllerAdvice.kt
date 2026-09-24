package no.nav.bidrag.person.aop

import com.fasterxml.jackson.databind.JsonMappingException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.person.model.HttpStatusException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import kotlin.collections.firstOrNull

@RestControllerAdvice
class HttpStatusRestControllerAdvice {

    companion object {
        private const val EXTERNAL_SERVICE_ERROR_PREFIX = "Det skjedde en feil ved kall mot ekstern tjeneste: "
        private val logger = KotlinLogging.logger {}
    }

    @ResponseBody
    @ExceptionHandler
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        logger.warn(exception) { "Det skjedde en ukjent feil" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttpStatusException(exception: HttpStatusException): ResponseEntity<*> {
        logger.warn(exception) { "Noe gikk galt i kall mot ekstern tjeneste." }
        return ResponseEntity
            .status(exception.status)
            .header(HttpHeaders.WARNING, exception.message ?: "Noe gikk galt i kall mot ekstern tjeneste.")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleJwtTokenUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        logger.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, exception.message ?: "Ugyldig eller manglende sikkerhetstoken")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttClientErrorException(exception: HttpClientErrorException): ResponseEntity<*> = ResponseEntity
        .status(exception.statusCode)
        .header(HttpHeaders.WARNING, "Http client says: " + exception.message)
        .build<Any>()

    @ResponseBody
    @ExceptionHandler
    fun handleMissingKotlinParameterException(exception: JsonMappingException): ResponseEntity<*> {
        logger.warn(exception) { "Noe gikk galt i jsonMapping." }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "Noe gikk galt i jsonMapping.")
            .build<Any>()
    }
}
