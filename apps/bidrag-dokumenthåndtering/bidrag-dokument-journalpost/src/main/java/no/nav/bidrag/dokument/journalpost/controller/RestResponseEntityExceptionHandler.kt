package no.nav.bidrag.dokument.journalpost.controller

import no.nav.bidrag.commons.ExceptionLogger
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.WebUtil.initHttpHeadersWith
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler::class.java)
    }

    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
    protected fun handeUnauthorized(
        ex: JwtTokenUnauthorizedException?,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val message = "Ugyldig sikkerhetstoken"
        secureLogger.error(ex) { message }
        return handleExceptionInternal(ex!!, message, initHttpHeadersWith(HttpHeaders.WARNING, message), HttpStatus.UNAUTHORIZED, request)
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(errorMessage, exception)
        return ResponseEntity
            .status(exception.statusCode)
            .header(HttpHeaders.WARNING, errorMessage)
            .build<Any>()
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        if (exception.responseHeaders != null) {
            errorMessage.append("Det skjedde en feil ved kall mot ekstern tjeneste: ")
            exception.responseHeaders
                ?.get("Warning")
                ?.let { if (it.size > 0) errorMessage.append(it[0]) }
            errorMessage.append(" - ")
        }

        if (!exception.statusText.isNullOrEmpty()) {
            errorMessage.append(exception.statusText)
        }
        return errorMessage.toString()
    }
}
