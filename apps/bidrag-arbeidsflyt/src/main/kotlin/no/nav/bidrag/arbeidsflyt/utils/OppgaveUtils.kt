package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.commons.service.organisasjon.EnhetProvider

fun lagSaksbehandlerInfo(saksbehandlerIdent: String?, saksbehandlerEnhet: String?) = if (saksbehandlerIdent.isNullOrEmpty()) {
    "ikke valgt"
} else {
    hentBrukeridentMedSaksbehandler(
        saksbehandlerIdent,
        saksbehandlerEnhet ?: "Ukjent enhet",
    )
}

private fun hentBrukeridentMedSaksbehandler(saksbehandlerIdent: String, enhetsnummer: String) = "${EnhetProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: "Ukjent"} ${saksbehandlerIdent.let { "($it, $enhetsnummer)" }}"
