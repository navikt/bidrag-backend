package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.LeggTilKommentarPaaOppgave
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.OppgaveEnhet
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveFagpostRequest
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import java.util.function.Consumer

class OppgaveService(
    private val personConsumers: ResourceByDiscriminator<PersonConsumer>,
    private val oppgaveConsumers: ResourceByDiscriminator<OppgaveConsumer>,
    private val saksbehandlerInfoManager: SaksbehandlerInfoManager,
) {

    fun leggTilKommentarPaaJournalforingsoppgave(journalpost: Journalpost, saksbehandlerMedEnhet: SaksbehandlerMedEnhet, kommentar: String) {
        val oppgaver = finnJournalforingOppgaverForJournalpost(journalpost.hentJournalpostIdLong())
        oppgaver.filter { it.tildeltEnhetsnr != OppgaveEnhet.FAGPOST }.forEach(
            Consumer { oppgave: OppgaveData ->
                oppgaveConsumers.get(Discriminator.SERVICE_USER)
                    .patchOppgave(
                        LeggTilKommentarPaaOppgave(
                            oppgave,
                            saksbehandlerMedEnhet.enhetsnummer,
                            saksbehandlerMedEnhet.hentSaksbehandlerInfo(),
                            kommentar,
                        ),
                    )
            },
        )
    }

    fun opprettOppgaveTilFagpost(opprettOppgaveFagpostRequest: OpprettOppgaveFagpostRequest) {
        if (opprettOppgaveFagpostRequest.hasGjelderId()) {
            val aktorId = hentAktorId(opprettOppgaveFagpostRequest.hentGjelderId())
            opprettOppgaveFagpostRequest.aktoerId = aktorId
        }
        opprettOppgave(opprettOppgaveFagpostRequest)
    }

    fun ferdigstillVurderDokumentOppgaver(journalpostId: Long, enhetsnr: String) {
        val oppgaver = finnVurderDokumentOppgaverForJournalpost(journalpostId)
        oppgaver.forEach(Consumer { oppgave: OppgaveData -> ferdigstillOppgave(oppgave, enhetsnr) })
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, enhetsnr: String) {
        oppgaveConsumers.get(Discriminator.SERVICE_USER)
            .patchOppgave(FerdigstillOppgaveRequest(oppgaveData, enhetsnr))
    }

    private fun opprettOppgave(request: OpprettOppgaveRequest) {
        oppgaveConsumers.get(Discriminator.REGULAR_USER).opprett(request)
    }

    private fun hentAktorId(gjelder: String?): String? {
        if (gjelder == null) return null
        return personConsumers.get(Discriminator.SERVICE_USER).hentPerson(gjelder)
            .orElseGet {
                PersonDto(
                    ident = Personident(gjelder),
                    aktørId = gjelder,
                )
            }.aktørId
    }

    private fun finnVurderDokumentOppgaverForJournalpost(journalpostId: Long): List<OppgaveData> {
        val parametre = OppgaveSokParametre()
            .leggTilFagomrade("BID")
            .leggTilJournalpostId(journalpostId)
            .brukVurderDokumentSomOppgaveType()
        return oppgaveConsumers.get(Discriminator.SERVICE_USER).finnOppgaver(parametre)?.oppgaver
            ?: emptyList()
    }

    private fun finnJournalforingOppgaverForJournalpost(journalpostId: Long?): List<OppgaveData> {
        val parametre = OppgaveSokParametre()
            .leggTilFagomrade("BID")
            .leggTilJournalpostId(journalpostId!!)
            .brukJournalforingSomOppgaveType()
        return oppgaveConsumers.get(Discriminator.SERVICE_USER).finnOppgaver(parametre)?.oppgaver
            ?: emptyList()
    }
}
