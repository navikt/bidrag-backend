package no.nav.bidrag.dokument.journalpost.aop

import no.nav.bidrag.dokument.journalpost.dto.Violation
import no.nav.bidrag.dokument.journalpost.exception.AvvikDetaljException
import no.nav.bidrag.dokument.journalpost.exception.CharacterOverflowException
import no.nav.bidrag.dokument.journalpost.exception.DokumentetErIkkePdfException
import no.nav.bidrag.dokument.journalpost.exception.HentingAvDokumentFeiletException
import no.nav.bidrag.dokument.journalpost.exception.HttpStatusException
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.journalpost.exception.KanIkkeHenteDokumentUnderProduksjon
import no.nav.bidrag.dokument.journalpost.exception.OppgaveIkkeOpprettetException
import no.nav.bidrag.dokument.journalpost.exception.SakIkkeTilknyttetJournalpostException
import no.nav.bidrag.dokument.journalpost.exception.SaksnummerManglerException
import no.nav.bidrag.dokument.journalpost.exception.UgyldigBrevkodeException
import no.nav.bidrag.dokument.journalpost.exception.ViolationException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException

@RestControllerAdvice
class ExceptionHandlerAdvice {
    @ResponseBody
    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    fun jsonParseException(exception: HttpMessageNotReadableException): ResponseEntity<*> = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, "Ugyldig json input ${exception.message}")
        .build<Any>()

    @ResponseBody
    @ExceptionHandler(value = [UgyldigBrevkodeException::class])
    fun handleInvalidInputException(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(
        value = [KanIkkeHenteDokumentUnderProduksjon::class, DokumentetErIkkePdfException::class, HentingAvDokumentFeiletException::class],
    )
    fun handleDokumentExceptions(exception: Exception): ResponseEntity<*> = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, exception.message ?: "")
        .build<Any>()

    @ResponseBody
    @ExceptionHandler
    fun handleHttpStatusException(httpStatusException: HttpStatusException): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(httpStatusException.status, httpStatusException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttClientErrorException(httpClientErrorException: HttpClientErrorException): ResponseEntity<*> = ResponseEntity
        .status(httpClientErrorException.statusCode)
        .header(HttpHeaders.WARNING, httpClientErrorException.message ?: "")
        .build<Any>()

    @ResponseBody
    @ExceptionHandler
    fun handleSaksnummerManglerException(saksnummerManglerException: SaksnummerManglerException): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(HttpStatus.BAD_REQUEST, saksnummerManglerException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleJournalpostIkkeFunnetException(journalpostIkkeFunnetException: JournalpostIkkeFunnetException): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(HttpStatus.NOT_FOUND, journalpostIkkeFunnetException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleSakIkkeTilknyttetJournalpostException(
        sakIkkeTilknyttetJournalpostException: SakIkkeTilknyttetJournalpostException,
    ): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(HttpStatus.NOT_FOUND, sakIkkeTilknyttetJournalpostException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleOppgaveIkkeOpprettetException(oppgaveIkkeOpprettetException: OppgaveIkkeOpprettetException): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR, oppgaveIkkeOpprettetException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleViolationException(violationException: ViolationException): ResponseEntity<List<Violation>> {
        val responseBuilder = ResponseBuilder(HttpStatus.BAD_REQUEST, violationException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .body(violationException.violations)
    }

    @ResponseBody
    @ExceptionHandler
    fun handleCharacterOverflowException(characterOverflowException: CharacterOverflowException): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(HttpStatus.BAD_REQUEST, characterOverflowException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleAvvikDetaljException(avvikDetaljException: AvvikDetaljException): ResponseEntity<*> {
        val responseBuilder = ResponseBuilder(HttpStatus.BAD_REQUEST, avvikDetaljException)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build<Any>()
    }

    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
    protected fun handeUnauthorized(ex: JwtTokenUnauthorizedException): ResponseEntity<Any> {
        val responseBuilder = ResponseBuilder(HttpStatus.UNAUTHORIZED, ex)
        return ResponseEntity
            .status(responseBuilder.fetchStatus())
            .header(HttpHeaders.WARNING, responseBuilder.warningMessage)
            .build()
    }

    private class ResponseBuilder(
        private val httpStatus: HttpStatus,
        runtimeException: RuntimeException,
    ) {
        val warningMessage: String

        init {
            warningMessage = String.format("%s: %s", runtimeException.javaClass.simpleName, runtimeException.message)
        }

        fun fetchStatus(): HttpStatus {
            LOGGER.info("Status {}: {}", httpStatus, warningMessage)
            return httpStatus
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ExceptionHandlerAdvice::class.java)
    }
}
