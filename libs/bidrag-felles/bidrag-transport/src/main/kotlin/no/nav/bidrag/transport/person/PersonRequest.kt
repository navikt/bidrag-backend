package no.nav.bidrag.transport.person

import no.nav.bidrag.domene.ident.Personident

data class PersonRequest(
    val ident: Personident,
)
