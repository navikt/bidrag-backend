package no.nav.bidrag.grunnlag.consumer.bidragperson.api

import no.nav.bidrag.transport.person.PersonRequest
import java.time.LocalDate

data class HusstandsmedlemmerRequest(val personRequest: PersonRequest, val periodeFra: LocalDate?)
