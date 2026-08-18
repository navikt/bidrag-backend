package no.nav.bidrag.dokument.arkivering.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.arkivering.exceptions.BidragDokumentArkiveringFunctionalException
import no.nav.bidrag.dokument.arkivering.exceptions.BidragDokumentArkiveringTechnicalException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.context.request.WebRequest

private val log = KotlinLogging.logger {}

@RestControllerAdvice
class ArkiveringControllerExceptionHandler {
    @ResponseBody
    @ExceptionHandler(value = [Exception::class])
    protected fun handleOtherException(e: Exception): ResponseEntity<Any> {
        val cause = e.cause
        val feilmelding = if (cause != null) cause.message else e.message
        log.error(e) { "$feilmelding" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Ukjent feil oppstod")
            .build()
    }

    @ResponseBody
    @ExceptionHandler(value = [HttpStatusCodeException::class])
    protected fun handleHttpStatusCodeException(e: HttpStatusCodeException): ResponseEntity<*> {
        log.warn(e) { getWarningHeader(e) }
        return ResponseEntity
            .status(e.statusCode)
            .header(HttpHeaders.WARNING, getWarningHeader(e) ?: "")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
    protected fun handeUnauthorized(
        e: JwtTokenUnauthorizedException?,
        request: WebRequest?,
    ): ResponseEntity<Any> {
        val feilmelding = "Ugyldig sikkerhetstoken!"
        log.warn(e) { feilmelding }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, feilmelding)
            .build()
    }

    @ResponseBody
    @ExceptionHandler(value = [IllegalArgumentException::class])
    protected fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<Any> {
        val cause = e.cause
        val feilmelding = if (cause != null) cause.message else e.message
        log.warn(e) { "$feilmelding" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, feilmelding ?: "")
            .build()
    }

    @ResponseBody
    @ExceptionHandler(value = [BidragDokumentArkiveringFunctionalException::class])
    protected fun handleBidragDokumentArkiveringFunctionalException(e: BidragDokumentArkiveringFunctionalException): ResponseEntity<Any> {
        val cause = e.cause
        val feilmelding = if (cause != null) cause.message else e.message
        log.warn(e) { "Det skjedde en funksjonell feil" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, feilmelding ?: "")
            .build()
    }

    @ResponseBody
    @ExceptionHandler(value = [BidragDokumentArkiveringTechnicalException::class])
    protected fun handleBidragDokumentArkiveringTechnicalException(e: BidragDokumentArkiveringTechnicalException): ResponseEntity<Any> {
        log.error(e) { "Det skjedde en teknisk feil" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, e.message ?: "")
            .build()
    }

    private fun getWarningHeader(httpClientErrorException: HttpStatusCodeException): String? {
        val message = httpClientErrorException.message
        if (httpClientErrorException.responseHeaders == null) {
            return message
        }
        val warningHeaders = httpClientErrorException.responseHeaders!![HttpHeaders.WARNING]
        return if (warningHeaders.isNullOrEmpty()) message else warningHeaders[0]
    }
}
