package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.commons.service.organisasjon.EnhetProvider
import no.nav.bidrag.transport.dokument.JournalpostHendelse

fun lagSaksbehandlerInfo(saksbehandlerIdent: String?): String = if (saksbehandlerIdent.isNullOrEmpty()) {
    "ikke valgt"
} else {
    hentSaksbehandlernavn(saksbehandlerIdent)?.let { "$it ($saksbehandlerIdent)" } ?: saksbehandlerIdent
}

fun lagSaksbehandlerInfoMedEnhet(
    saksbehandlerIdent: String?,
    enhetsnummer: String?,
    saksbehandlerNavn: String? = null,
): String = if (saksbehandlerIdent.isNullOrEmpty()) {
    "ukjent saksbehandler"
} else {
    val navn = saksbehandlerNavn ?: hentSaksbehandlernavn(saksbehandlerIdent) ?: "Ukjent $saksbehandlerIdent"
    "$navn ($saksbehandlerIdent, ${enhetsnummer ?: "ukjent enhet"})"
}

fun JournalpostHendelse.lagSaksbehandlerInfoMedEnhet(): String = lagSaksbehandlerInfoMedEnhet(sporing?.brukerident, hentEndretAvEnhetsnummer(), sporing?.saksbehandlersNavn)

private fun hentSaksbehandlernavn(saksbehandlerIdent: String): String? = EnhetProvider.hentSaksbehandlernavn(saksbehandlerIdent)?.takeIf { it.isNotEmpty() }
