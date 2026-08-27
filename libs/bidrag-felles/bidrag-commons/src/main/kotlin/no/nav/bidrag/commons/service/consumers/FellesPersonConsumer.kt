package no.nav.bidrag.commons.service.consumers

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import java.time.LocalDate

interface FellesPersonConsumer {
    fun hentPerson(personident: Personident): PersonDto

    fun hentFødselsdatoForPerson(personident: Personident): LocalDate?
}
