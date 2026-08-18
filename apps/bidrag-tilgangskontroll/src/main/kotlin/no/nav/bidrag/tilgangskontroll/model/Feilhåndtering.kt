package no.nav.bidrag.tilgangskontroll.model

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

fun sakIkkeFunnet(message: String): Nothing = throw HttpClientErrorException(HttpStatus.NOT_FOUND, message)
