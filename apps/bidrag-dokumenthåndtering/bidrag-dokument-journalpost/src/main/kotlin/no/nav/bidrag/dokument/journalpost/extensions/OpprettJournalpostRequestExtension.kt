package no.nav.bidrag.dokument.journalpost.extensions

import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest

fun OpprettJournalpostRequest.hentJournalførendeEnhet(): String? = journalførendeEnhet ?: journalfoerendeEnhet

fun OpprettJournalpostRequest.hentGjelderIdent(): String? = gjelderIdent ?: gjelder?.ident

val OpprettJournalpostRequest.erUtgående get() = journalposttype == JournalpostType.UTGÅENDE || journalposttype == JournalpostType.UTGAAENDE
