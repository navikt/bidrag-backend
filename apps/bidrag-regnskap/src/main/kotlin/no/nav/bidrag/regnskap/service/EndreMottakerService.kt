package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.BidragReskontroConsumer
import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val LOGGER = KotlinLogging.logger { }

/**
 * Håndterer endring av regnskapsmottaker (RM) mot ELIN via bidrag-reskontro.
 *
 * Hvert `ENDRING_MOTTAKER`-vedtak lagres som en egen historikkrad og forsøkes overført umiddelbart.
 * ELIN lagrer kun gjeldende mottaker (ingen perioder), så ved resending overføres kun den nyeste
 * ikke-godkjente raden per (sak, barn) — se [PersistenceService.hentNyesteIkkeGodkjenteEndreMottakerPerSakOgBarn].
 */
@Service
class EndreMottakerService(
    private val persistenceService: PersistenceService,
    private val bidragReskontroConsumer: BidragReskontroConsumer,
    private val kravService: KravService,
) {

    companion object {
        private const val MAKS_LENGDE_FEILMELDING = 1000
    }

    @Transactional
    fun opprettOgOverførEndreMottaker(vedtakId: Int, sakId: String, barnIdent: String, nyMottakerIdent: String) {
        val endreMottaker = persistenceService.lagreEndreMottaker(
            EndreMottaker(
                vedtakId = vedtakId,
                saksnummer = sakId,
                barnIdent = barnIdent,
                nyMottakerIdent = nyMottakerIdent,
            ),
        )
        LOGGER.info { "Lagret endring av mottaker for vedtak: $vedtakId, sak: $sakId (id: ${endreMottaker.id})." }

        if (overføringErBlokkert()) {
            LOGGER.info { "Overføring av endring av mottaker er blokkert av driftsavvik/vedlikeholdsmodus. Rad ${endreMottaker.id} overføres ved neste skedulerte kjøring." }
            return
        }

        overførTilSkatt(endreMottaker)
    }

    @Transactional
    fun resendIkkeGodkjenteEndringer() {
        if (overføringErBlokkert()) {
            LOGGER.warn { "Overføring av endring av mottaker er blokkert av driftsavvik/vedlikeholdsmodus. Resender ikke." }
            return
        }

        val ikkeGodkjente = persistenceService.hentNyesteIkkeGodkjenteEndreMottakerPerSakOgBarn()
        if (ikkeGodkjente.isEmpty()) {
            LOGGER.info { "Det finnes ingen endringer av mottaker som ikke er godkjent av skatt." }
            return
        }

        LOGGER.info { "Forsøker å overføre ${ikkeGodkjente.size} endringer av mottaker på nytt." }
        ikkeGodkjente.forEach { overførTilSkatt(it) }
    }

    fun hentFeiledeOverføringer(): List<EndreMottaker> = persistenceService.hentNyesteIkkeGodkjenteEndreMottakerPerSakOgBarn()
        .filter { it.overførtTilSkattTidspunkt != null }

    private fun overførTilSkatt(endreMottaker: EndreMottaker) {
        endreMottaker.overførtTilSkattTidspunkt = LocalDateTime.now()
        try {
            bidragReskontroConsumer.endreRmForSak(
                saksnummer = Saksnummer(endreMottaker.saksnummer),
                barn = Personident(endreMottaker.barnIdent),
                nyMottaker = Personident(endreMottaker.nyMottakerIdent),
            )
            endreMottaker.godkjentAvSkattTidspunkt = LocalDateTime.now()
            endreMottaker.feilmeldingFraSkatt = null
            LOGGER.info { "Endring av mottaker (id: ${endreMottaker.id}) for sak ${endreMottaker.saksnummer} ble godkjent av skatt." }
        } catch (e: Exception) {
            endreMottaker.feilmeldingFraSkatt = e.message?.take(MAKS_LENGDE_FEILMELDING)
            LOGGER.error(e) { "Klarte ikke å overføre endring av mottaker (id: ${endreMottaker.id}) for sak ${endreMottaker.saksnummer} til skatt." }
            secureLogger.error(e) { "Klarte ikke å overføre endring av mottaker (id: ${endreMottaker.id}) for sak ${endreMottaker.saksnummer}, barn ${endreMottaker.barnIdent}, ny mottaker ${endreMottaker.nyMottakerIdent} til skatt." }
        }
        persistenceService.lagreEndreMottaker(endreMottaker)
    }

    private fun overføringErBlokkert(): Boolean = persistenceService.harAktivtDriftsavvik(erInnlesing = false) || kravService.erVedlikeholdsmodusPåslått()
}
