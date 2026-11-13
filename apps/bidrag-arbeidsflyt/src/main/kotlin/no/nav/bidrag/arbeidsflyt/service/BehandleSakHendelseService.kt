package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.consumer.OppgaveConsumer
import no.nav.bidrag.arbeidsflyt.dto.FlyttOppgaveVedEnhetsbytteRequest
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleSakHendelseService(
    private val sakConsumer: BidragSakConsumer,
    private val oppgaveConsumer: OppgaveConsumer,
    private val organisasjonConsumer: BidragOrganisasjonConsumer,
    private val jdbcTemplate: JdbcTemplate,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun behandleHendelse(hendelse: SakHendelse) {
        val forrigeEierfogd = hendelse.forrigeEierfogd?.verdi ?: return
        val saksnummer = hendelse.saksnummer.verdi
        låsSak(saksnummer)
        val sak = sakConsumer.hentSakUtenCache(saksnummer)
        if (sak.begrensetTilgang) {
            LOGGER.warn { "Flytter ikke oppgaver fordi sakstilgangen er begrenset" }
            return
        }
        val eierfogd = sak.eierfogd.verdi
        if (forrigeEierfogd == eierfogd) return
        oppgaveConsumer.hentÅpneOppgaverForSak(saksnummer)
            .filter { it.tildeltEnhetsnr == forrigeEierfogd }
            .distinctBy { it.id }
            .forEach { flytt(it, eierfogd) }
    }

    private fun låsSak(saksnummer: String) {
        jdbcTemplate.queryForList(
            "SELECT set_config('lock_timeout', '10s', true), set_config('idle_in_transaction_session_timeout', '60s', true)",
        )
        jdbcTemplate.queryForList("SELECT pg_advisory_xact_lock(hashtext('bidrag-arbeidsflyt-sak'), ?)", saksnummer.toInt())
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
