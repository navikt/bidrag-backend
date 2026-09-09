package no.nav.bidrag.henvendelse.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val log = KotlinLogging.logger {}

/**
 * Feil svares ut som ProblemDetail (RFC 7807). Frontend har egen håndtering av formatet, se
 * `packages/api/src/ProblemDetail.ts` i bidrag-frontend.
 *
 * Tre ting er verdt å merke seg:
 *
 * Klassen arver [ResponseEntityExceptionHandler] framfor å ta `@ExceptionHandler(Exception::class)`
 * alene. Et rent catch-all her registreres før Spring Boots egen `ProblemDetailsExceptionHandler`
 * og overskygger den, slik at ugyldig request-body, feil HTTP-metode og ukjent path alle ble 500
 * istedenfor 400/405/404.
 *
 * Ingen tekst fra exceptions legges i `detail`. Meldingene fra AbstractRestClient og Spring
 * inneholder den kallede URL-en - som hos oss har `?aktorid=...` - og noen av Spring sine egne
 * meldinger siterer verdier fra forespørselen. Derfor overstyres også [handleExceptionInternal],
 * slik at *alle* de arvede handlerne får en fast detaljtekst. Detaljene hører i loggen.
 *
 * Catch-allen ser på årsakskjeden. AbstractRestClient pakker alt som ikke er en
 * `RestClientResponseException` i en `RuntimeException`, og Spring finner handler ut fra
 * exception-klassen som faktisk kastes - ikke årsaken. Uten utpakkingen ble timeout og feilet
 * token-veksling 500 "Ukjent feil" istedenfor 502, og [handleResourceAccessException] var
 * død kode.
 */
@RestControllerAdvice
class DefaultRestControllerAdvice : ResponseEntityExceptionHandler() {
    /**
     * Feil fra tjenestene vi kaller skal ikke lekke ut som vår egen status. En 403 fra
     * sf-henvendelse-api-proxy betyr at *appen* mangler tilgang, og ville ellers fått frontend
     * til å fortelle saksbehandleren at hun ikke har tilgang til personen.
     *
     * Tar `RestClientResponseException` og ikke `HttpStatusCodeException`, siden det er den
     * førstnevnte AbstractRestClient kaster videre - `UnknownHttpStatusCodeException` (ikke-
     * standard statuskode) er også en av dem.
     */
    @ExceptionHandler(RestClientResponseException::class)
    fun handleRestClientResponseException(exception: RestClientResponseException): ProblemDetail {
        log.warn(exception) { "Feil ved kall mot ekstern tjeneste, status ${exception.statusCode}" }
        return problemDetail(
            status = HttpStatus.BAD_GATEWAY,
            tittel = "Feil ved kall mot tjeneste",
            detalj = "Kunne ikke hente henvendelser fordi en tjeneste vi er avhengig av svarte med feil.",
        )
    }

    /** Timeout, brutt forbindelse, DNS-feil. */
    @ExceptionHandler(ResourceAccessException::class)
    fun handleResourceAccessException(exception: ResourceAccessException): ProblemDetail {
        log.warn(exception) { "Fikk ikke kontakt med ekstern tjeneste" }
        return problemDetail(
            status = HttpStatus.BAD_GATEWAY,
            tittel = "Tjenesten svarte ikke",
            detalj = "Kunne ikke hente henvendelser fordi en tjeneste vi er avhengig av ikke svarte.",
        )
    }

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ProblemDetail {
        log.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return problemDetail(
            status = HttpStatus.UNAUTHORIZED,
            tittel = "Autentiseringsfeil",
            detalj = "Ugyldig eller manglende sikkerhetstoken.",
        )
    }

    @ExceptionHandler(UgyldigIdentException::class)
    fun handleUgyldigIdent(exception: UgyldigIdentException): ProblemDetail {
        log.warn { "Fikk forespørsel med ugyldig ident" }
        return problemDetail(
            status = HttpStatus.BAD_REQUEST,
            tittel = "Ugyldig ident",
            detalj = "Identen i forespørselen er ikke et gyldig fødselsnummer eller d-nummer.",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUkjentFeil(exception: Exception): ProblemDetail {
        årsakskjede(exception).forEach { årsak ->
            when (årsak) {
                is ResourceAccessException -> return handleResourceAccessException(årsak)
                is RestClientResponseException -> return handleRestClientResponseException(årsak)
                else -> Unit
            }
        }

        log.error(exception) { "Det skjedde en ukjent feil" }
        return problemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            tittel = "Ukjent feil",
            detalj = "Det skjedde en uventet feil. Feilen er logget.",
        )
    }

    /**
     * Alle de arvede handlerne går gjennom her. Vi beholder statuskoden Spring har valgt, men
     * bytter ut detaljteksten: flere av dem siterer forespørselen, for eksempel
     * "Failed to convert 'x' with value: '...'", og da kan en ident havne i responsen.
     */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.warn(ex) { "Avviste forespørsel med status $statusCode" }
        val problem = problemDetail(
            status = statusCode,
            tittel = "Forespørselen kunne ikke behandles",
            detalj = "Forespørselen kunne ikke behandles. Se loggen for detaljer.",
        )
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request)
    }

    private fun problemDetail(
        status: HttpStatusCode,
        tittel: String,
        detalj: String,
    ) = ProblemDetail.forStatusAndDetail(status, detalj).apply { title = tittel }

    /** Exception-en selv og årsakene under, med tak for å tåle sykliske kjeder. */
    private fun årsakskjede(exception: Throwable) = generateSequence(exception) { it.cause }.take(10)
}

/** Kastes når identen i forespørselen ikke er en gyldig personident. */
class UgyldigIdentException : RuntimeException("Ugyldig personident")
