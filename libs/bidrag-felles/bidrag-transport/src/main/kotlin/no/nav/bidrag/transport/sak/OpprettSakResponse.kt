package no.nav.bidrag.transport.sak

import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer

data class OpprettSakResponse(
    val saksnummer: Saksnummer,
    val eierfogd: Enhetsnummer,
)
