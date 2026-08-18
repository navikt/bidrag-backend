package no.nav.bidrag.reskontro.consumer

import no.nav.bidrag.commons.security.maskinporten.MaskinportenClientException
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.reskontro.dto.consumer.ReskontroConsumerInput
import no.nav.bidrag.reskontro.dto.consumer.ReskontroConsumerOutput
import no.nav.bidrag.reskontro.exceptions.FeilMotSkattException
import no.nav.bidrag.reskontro.exceptions.IngenDataFraSkattException
import no.nav.bidrag.reskontro.exceptions.TimeoutFraSkattException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import org.springframework.web.client.postForEntity
import java.net.URI

@Service
class SkattReskontroConsumer(
    @param:Value($$"${SKATT_URL}") private val skattUrl: String,
    @param:Qualifier("maskinporten") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-reskontro") {
    companion object {
        const val BIDRAGSSAK_PATH = "/BisysResk/bidragssak"
        const val TRANSAKSJONER_PATH = "/BisysResk/transaksjoner"
        const val INNKREVINGSSAK_PATH = "/BisysResk/innkrevingssak"
        const val ENDRE_RM_PATH = "/BisysResk/endrerm"
    }

    fun hentInnkrevningssakerPåSak(saksnummer: Long): ReskontroConsumerOutput {
        try {
            val response = restTemplate.postForEntity<ReskontroConsumerOutput>(
                URI.create(skattUrl + BIDRAGSSAK_PATH),
                ReskontroConsumerInput(aksjonskode = 1, bidragssaksnummer = saksnummer),
            )
            secureLogger.debug { "Kaller hent bidragssak for sak: $saksnummer.\nResponse fra skatt: $response." }
            return validerOutput(response)
        } catch (e: ResourceAccessException) {
            loggOgKastSkattTimeoutException("Timeout ved kall på hent bidragssaker for sak: $saksnummer. ${e.message}", e)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                loggOgKastSkattIngenDataException("Fant ingen bidragssak for sak: $saksnummer.")
            } else {
                loggOgKastException("Feil ved kall på hent bidragssaker for sak: $saksnummer. ${e.message}.", e)
            }
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                loggOgKastSkattTimeoutException("Gateway-timeout ved kall på hent bidragssaker for sak: $saksnummer. ${e.message}", e)
            } else {
                loggOgKastException("Ukjent server-feil ved kall på hent bidragssaker for sak: $saksnummer. ${e.message}.", e)
            }
        }
    }

    private fun loggOgKastSkattTimeoutException(
        melding: String?,
        e: Exception,
    ): Nothing {
        secureLogger.error(e) { melding }
        throw TimeoutFraSkattException(melding)
    }

    private fun loggOgKastSkattIngenDataException(
        melding: String?,
    ): Nothing {
        secureLogger.info { melding }
        throw IngenDataFraSkattException(melding)
    }

    private fun loggOgKastException(
        melding: String?,
        e: Exception,
    ): Nothing {
        secureLogger.error(e) { melding }
        throw FeilMotSkattException(melding, e.cause)
    }

    fun hentInnkrevningssakerPåPerson(person: Personident): ReskontroConsumerOutput {
        try {
            val response = restTemplate.postForEntity<ReskontroConsumerOutput>(
                URI.create(skattUrl + BIDRAGSSAK_PATH),
                ReskontroConsumerInput(aksjonskode = 2, fodselsOrgnr = person.verdi),
            )
            secureLogger.debug { "Kaller hent bidragssaker for person: ${person.verdi}. Response fra skatt: $response. " }
            return validerOutput(response)
        } catch (e: ResourceAccessException) {
            loggOgKastSkattTimeoutException("Timeout ved kall på hent bidragssaker for person: ${person.verdi}. ${e.message}", e)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                loggOgKastSkattIngenDataException("Fant ingen bidragssaker for person: ${person.verdi}.")
            } else {
                loggOgKastException("Feil ved kall på hent bidragssaker for person: ${person.verdi}. ${e.message}.", e)
            }
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                loggOgKastSkattTimeoutException("Gateway-timeout ved kall på hent bidragssaker for person: ${person.verdi}. ${e.message}", e)
            } else {
                loggOgKastException(
                    "Ukjent server-feil ved kall på hent bidragssaker for person: ${person.verdi}. ${e.message}.",
                    e,
                )
            }
        }
    }

    fun hentTransaksjonerPåBidragssak(saksnummer: Long): ReskontroConsumerOutput {
        try {
            val response = restTemplate.postForEntity<ReskontroConsumerOutput>(
                URI.create(skattUrl + TRANSAKSJONER_PATH),
                ReskontroConsumerInput(
                    aksjonskode = 3,
                    bidragssaksnummer = saksnummer,
                    datoFom = "1900-01-01T00:00:00.000Z",
                    datoTom = "9999-01-01T00:00:00.000Z",
                    maxAntallTransaksjoner = Int.MAX_VALUE,
                ),
            )
            secureLogger.debug { "Kaller hent transaksjoner for sak: $saksnummer. Response fra skatt: $response." }
            return validerOutput(response)
        } catch (e: ResourceAccessException) {
            loggOgKastSkattTimeoutException("Timeout ved kall på hent transaksjoner for sak: $saksnummer. ${e.message}", e)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                loggOgKastSkattIngenDataException("Fant ingen transaksjoner for sak: $saksnummer.")
            } else {
                loggOgKastException("Feil ved kall på hent transaksjoner for sak: $saksnummer. ${e.message}.", e)
            }
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                loggOgKastSkattTimeoutException("Gateway-timeout ved kall på hent transaksjoner for sak: $saksnummer. ${e.message}", e)
            } else {
                loggOgKastException("Ukjent server-feil ved kall på hent transaksjoner for sak: $saksnummer. ${e.message}.", e)
            }
        }
    }

    fun hentTransaksjonerPåPerson(person: Personident): ReskontroConsumerOutput {
        try {
            val response = restTemplate.postForEntity<ReskontroConsumerOutput>(
                URI.create(skattUrl + TRANSAKSJONER_PATH),
                ReskontroConsumerInput(
                    aksjonskode = 4,
                    fodselsOrgnr = person.verdi,
                    datoFom = "1900-01-01T00:00:00.000Z",
                    datoTom = "9999-01-01T00:00:00.000Z",
                    maxAntallTransaksjoner = Int.MAX_VALUE,
                ),
            )
            secureLogger.debug { "Kaller hent transaksjoner for person: ${person.verdi}. Response fra skatt: $response." }
            return validerOutput(response)
        } catch (e: ResourceAccessException) {
            loggOgKastSkattTimeoutException("Timeout ved kall på hent transaksjoner for person: ${person.verdi}. ${e.message}", e)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                loggOgKastSkattIngenDataException("Fant ingen transaksjoner for person: ${person.verdi}.")
            } else {
                loggOgKastException("Feil ved kall på hent transaksjoner for person: ${person.verdi}. ${e.message}.", e)
            }
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                loggOgKastSkattTimeoutException("Gateway-timeout ved kall på hent transaksjoner for person: ${person.verdi}. ${e.message}", e)
            } else {
                loggOgKastException(
                    "Ukjent server-feil ved kall på hent transaksjoner for person: ${person.verdi}. ${e.message}.",
                    e,
                )
            }
        }
    }

    fun hentTransaksjonerPåTransaksjonsId(transaksjonsid: Long): ReskontroConsumerOutput {
        try {
            val response = restTemplate.postForEntity<ReskontroConsumerOutput>(
                URI.create(skattUrl + TRANSAKSJONER_PATH),
                ReskontroConsumerInput(
                    aksjonskode = 5,
                    transaksjonsId = transaksjonsid,
                    datoFom = "1900-01-01T00:00:00.000Z",
                    datoTom = "9999-01-01T00:00:00.000Z",
                ),
            )
            secureLogger.debug { "Kaller hent transaksjoner for transaksjonsId: $transaksjonsid. Response fra skatt: $response." }
            return validerOutput(response)
        } catch (e: ResourceAccessException) {
            loggOgKastSkattTimeoutException("Timeout ved kall på hent transaksjoner for transaksjonsId: $transaksjonsid. ${e.message}", e)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                loggOgKastSkattIngenDataException("Fant ingen transaksjon for transaksjonsId: $transaksjonsid.")
            } else {
                loggOgKastException(
                    "Feil ved kall på hent transaksjoner for transaksjonsId: $transaksjonsid. ${e.message}.",
                    e,
                )
            }
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                loggOgKastSkattTimeoutException("Gateway-timeout ved kall på hent transaksjoner for transaksjonsId: $transaksjonsid. ${e.message}", e)
            } else {
                loggOgKastException("Ukjent server-feil ved kall på hent transaksjoner for transaksjonsId: $transaksjonsid. ${e.message}.", e)
            }
        }
    }

    fun hentInformasjonOmInnkrevingssaken(person: Personident): ReskontroConsumerOutput {
        try {
            val response = restTemplate.postForEntity<ReskontroConsumerOutput>(
                URI.create(skattUrl + INNKREVINGSSAK_PATH),
                ReskontroConsumerInput(aksjonskode = 6, fodselsOrgnr = person.verdi),
            )
            secureLogger.debug { "Response på hentInformasjonOmInnkrevingssaken for person: ${person.verdi}.Response fra skatt: $response." }
            return validerOutput(response)
        } catch (e: ResourceAccessException) {
            loggOgKastSkattTimeoutException("Timeout ved kall på hentInformasjonOmInnkrevingssaken for person: ${person.verdi}. ${e.message}", e)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                loggOgKastSkattIngenDataException("Fant ingen hentInformasjonOmInnkrevingssaken for person: ${person.verdi}.")
            } else {
                loggOgKastException(
                    "Feil ved kall på hentInformasjonOmInnkrevingssaken for person: ${person.verdi}. ${e.message}.",
                    e,
                )
            }
        } catch (e: HttpServerErrorException) {
            if (e.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                loggOgKastSkattTimeoutException("Gateway-timeout ved kall på hentInformasjonOmInnkrevingssaken for person: ${person.verdi}. ${e.message}", e)
            } else {
                loggOgKastException(
                    "Ukjent server-feil ved kall på hentInformasjonOmInnkrevingssaken for person: ${person.verdi}. ${e.message}.",
                    e,
                )
            }
        }
    }

    fun endreRmForSak(saksnummer: Long, barn: Personident, nyRm: Personident): ReskontroConsumerOutput {
        val response = restTemplate.exchange<ReskontroConsumerOutput>(
            URI.create(skattUrl + ENDRE_RM_PATH),
            HttpMethod.PATCH,
            HttpEntity(
                ReskontroConsumerInput(
                    aksjonskode = 8,
                    bidragssaksnummer = saksnummer,
                    fodselsnrGjelder = barn.verdi,
                    fodselsnrNy = nyRm.verdi,
                ),
            ),
        )
        secureLogger.info { "Response på endre RM for sak: NyRM: ${nyRm.verdi} i sak $saksnummer med barn: ${barn.verdi}. Response fra skatt: $response." }
        return validerOutput(response)
    }

    /*
    Dette må gjøres siden det ikke returneres en korrekt HTTP statuskode i REST kallet mot Skatteetaten.
    Det blir derimot vedlagt en returnkode i responsen som avgjør om kallet var velykket eller ikke.
    Mulige returkoder er følgende:
        0  = OK
        -1 = Feilmelding
        -2 = Ugyldig aksjonskode - Aksjonskoden er satt individuelt for hvert endepunkt på vår side, burde ikke oppstå.
        -3 = Ingen data funnet - Tilsvarer 204 No Content.
     */
    private fun validerOutput(reskontroConsumerOutputResponse: ResponseEntity<ReskontroConsumerOutput>): ReskontroConsumerOutput {
        if (reskontroConsumerOutputResponse.statusCode == HttpStatus.UNAUTHORIZED) {
            throw MaskinportenClientException("Feil i maskinportentoken benyttet mot skatt")
        }
        if (reskontroConsumerOutputResponse.body == null) {
            error("Det mangler body i responsen fra Skatt!")
        }
        if (reskontroConsumerOutputResponse.body!!.retur == null) {
            error("Responsekoden mangler i responsen fra Skatt!")
        }

        when (reskontroConsumerOutputResponse.body?.retur?.kode) {
            0 -> return reskontroConsumerOutputResponse.body!!
            -1 -> error("Kallet mot skatt feilet med feilmelding: ${reskontroConsumerOutputResponse.body?.retur?.beskrivelse}")
            -2 -> error("Kallet mot skatt hadde ugyldig aksjonskode! Dette er ikke basert på innput og må rettes i koden/hos skatt.")
            -3 -> throw IngenDataFraSkattException("Skatt svarte med ingen data.")
            else -> error("Kallet mot skatt returnerte ukjent returnkode ${reskontroConsumerOutputResponse.body?.retur?.kode}")
        }
    }
}
