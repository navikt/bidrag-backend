package no.nav.bidrag.person.bo

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.query.Bostedsadresse
import java.time.LocalDate

data class BarnBostedsadresserBo(
    val personId: Personident,
    var navn: String?,
    var fødselsdato: LocalDate?,
    var dødsdato: LocalDate?,
    val bostedsadresseListe: List<Bostedsadresse> = emptyList(),
)
