package no.nav.bidrag.dokument.journalpost.model

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.Sporingsdata

fun initJournalpostHendelse(journalpostIntern: JournalpostIntern) = JournalpostHendelse(
    journalpostId = journalpostIntern.journalpostId ?: "-1",
    fagomrade = journalpostIntern.fagomrade,
    enhet = journalpostIntern.journalforendeEnhet,
    journalstatus = journalpostIntern.journalstatus,
    sporing = Sporingsdata(correlationId = CorrelationId.fetchCorrelationIdForThread()),
)
