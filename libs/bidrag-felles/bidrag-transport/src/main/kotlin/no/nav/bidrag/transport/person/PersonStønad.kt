package no.nav.bidrag.transport.person

import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.dokument.forsendelse.PersonIdent

// Brukes for å identifisere søknadsbarn basert på ident og stønadstype og ikke bare ident
// Dette er nyttig når det gjøres vurdering på samme barn i samme søknad (feks i FF) for både 18 år og ordinær bidrag
// Da må barnet kunne identifiseres basert på både ident og stønadstype (18år vs ordinær bidrag)
data class PersonStønad(
    val ident: PersonIdent,
    val stønadstype: Stønadstype? = null,
)
