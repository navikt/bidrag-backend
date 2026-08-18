package no.nav.bidrag.person.model

import org.springframework.http.HttpStatus

abstract class HttpStatusException(message: String) : RuntimeException(message) {
    abstract val status: HttpStatus
}

class PdlException(message: String, override val status: HttpStatus) : HttpStatusException(message)

class TokenException(message: String) : RuntimeException(message)

class PersonIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NO_CONTENT
}

class PersonAdresseIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NOT_FOUND
}

class BidragPersonFunctionalException(override var message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.BAD_REQUEST
}
