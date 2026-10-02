package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.UnleashFeatures
import no.nav.bidrag.regnskap.consumer.BidragReskontroConsumer
import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

/**
 * Overføringen til ELIN skjer først etter at persist-transaksjonen er committet (via
 * [EndreMottakerOpprettetEvent]), slik at et ikke-reverserbart ELIN-kall aldri gjøres i en transaksjon som kan
 * rulle tilbake. Overføringer for samme sak og barn serialiseres gjennom en transaksjonslås i databasen.
 */
@Service
class EndreMottakerService(
    private val persistenceService: PersistenceService,
    private val bidragReskontroConsumer: BidragReskontroConsumer,
    private val kravService: KravService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    companion object {
        private const val MAKS_LENGDE_FEILMELDING = 1000
    }

    @Transactional
    fun opprettEndreMottaker(vedtakId: Int, sakId: String, barnIdent: String, nyMottakerIdent: String) {
        val endreMottaker = persistenceService.lagreEndreMottaker(
            EndreMottaker(
                vedtakId = vedtakId,
                saksnummer = sakId,
                barnIdent = barnIdent,
                nyMottakerIdent = nyMottakerIdent,
            ),
        )
        LOGGER.info { "Lagret endring av mottaker for vedtak: $vedtakId, sak: $sakId (id: ${endreMottaker.id})." }
        applicationEventPublisher.publishEvent(EndreMottakerOpprettetEvent(endreMottaker.id!!))
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW,
        noRollbackFor = [HttpClientErrorException::class, HttpServerErrorException::class, JwtTokenUnauthorizedException::class],
    )
    fun overførEndreMottaker(id: Long) {
        if (!UnleashFeatures.ENDRE_MOTTAKER.isEnabled) {
            LOGGER.info { "Endring av mottaker er deaktivert. Overfører ikke til skatt." }
            return
        }

        persistenceService.låsEndreMottakerForOverføring(id)
        val endreMottaker = persistenceService.hentEndreMottaker(id)
        if (endreMottaker == null) {
            LOGGER.error { "Fant ingen endring av mottaker med id: $id. Kan ikke overføre til skatt." }
            return
        }

        if (endreMottaker.godkjentAvSkattTidspunkt != null) {
            LOGGER.info { "Endring av mottaker (id: $id) er allerede godkjent av skatt. Overfører ikke på nytt." }
            return
        }

        if (persistenceService.finnesEldreIkkeGodkjentEndreMottaker(endreMottaker)) {
            LOGGER.info { "Endring av mottaker (id: $id) har eldre ikke-godkjente endringer på saken. Overfører ikke ennå." }
            return
        }

        if (overføringErBlokkert()) {
            LOGGER.info { "Overføring av endring av mottaker (id: $id) er blokkert av driftsavvik/vedlikeholdsmodus. Overføres ved neste skedulerte kjøring." }
            return
        }

        val nå = LocalDateTime.now()
        val oppdatert = try {
            bidragReskontroConsumer.endreRmForSak(
                saksnummer = Saksnummer(endreMottaker.saksnummer),
                barn = Personident(endreMottaker.barnIdent),
                nyMottaker = Personident(endreMottaker.nyMottakerIdent),
            )
            LOGGER.info { "Endring av mottaker (id: $id) for sak ${endreMottaker.saksnummer} ble godkjent av skatt." }

            endreMottaker.overførtTilSkattTidspunkt = nå
            endreMottaker.godkjentAvSkattTidspunkt = nå
            endreMottaker.feilmeldingFraSkatt = null
            endreMottaker
        } catch (e: Exception) {
            secureLogger.error(e) { "Klarte ikke å overføre endring av mottaker (id: $id) for sak ${endreMottaker.saksnummer}, barn ${endreMottaker.barnIdent}, ny mottaker ${endreMottaker.nyMottakerIdent} til skatt." }
            val feilmeldingFraSkatt = when (e) {
                is HttpStatusCodeException ->
                    "${e.statusCode.value()} ${e.statusText}".take(MAKS_LENGDE_FEILMELDING)

                is JwtTokenUnauthorizedException -> "Uautorisert kall til skatt"

                else -> "Uventet feil ved kall til skatt"
            }
            endreMottaker.overførtTilSkattTidspunkt = nå
            endreMottaker.godkjentAvSkattTidspunkt = null
            endreMottaker.feilmeldingFraSkatt = feilmeldingFraSkatt
            endreMottaker
        }
        persistenceService.lagreEndreMottaker(oppdatert)
    }

    fun hentIkkeGodkjenteEndringer(): List<EndreMottaker> = persistenceService.hentEldsteIkkeGodkjenteEndreMottakerPerSakOgBarn()

    fun hentFeiledeOverføringer(): List<EndreMottaker> = persistenceService.hentEldsteIkkeGodkjenteEndreMottakerPerSakOgBarn()
        .filter { it.overførtTilSkattTidspunkt != null }

    private fun overføringErBlokkert(): Boolean = persistenceService.harAktivtDriftsavvik(erInnlesing = false) || kravService.erVedlikeholdsmodusPåslått()
}
