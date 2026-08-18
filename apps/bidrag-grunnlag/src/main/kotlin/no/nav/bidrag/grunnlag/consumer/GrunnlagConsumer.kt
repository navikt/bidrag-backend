package no.nav.bidrag.grunnlag.consumer

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class GrunnlagConsumer {

    fun <T> logResponse(logger: Logger, restResponse: RestResponse<T>) {
        when (restResponse) {
            is RestResponse.Success -> logger.debug("Response: {}", HttpStatus.OK)
            is RestResponse.Failure -> logger.warn("Response: ${restResponse.statusCode}/${restResponse.message}")
        }
    }

    fun <T> logResponse(type: String, ident: String, fom: LocalDate?, tom: LocalDate?, restResponse: RestResponse<T>) {
        when (restResponse) {
            is RestResponse.Success -> {
                logger.info("Hent av grunnlag $type utført ok")
                secureLogger.info { "Hent av grunnlag $type for $ident for perioden $fom - $tom ga følgende respons: ${restResponse.body}" }
            }

            is RestResponse.Failure -> {
                if (type == "Skattegrunnlag") {
                    // Legger ikke ut noe hvis skattegrunnlag ikke er tilgjengelig ennå
                    if (restResponse.statusCode == HttpStatus.NOT_FOUND &&
                        inntektsårIkkeStøttet(restResponse.message)
                    ) {
                        logger.warn("Skattegrunnlag er ikke tilgjengelig ennå for personen")
                        secureLogger.warn { "Skattegrunnlag er ikke tilgjengelig ennå for $ident og perioden $fom - $tom" }

                        // Legger ut tom liste hvis det ikke finnes data
                    } else if (((restResponse.statusCode == HttpStatus.NOT_FOUND) && fantIkkeSkattegrunnlag(restResponse.message)) ||
                        (
                            (restResponse.statusCode == HttpStatus.INTERNAL_SERVER_ERROR) &&
                                fantIkkeSkattegrunnlag(restResponse.message)
                            )
                    ) {
                        logger.warn("Fant ikke skattegrunnlag for personen")
                        secureLogger.warn { "Fant ikke skattegrunnlag for $ident og perioden $fom - $tom" }
                    } else {
                        secureLogger.warn {
                            "Feil ved hent av skattegrunnlag for $ident for perioden $fom - $tom. " +
                                "${restResponse.statusCode}/${restResponse.message}"
                        }
                    }
                } else {
                    // Logger som warning i stedet for error hvis status er not found
                    if (restResponse.statusCode == HttpStatus.NOT_FOUND) {
                        logger.warn(
                            "Feil ved hent av grunnlag $type ${restResponse.statusCode}/${restResponse.message}",
                        )
                        secureLogger.warn {
                            "Feil ved hent av grunnlag $type for $ident for perioden $fom - $tom. " +
                                "${restResponse.statusCode}/${restResponse.message}"
                        }
                    } else {
                        logger.error(
                            "Feil ved hent av grunnlag $type ${restResponse.statusCode}/${restResponse.message}",
                        )
                        secureLogger.error {
                            "Feil ved hent av grunnlag $type for $ident for perioden $fom - $tom. " +
                                "${restResponse.statusCode}/${restResponse.message}"
                        }
                    }
                }
            }
        }
    }

    fun <T : Any> initHttpEntity(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, httpHeaders)
    }

    fun <T : Any> initHttpEntityInntektskomponenten(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.add(NAV_CALL_ID, UUID.randomUUID().toString())
        httpHeaders.add(NAV_CONSUMER_ID, NAV_CONSUMER_ID_VERDI)
        return HttpEntity(body, httpHeaders)
    }

    fun <T : Any> initHttpEntitySkattegrunnlag(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.add(NAV_CALL_ID, UUID.randomUUID().toString())
        httpHeaders.add(NAV_CONSUMER_ID, NAV_CONSUMER_ID_VERDI)
        return HttpEntity(body, httpHeaders)
    }

    fun <T : Any> initHttpEntityAareg(body: T, ident: String): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.add(NAV_CALL_ID, UUID.randomUUID().toString())
        httpHeaders.add(NAV_PERSONIDENT, ident)
        return HttpEntity(body, httpHeaders)
    }

    fun <T : Any> initHttpEntityEreg(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        httpHeaders.add(NAV_CALL_ID, UUID.randomUUID().toString())
        return HttpEntity(body, httpHeaders)
    }

    fun <T : Any> initHttpEntityNorgesBank(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
//        httpHeaders.add(NAV_CALL_ID, UUID.randomUUID().toString())
        return HttpEntity(body, httpHeaders)
    }

    private fun inntektsårIkkeStøttet(message: String?) = message?.contains(INNTEKTSAAR_IKKE_STØTTET) ?: false

    private fun fantIkkeSkattegrunnlag(message: String?): Boolean = message?.contains(FANT_IKKE_SKATTEGRUNNLAG_PROD) == true || message?.contains(FANT_IKKE_SKATTEGRUNNLAG_TEST) == true

    companion object {
        const val NAV_CALL_ID = "Nav-Call-Id"
        const val NAV_CONSUMER_ID = "Nav-Consumer-Id"
        const val NAV_CONSUMER_ID_VERDI = "BIDRAG-GRUNNLAG"
        const val NAV_PERSONIDENT = "Nav-Personident"
        const val INNTEKTSAAR_IKKE_STØTTET = "Oppgitt inntektsår er ikke støttet"
        const val FANT_IKKE_SKATTEGRUNNLAG_PROD = "Fant ikke summert skattegrunnlag"
        const val FANT_IKKE_SKATTEGRUNNLAG_TEST = "Det finnes ikke summertskattegrunnlag"

        @JvmStatic
        val logger: Logger = LoggerFactory.getLogger(BidragPersonConsumer::class.java)
    }
}
