package no.nav.bidrag.dokument.journalpost.dto

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import no.nav.bidrag.dokument.journalpost.exception.ViolationException
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import org.junit.jupiter.api.Test

internal class OpprettUtgaaendeJournalpostInternTest {
    @Test
    fun shouldFailWhenJournalforendeenhetIsEmpty() {
        val jp =
            OpprettJournalpostRequest(
                avsenderMottaker = AvsenderMottakerDto(ident = "123123213"),
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = null,
                gjelder = AktorDto("1321312312"),
                dokumenter = listOf(OpprettDokumentDto("Tittel på dokument", brevkode = "BI099")),
                tilknyttSaker = listOf("12312321"),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "JournalfoerendeEnhet kan ikke være tom"
    }

    @Test
    fun shouldFailWhenNoDocuments() {
        val jp =
            OpprettJournalpostRequest(
                avsenderMottaker = AvsenderMottakerDto(ident = "123123213"),
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = "4806",
                gjelder = AktorDto("1321312312"),
                dokumenter = emptyList(),
                tilknyttSaker = listOf("12312321"),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "Journalpost må knyttes til et dokument"
    }

    @Test
    fun shouldFailWhenMoreThanOneDocument() {
        val jp =
            OpprettJournalpostRequest(
                avsenderMottaker = AvsenderMottakerDto(ident = "123123213"),
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = "4806",
                gjelder = AktorDto("1321312312"),
                dokumenter =
                listOf(
                    OpprettDokumentDto("Tittel på dokument", brevkode = "BI099"),
                    OpprettDokumentDto("Tittel 2 på dokument", brevkode = "BI099"),
                ),
                tilknyttSaker = listOf("12312321"),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "Midlertidig brevlager støtter bare et dokument per journalpost"
    }

    @Test
    fun shouldFailWhenDocumentsHasNoTitle() {
        val jp =
            OpprettJournalpostRequest(
                avsenderMottaker = AvsenderMottakerDto(ident = "123123213"),
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = "4806",
                gjelder = AktorDto("1321312312"),
                dokumenter = listOf(OpprettDokumentDto("", brevkode = "BI099")),
                tilknyttSaker = listOf("12312321"),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "Dokumentet journalpost knyttes må ha satt tittel"
    }

    @Test
    fun shouldFailWhenMissingSak() {
        val jp =
            OpprettJournalpostRequest(
                avsenderMottaker = AvsenderMottakerDto(ident = "123123213"),
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = "4806",
                gjelder = AktorDto("1321312312"),
                dokumenter = listOf(OpprettDokumentDto("Tittel på dokument", brevkode = "BI099")),
                tilknyttSaker = listOf(),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "Journalpost må knyttes til minst en sak"
    }

    @Test
    fun shouldFailWhenGjelderIsMissing() {
        val jp =
            OpprettJournalpostRequest(
                avsenderMottaker = AvsenderMottakerDto(ident = "123123213"),
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = "4806",
                dokumenter = listOf(OpprettDokumentDto("Tittel på dokument", brevkode = "BI099")),
                tilknyttSaker = listOf(),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "Gjelder ident kan ikke være tom"
    }

    @Test
    fun shouldFailWhenAvsenderMottakerIsMissingForUtgaaendeJournalpost() {
        val jp =
            OpprettJournalpostRequest(
                journalposttype = JournalpostType.UTGAAENDE,
                journalfoerendeEnhet = "4806",
                dokumenter = listOf(OpprettDokumentDto("Tittel på dokument", brevkode = "BI099")),
                tilknyttSaker = listOf("213213213"),
                gjelder = AktorDto("1321312312"),
                tittel = "Journalpost tittel",
            )
        val result = shouldThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }

        result.message shouldContain "Mottaker ident kan ikke være tom for utgående journalpost"
    }

    @Test
    fun shouldNotFailWhenAvsenderMottakerIsMissingForUtgaaendeNotat() {
        val jp =
            OpprettJournalpostRequest(
                journalposttype = JournalpostType.NOTAT,
                journalfoerendeEnhet = "4806",
                dokumenter = listOf(OpprettDokumentDto("Tittel på dokument", brevkode = "BI099")),
                tilknyttSaker = listOf("13123213"),
                gjelder = AktorDto("1321312312"),
                tittel = "Journalpost tittel",
            )
        shouldNotThrow<ViolationException> { OpprettUtgaaendeJournalpostIntern.sjekkKanOppretteJournalpost(jp) }
    }
}
