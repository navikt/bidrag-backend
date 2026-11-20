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
    fun behandleHendelse(hendelse: SakHendelse) {
        val saksnummer = hendelse.saksnummer.verdi
        val sak = sakConsumer.hentSakUtenCache(saksnummer)
        if (sak.begrensetTilgang) {
            LOGGER.warn { "Flytter ikke oppgaver fordi sakstilgangen er begrenset" }
            return
        }
        val eierfogd = sak.eierfogd.verdi
        val lagretSak = sakRepository.findBySaksnummer(saksnummer)
        if (UnleashFeatures.FLYTT_OPPGAVER_VED_ENHETSBYTTE.isEnabled) {
            val forrigeEierfogd = lagretSak?.eierfogd ?: forrigeEierfogdFraHistorikk(saksnummer)
            if (forrigeEierfogd != null && forrigeEierfogd != eierfogd) {
                flyttOppgaver(saksnummer, forrigeEierfogd, eierfogd)
            }
        }
        lagreEierfogd(lagretSak, saksnummer, eierfogd)
    }

    private fun forrigeEierfogdFraHistorikk(saksnummer: String): String? = sakConsumer.hentFogdhistorikk(saksnummer)
        .filter { it.type == Tilgangstype.EIER }
        .filter { it.tilgangTomDato?.isBefore(LocalDate.now().minusDays(1)) == false }
        .maxByOrNull { it.tilgangId }
        ?.enhetsnummer

    private fun flyttOppgaver(saksnummer: String, forrigeEierfogd: String, eierfogd: String) {
        oppgaveConsumer.hentÅpneOppgaverForSak(saksnummer)
            .filter { it.tildeltEnhetsnr == forrigeEierfogd }
            .distinctBy { it.id }
            .forEach { flytt(it, eierfogd) }
    }

    private fun lagreEierfogd(lagretSak: Sak?, saksnummer: String, eierfogd: String) {
        when {
            lagretSak == null -> sakRepository.save(Sak(saksnummer = saksnummer, eierfogd = eierfogd))
            lagretSak.eierfogd != eierfogd -> {
                lagretSak.eierfogd = eierfogd
                lagretSak.endretTidspunkt = LocalDateTime.now()
                sakRepository.save(lagretSak)
            }
        }
    }

    private fun flytt(oppgave: OppgaveData, nyEnhet: String) {
        oppgaveConsumer.endreOppgave(
            FlyttOppgaveVedEnhetsbytteRequest(oppgave, nyEnhet, saksbehandlerTilhørerEnhet(oppgave, nyEnhet), tilsvarendeMappe(oppgave, nyEnhet)),
        )
        LOGGER.info { "Flyttet oppgave ${oppgave.id} til enhet $nyEnhet" }
    }

    private fun saksbehandlerTilhørerEnhet(oppgave: OppgaveData, enhet: String): Boolean = oppgave.tilordnetRessurs
        ?.takeIf(String::isNotBlank)
        ?.let { ident -> organisasjonConsumer.hentSaksbehandlerEnheter(ident).any { it.nummer.verdi == enhet } } == true

    private fun tilsvarendeMappe(oppgave: OppgaveData, enhet: String): Long? = oppgave.mappeId?.let { mappeId ->
        val navn = requireNotNull(oppgaveConsumer.hentMappe(mappeId).navn) { "Mappen mangler navn" }
        val tema = requireNotNull(oppgave.tema) { "Oppgaven mangler tema" }
        oppgaveConsumer.hentMapper(enhet, tema).firstOrNull { it.navn == navn }?.id
    }
}
