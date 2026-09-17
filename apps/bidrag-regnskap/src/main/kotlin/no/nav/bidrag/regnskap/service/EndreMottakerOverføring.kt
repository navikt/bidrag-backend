package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val LOGGER = KotlinLogging.logger { }

data class EndreMottakerOpprettetEvent(val id: Long)

/**
 * Trigger overføring til ELIN først etter at persist-transaksjonen er committet, slik at det ikke-reverserbare
 * kallet aldri gjøres i en transaksjon som kan rulle tilbake. Feiler kallet, resendes raden skedulert.
 */
@Component
class EndreMottakerOverføringLytter(
    private val endreMottakerService: EndreMottakerService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun overførEtterCommit(event: EndreMottakerOpprettetEvent) {
        try {
            endreMottakerService.overførEndreMottaker(event.id)
        } catch (e: Exception) {
            LOGGER.error(e) { "Uventet feil ved overføring av endring av mottaker (id: ${event.id}) etter commit. Raden resendes ved neste skedulerte kjøring." }
        }
    }
}
