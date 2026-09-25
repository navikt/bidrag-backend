package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.commons.service.organisasjon.EnhetProvider

fun lagSaksbehandlerInfo(saksbehandlerIdent: String?) = if (saksbehandlerIdent.isNullOrEmpty()) {
    "ikke valgt"
} else {
    hentBrukeridentMedSaksbehandler(
        saksbehandlerIdent,
    )
}

private fun hentBrukeridentMedSaksbehandler(saksbehandlerIdent: String) = "${EnhetProvider.hentSaksbehandlernavn(saksbehandlerIdent) ?: "Ukjent"} ${saksbehandlerIdent.let { "($it)" }}"
