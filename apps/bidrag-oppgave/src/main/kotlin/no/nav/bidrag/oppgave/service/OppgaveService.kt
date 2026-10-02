package no.nav.bidrag.oppgave.service

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FellesKodeverkTema
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FinnOppgaverParams
import no.nav.bidrag.oppgave.controller.FinnOppgaverRequest
import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import no.nav.bidrag.tilgang.TilgangskontrollService
import org.springframework.stereotype.Service
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto as OppgaveApiDto

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
        tildeltEnhetsnr = enhetsnummer,
        tilordnetRessurs = saksbehandler,
        tema = listOf(FellesKodeverkTema.BID),
        statuskategori = "AAPEN",
        limit = limit,
        offset = offset,
    )
}

data class FinnOppgaverResultat(
    val oppgaver: List<OppgaveDto>,
    val offset: Int,
    val limit: Int,
    val antallTreffTotalt: Long?,
)

private fun OppgaveApiDto.tilBidragOppgave(): OppgaveDto = OppgaveDto(
    id = id.verdi,
    tittel = "$tema - $oppgavetype",
    beskrivelse = beskrivelse,
    beskrivelseshistorikk = OppgaveBeskrivelseParser.parse(
        beskrivelse = beskrivelse,
        sistEndretTidspunkt = endretTidspunkt ?: opprettetTidspunkt,
        sistEndretAv = endretAv ?: opprettetAv,
        sistEndretEnhetsnr = endretAvEnhetsnr ?: opprettetAvEnhetsnr,
        oppgaveId = id.verdi,
    ),
    status = status.tilBidragStatus(),
    opprettet = opprettetTidspunkt,
)

private fun OppgaveApiDto.Status.tilBidragStatus(): OppgaveStatus = when (this) {
    OppgaveApiDto.Status.OPPRETTET, OppgaveApiDto.Status.AAPNET -> OppgaveStatus.OPPRETTET
    OppgaveApiDto.Status.UNDER_BEHANDLING -> OppgaveStatus.UNDER_BEHANDLING
    OppgaveApiDto.Status.FERDIGSTILT, OppgaveApiDto.Status.FEILREGISTRERT -> OppgaveStatus.FERDIG
}
