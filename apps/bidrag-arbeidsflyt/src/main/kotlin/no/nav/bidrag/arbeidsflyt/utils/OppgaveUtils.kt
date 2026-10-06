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
): String {
    val enhet = enhetsnummer ?: "ukjent enhet"
    return if (saksbehandlerIdent.isNullOrEmpty()) {
        if (saksbehandlerNavn.isNullOrEmpty()) "ukjent saksbehandler ($enhet)" else "$saksbehandlerNavn ($enhet)"
    } else {
        val navn = saksbehandlerNavn ?: hentSaksbehandlernavn(saksbehandlerIdent) ?: "Ukjent $saksbehandlerIdent"
        "$navn ($saksbehandlerIdent, $enhet)"
    }
}

fun JournalpostHendelse.lagSaksbehandlerInfoMedEnhet(): String = lagSaksbehandlerInfoMedEnhet(sporing?.brukerident, sporing?.enhetsnummer, sporing?.saksbehandlersNavn)

private fun hentSaksbehandlernavn(saksbehandlerIdent: String): String? = EnhetProvider.hentSaksbehandlernavn(saksbehandlerIdent)?.takeIf { it.isNotEmpty() }
