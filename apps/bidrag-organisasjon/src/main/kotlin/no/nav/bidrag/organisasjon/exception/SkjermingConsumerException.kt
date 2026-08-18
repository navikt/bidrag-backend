package no.nav.bidrag.organisasjon.exception

import org.springframework.http.HttpStatus

class SkjermingConsumerException(melding: String?, val httpStatus: HttpStatus) : RuntimeException(melding)
