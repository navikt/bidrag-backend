package no.nav.bidrag.admin.utils

import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.service.organisasjon.EnhetProvider
import no.nav.bidrag.commons.service.organisasjon.SaksbehandlernavnProvider

fun hentBrukerIdent() = TokenUtils.hentSaksbehandlerIdent() ?: TokenUtils.hentApplikasjonsnavn() ?: "Ukjent"

fun hentBrukerNavn() = TokenUtils.hentSaksbehandlerIdent()?.let { EnhetProvider.hentSaksbehandlernavn(it) }
    ?: TokenUtils.hentApplikasjonsnavn() ?: "Ukjent"
