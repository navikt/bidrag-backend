package no.nav.bidrag.oppgave.service

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FellesKodeverkTema
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FinnOppgaverParams
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto
import no.nav.bidrag.oppgave.controller.BidragOppgaveDto
import no.nav.bidrag.oppgave.controller.FinnOppgaverRequest
import no.nav.bidrag.tilgang.TilgangskontrollService
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val tilgangService: TilgangskontrollService,
) {

    fun finnOppgaver(query: FinnOppgaverRequest): FinnOppgaverResultat {
        query.saksnummer?.let {
            tilgangService.sjekkTilgangSaksnummer(Saksnummer(it))
        }
        query.aktoerId?.let {
            tilgangService.sjekkTilgangPerson(Personident(it.verdi))
        }
        val offset = query.offset ?: FinnOppgaverRequest.STANDARD_OFFSET
        val limit = query.limit ?: FinnOppgaverRequest.STANDARD_LIMIT
        val respons = oppgaveClient.finnOppgaver(query.toOppgaveParams(offset, limit))
        return FinnOppgaverResultat(
            oppgaver = respons.oppgaver.orEmpty().map { it.tilBidragOppgave() },
            offset = offset,
            limit = limit,
            antallTreffTotalt = respons.antallTreffTotalt,
        )
    }

    private fun FinnOppgaverRequest.toOppgaveParams(offset: Int, limit: Int): FinnOppgaverParams = FinnOppgaverParams(
        saksreferanse = saksnummer?.let { listOf(it) },
        aktoerId = aktoerId?.let { listOf(it) },
        tema = listOf(FellesKodeverkTema.BID),
        statuskategori = "AAPEN",
        limit = limit,
        offset = offset,
    )
}

data class FinnOppgaverResultat(
    val oppgaver: List<BidragOppgaveDto>,
    val offset: Int,
    val limit: Int,
    val antallTreffTotalt: Long?,
)

private fun OppgaveDto.tilBidragOppgave(): BidragOppgaveDto = BidragOppgaveDto(
    id = id,
    tema = tema,
    oppgavetype = oppgavetype,
    brukerIdent = brukerIdent,
    saksreferanse = saksreferanse,
    prioritet = prioritet,
    journalpostId = journalpostId,
    tildeltEnhetsnr = tildeltEnhetsnr,
    tilordnetRessurs = tilordnetRessurs,
    beskrivelse = beskrivelse,
    beskrivelseListe = OppgaveBeskrivelseParser.parse(
        beskrivelse = beskrivelse,
        sistEndretTidspunkt = endretTidspunkt ?: opprettetTidspunkt,
        sistEndretAv = endretAv ?: opprettetAv,
        sistEndretEnhetsnr = endretAvEnhetsnr ?: opprettetAvEnhetsnr,
        oppgaveId = id.verdi,
    ),
    fristFerdigstillelse = fristFerdigstillelse,
    status = status,
    opprettetTidspunkt = opprettetTidspunkt,
)

/** Fnr, dnr og NPID har 11 sifre; aktør-ID har 13 og filtreres bort. */
private val OppgaveDto.brukerIdent: Personident?
    get() = bruker
        ?.takeIf { it.type == OppgaveDto.Bruker.BrukerType.PERSON && it.ident.length == PERSONIDENT_LENGDE }
        ?.let { Personident(it.ident) }

private const val PERSONIDENT_LENGDE = 11
