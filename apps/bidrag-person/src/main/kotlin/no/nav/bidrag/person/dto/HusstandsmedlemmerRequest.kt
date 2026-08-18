package no.nav.bidrag.person.dto

import no.nav.bidrag.transport.person.PersonRequest
import java.time.LocalDate

data class HusstandsmedlemmerRequest(val personRequest: PersonRequest, val periodeFra: LocalDate?)
