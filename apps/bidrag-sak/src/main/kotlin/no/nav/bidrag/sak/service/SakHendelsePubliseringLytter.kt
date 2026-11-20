package no.nav.bidrag.sak.service

import no.nav.bidrag.sak.integration.kafka.KafkaProducer
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

data class SakHendelsePubliseringEvent(
    val sakHendelse: SakHendelse,
)

@Component
class SakHendelsePubliseringLytter(
    private val kafkaProducer: KafkaProducer,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publiser(event: SakHendelsePubliseringEvent) {
        kafkaProducer.sendSakshendelse(event.sakHendelse)
    }
}
