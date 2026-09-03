package no.nav.bidrag.behandling.async

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import no.nav.bidrag.behandling.async.dto.BehandlingOppdateringBestilling
import no.nav.bidrag.behandling.async.dto.GrunnlagInnhentingBestilling
import no.nav.bidrag.behandling.async.dto.OpprettForsendelseBestilling
import no.nav.bidrag.behandling.async.dto.OpprettNotatBestilling
import no.nav.bidrag.behandling.async.dto.SøknadSlettetBestilling
import no.nav.bidrag.behandling.service.BehandlingService
import no.nav.bidrag.behandling.service.GrunnlagService
import no.nav.bidrag.behandling.service.NotatOpplysningerService
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.transport.felles.tilJsonString
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = KotlinLogging.logger {}

@Component
class BestillAsyncJobListener(
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val notatOpplysningerService: NotatOpplysningerService,
) {
    @EventListener
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Async
    fun bestillInnhentingAvGrunnlag(bestilling: GrunnlagInnhentingBestilling) {
        if (bestilling.waitForCommit) return
        log.info { "Async: Henter grunnlag for behandling ${bestilling.behandlingId}" }
        grunnlagService.oppdatereGrunnlagForBehandling(bestilling.behandlingId)
    }

    @EventListener
    @Async
    fun behandleBestillingAvOppdateringAvRoller(bestilling: BehandlingOppdateringBestilling) {
        log.info { "Async: Oppdaterer roller for behandling ${bestilling.behandlingId} og request ${tilJsonString(bestilling.request)}" }
        behandlingService.oppdaterRoller(bestilling.behandlingId, bestilling.request)
    }

    @EventListener
    @Async
    fun behandleBestillingAvForsendelse(bestilling: OpprettForsendelseBestilling) {
        if (bestilling.waitForCommit) return
        log.info { "Async: Oppretter forsendelse for behandling ${bestilling.behandlingId}" }
        behandlingService.opprettForsendelseForBehandling(bestilling.behandlingId)
    }

    @EventListener
    @Async
    fun behandleBestillingEtterSøknadSlettet(bestilling: SøknadSlettetBestilling) {
        log.info { "Async: Behandler etter søknad slettet for søknadsid ${bestilling.søknadsid}" }
        behandlingService.behandleEtterSøknadSlettet(bestilling.søknadsid, bestilling.behandlingsid)
    }

    /**
     * Notatopprettelse er tung (PDF-produksjon + journalføring) og kjøres derfor i bakgrunnen etter at
     * vedtaket er fattet. Kjøres i applikasjonskontekst fordi saksbehandlerens token ikke propageres til
     * async-tråder.
     * Feiler jobben, plukkes behandlingen opp igjen av [no.nav.bidrag.behandling.scheduling.NotatFeilhåndteringScheduler].
     */
    @EventListener
    @Async
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true,
    )
    fun behandleBestillingAvNotat(bestilling: OpprettNotatBestilling) {
        log.info {
            "Async: Oppretter notat for behandling ${bestilling.behandlingId}" +
                bestilling.saksnummer?.let { " i sak $it" }.orEmpty()
        }
        try {
            SikkerhetsKontekst.medApplikasjonKontekst {
                notatOpplysningerService.opprettNotat(bestilling.behandlingId, saksnummer = bestilling.saksnummer)
            }
        } catch (e: Exception) {
            log.error(e) {
                "Det skjedde en feil ved opprettelse av notat for behandling ${bestilling.behandlingId}" +
                    bestilling.saksnummer?.let { " i sak $it" }.orEmpty()
            }
        }
    }
}
