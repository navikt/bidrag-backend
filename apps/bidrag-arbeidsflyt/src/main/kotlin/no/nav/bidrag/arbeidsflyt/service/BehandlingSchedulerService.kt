package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.consumer.BidragBehandlingConsumer
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.transport.behandling.behandling.BehandlingDetaljerDtoV2
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseType
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.dokument.Sporingsdata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger {}

/**
 * Handles per-behandling processing during scheduled runs.
 * Each method uses REQUIRES_NEW so that a failure in one behandling
 * does not roll back statusSjekketTidspunkt updates for other behandlinger.
 */
@Service
class BehandlingSchedulerService(
    private val behandlingRepository: BehandlingRepository,
    private val behandleBehandlingHendelseService: BehandleBehandlingHendelseService,
    private val bidragBehandlingConsumer: BidragBehandlingConsumer,
) {
    /**
     * Processes a single behandling and unconditionally updates statusSjekketTidspunkt
     * in its own independent transaction so the timestamp is always committed,
     * regardless of whether [BehandleBehandlingHendelseService.behandleHendelse] succeeds or fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun behandleOgOppdaterStatusSjekket(behandlingId: Long) {
        val behandling = behandlingRepository.findById(behandlingId).orElse(null)
        if (behandling?.hendelse == null) {
            // Behandlingen eller lagret hendelse mangler. Prøver å gjenskape hendelsen fra bidrag-behandling
            val behandlingsid = behandling?.behandlingsid ?: behandlingId
            LOGGER.info { "Fant ikke lagret hendelse for behandling med id=$behandlingId. Gjenskaper hendelse fra bidrag-behandling med behandlingsid=$behandlingsid" }
            val hendelse =
                bidragBehandlingConsumer.hentBehandling(behandlingsid)?.tilBehandlingHendelse()
            if (hendelse == null) {
                LOGGER.info { "Fant ikke lagret hendelse for behandling med id=$behandlingId. Den er mest sannsynlig slettet. Forsøker å ferdigstille alle tilhørende oppgaver" }
                behandleBehandlingHendelseService.ferdigstillOppgaverSomErSlettet(behandlingsid)
                return
            }
            behandleBehandlingHendelseService.behandleHendelse(hendelse, true)
            behandlingRepository.finnForBehandlingId(behandlingsid)?.statusSjekketTidspunkt = LocalDateTime.now()
            return
        }

        try {
            LOGGER.info { "Sjekker og behandler behandling med id med id=$behandlingId" }
            behandleBehandlingHendelseService.behandleHendelse(behandling.hendelse!!, true)
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved behandling av hendelse for behandling med id=$behandlingId" }
        } finally {
            behandling.statusSjekketTidspunkt = LocalDateTime.now()
        }
    }

    /** Barn, status og søknadsdata fylles inn fra behandlingsdetaljene i [BehandleBehandlingHendelseService.behandleHendelse] */
    private fun BehandlingDetaljerDtoV2.tilBehandlingHendelse(): BehandlingHendelse {
        val nå = LocalDateTime.now()
        val vedtakstype = vedtakstype ?: throw IllegalStateException("Behandling med id=$id mangler vedtakstype")
        val behandlerenhet = behandlerenhet ?: throw IllegalStateException("Behandling med id=$id mangler behandlerenhet")
        return BehandlingHendelse(
            type = if (erVedtakFattet) BehandlingHendelseType.AVSLUTTET else BehandlingHendelseType.ENDRET,
            status = if (erVedtakFattet) BehandlingStatusType.VEDTAK_FATTET else BehandlingStatusType.UNDER_BEHANDLING,
            vedtakstype = vedtakstype,
            opprettetTidspunkt = nå,
            endretTidspunkt = nå,
            mottattDato = mottattdato ?: nå.toLocalDate(),
            sporingsdata = Sporingsdata(brukerident = opprettetAv.ident, saksbehandlersNavn = opprettetAv.navn, enhetsnummer = behandlerenhet),
            behandlingsid = id,
            behandlerEnhet = behandlerenhet,
            søknadsid = søknadsid,
            omgjørSøknadsid = søknadRefId,
        )
    }
}
