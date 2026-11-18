package no.nav.bidrag.arbeidsflyt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.consumer.BidragBehandlingConsumer
import no.nav.bidrag.arbeidsflyt.persistence.repository.BehandlingRepository
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

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
    fun behandleOgOppdaterStatusSjekket(behandlingEntityId: Long?, behandlingId: Long?) {
        val behandling = behandlingId?.let { behandlingRepository.finnForBehandlingId(it) } ?: behandlingEntityId?.let { behandlingRepository.findById(it).getOrNull() }
        val behandlingId = behandlingId ?: behandling?.behandlingsid ?: run {
            secureLogger.warn { "Fant ikke behandling med databaseId=$behandlingEntityId" }
            return
        }
        behandling?.statusSjekketTidspunkt = LocalDateTime.now()
        if (bidragBehandlingConsumer.erBehandlingSlettet(behandlingId) == true) {
            LOGGER.info { "Behandling med behandlingsid=$behandlingId er slettet. Setter status til avbrutt og ferdigstiller tilhørende søknadsoppgaver" }
            behandleBehandlingHendelseService.oppdaterBehandlingIDatabasen(behandling)
            behandleBehandlingHendelseService.ferdigstillSøknadsoppgaverForSøknadSomErSlettet(behandlingId)
            return
        }
        if (behandling?.hendelse == null) {
            // Behandlingen eller lagret hendelse mangler. Prøver å gjenskape hendelsen fra bidrag-behandling
            val behandlingsid = behandling?.behandlingsid ?: behandlingId
            LOGGER.info { "Fant ikke lagret hendelse for behandling med id=$behandlingId. Gjenskaper hendelse fra bidrag-behandling med behandlingsid=$behandlingsid" }
            val hendelse =
                bidragBehandlingConsumer.hentBehandling(behandlingsid, inkluderSlettet = true)?.tilBehandlingHendelse()
            if (hendelse == null) {
                LOGGER.info { "Fant ikke lagret hendelse for behandling med id=$behandlingId. Den er mest sannsynlig avsluttet. Forsøker å ferdigstille alle tilhørende oppgaver" }
                behandleBehandlingHendelseService.oppdaterBehandlingIDatabasen(behandling)
                behandleBehandlingHendelseService.ferdigstillSøknadsoppgaverForSøknadSomErSlettet(behandlingsid)
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
}
