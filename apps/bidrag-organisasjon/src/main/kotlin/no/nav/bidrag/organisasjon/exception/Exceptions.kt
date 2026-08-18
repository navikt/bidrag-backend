package no.nav.bidrag.organisasjon.exception

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class HentEnhetKontaktinformasjonFeiletException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

class EnhetIkkeFunnetException(message: String, throwable: Throwable? = null) : RuntimeException(message, throwable)

fun ikkeFunnet(message: String): Nothing = throw HttpClientErrorException(HttpStatus.NOT_FOUND, message)
