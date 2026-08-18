package no.nav.bidrag.sak.aop

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientResponseException

@RestControllerAdvice
class RestResponseExceptionResolver {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException::class)
    protected fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<*> = loggOgFeilmeld(e, HttpStatus.BAD_REQUEST)

    @ResponseBody
    @ExceptionHandler(IllegalStateException::class)
    protected fun handleIllegalIllegalStateException(e: IllegalStateException): ResponseEntity<*> = loggOgFeilmeld(e, HttpStatus.INTERNAL_SERVER_ERROR)

    @ResponseBody
    @ExceptionHandler(RestClientResponseException::class)
    protected fun handleHttpClientErrorException(e: HttpStatusCodeException): ResponseEntity<*> = loggOgFeilmeld(e, HttpStatus.valueOf(e.statusCode.value()))

    @ResponseBody
    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<*> {
        logger.error("Feil [${e.javaClass.name}] inntraff med detaljer", e)
        val feilmelding = e.message ?: "Restkall feilet!"
        val headers = HttpHeaders().apply { add(HttpHeaders.WARNING, feilmelding) }
        return ResponseEntity(feilmelding, headers, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun loggOgFeilmeld(
        e: Exception,
        httpStatus: HttpStatus,
    ): ResponseEntity<String> {
        logger.error("Feil [${e.javaClass.name}] inntraff med detaljer", e)
        val feilmelding = e.message ?: "Restkall feilet!"
        val headers = HttpHeaders().apply { add(HttpHeaders.WARNING, feilmelding) }
        return ResponseEntity(feilmelding, headers, httpStatus)
    }
}
