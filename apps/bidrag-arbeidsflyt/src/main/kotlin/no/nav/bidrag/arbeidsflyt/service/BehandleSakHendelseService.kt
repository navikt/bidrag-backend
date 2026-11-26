package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.UnleashFeatures
import no.nav.bidrag.arbeidsflyt.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FlyttOppgaveVedEnhetsbytteRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.persistence.entity.Sak
import no.nav.bidrag.arbeidsflyt.persistence.repository.SakRepository
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleSakHendelseService(
    private val sakConsumer: BidragSakConsumer,
    private val oppgaveConsumer: OppgaveConsumer,
    private val organisasjonConsumer: BidragOrganisasjonConsumer,
    private val sakRepository: SakRepository,
) {
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun behandleHendelse(hendelse: SakHendelse) {
        val saksnummer = hendelse.saksnummer.verdi
        val eierfogd = sakConsumer.hentSakUtenCache(saksnummer).eierfogd.verdi
        if (UnleashFeatures.FLYTT_OPPGAVER_VED_ENHETSBYTTE.isEnabled) {
            flyttOppgaverTilEierfogd(saksnummer, eierfogd)
        } else {
            lagreEierfogdUtenFlytting(saksnummer, eierfogd)
        }
    }

    private fun flyttOppgaverTilEierfogd(saksnummer: String, eierfogd: String) {
        val lagretSak = sakRepository.findBySaksnummer(saksnummer)
            ?: sakRepository.save(Sak(saksnummer = saksnummer, eierfogd = forrigeEierfogdFraHistorikk(saksnummer) ?: eierfogd))
        val flytterFraEierfogder = lagretSak.flytterFraEierfogder + lagretSak.eierfogd - eierfogd
        if (flytterFraEierfogder.isEmpty()) return
        val sak = lagre(lagretSak.copy(eierfogd = eierfogd, flytterFraEierfogder = flytterFraEierfogder))
        oppgaveConsumer.hentÅpneOppgaverForSak(saksnummer)
            .filter { it.tildeltEnhetsnr in flytterFraEierfogder }
            .distinctBy { it.id }
            .forEach { flytt(saksnummer, it, eierfogd) }
        lagre(sak.copy(flytterFraEierfogder = emptySet()))
    }

    private fun forrigeEierfogdFraHistorikk(saksnummer: String): String? = sakConsumer.hentFogdhistorikk(saksnummer)
        .filter { it.type == Tilgangstype.EIER }
        .filter { it.tilgangTomDato?.isBefore(LocalDate.now().minusDays(1)) == false }
        .maxByOrNull { it.tilgangId }
        ?.enhetsnummer

    private fun lagreEierfogdUtenFlytting(saksnummer: String, eierfogd: String) {
        val lagretSak = sakRepository.findBySaksnummer(saksnummer)
        when {
            lagretSak == null -> sakRepository.save(Sak(saksnummer = saksnummer, eierfogd = eierfogd))

            lagretSak.eierfogd != eierfogd || lagretSak.flytterFraEierfogder.isNotEmpty() ->
                lagre(lagretSak.copy(eierfogd = eierfogd, flytterFraEierfogder = emptySet()))
        }
    }

    private fun lagre(sak: Sak): Sak = sakRepository.save(sak.copy(endretTidspunkt = LocalDateTime.now()))

    private fun flytt(saksnummer: String, oppgave: OppgaveData, nyEnhet: String) {
        val mappeNavn = oppgave.mappeId?.let { requireNotNull(oppgaveConsumer.hentMappe(it).navn) { "Mappen mangler navn" } }
        oppgaveConsumer.endreOppgave(
            FlyttOppgaveVedEnhetsbytteRequest(
                oppgave = oppgave,
                nyEnhet = nyEnhet,
                beholdSaksbehandler = saksbehandlerTilhørerEnhet(oppgave, nyEnhet),
                mappeNavn = mappeNavn,
                nyMappeId = mappeNavn?.let { tilsvarendeMappe(oppgave, nyEnhet, it) },
            ),
        )
        LOGGER.info { "Flyttet oppgave ${oppgave.id} i sak $saksnummer fra enhet ${oppgave.tildeltEnhetsnr} til $nyEnhet" }
    }

    private fun saksbehandlerTilhørerEnhet(oppgave: OppgaveData, enhet: String): Boolean = oppgave.tilordnetRessurs
        ?.takeIf(String::isNotBlank)
        ?.let { ident -> organisasjonConsumer.hentSaksbehandlerEnheter(ident).any { it.nummer.verdi == enhet } } == true

    private fun tilsvarendeMappe(oppgave: OppgaveData, enhet: String, mappeNavn: String): Long? {
        val tema = requireNotNull(oppgave.tema) { "Oppgaven mangler tema" }
        return oppgaveConsumer.hentMapper(enhet, tema).firstOrNull { it.navn == mappeNavn }?.id
    }
}
