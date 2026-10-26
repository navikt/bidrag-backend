package no.nav.bidrag.rest.exceptions

class RessursIkkeFunnetException(ressurs: String) : RuntimeException("$ressurs ikke funnet")
