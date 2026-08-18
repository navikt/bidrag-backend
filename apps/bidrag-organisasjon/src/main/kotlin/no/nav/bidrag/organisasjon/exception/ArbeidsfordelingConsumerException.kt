package no.nav.bidrag.organisasjon.exception

import org.springframework.http.HttpStatus

class ArbeidsfordelingConsumerException(melding: String?, val httpStatus: HttpStatus) : RuntimeException(melding)
