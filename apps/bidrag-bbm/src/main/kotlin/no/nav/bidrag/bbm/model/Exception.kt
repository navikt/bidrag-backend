package no.nav.bidrag.bbm.model

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

fun søknadIkkeFunnet(søknadsid: String?): Nothing = throw HttpClientErrorException(HttpStatus.NOT_FOUND, "Fant ikke søknad med id $søknadsid")
