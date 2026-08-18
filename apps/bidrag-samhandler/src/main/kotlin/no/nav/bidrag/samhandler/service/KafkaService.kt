package no.nav.bidrag.samhandler.service

import no.nav.bidrag.commons.web.MdcConstants
import no.nav.bidrag.samhandler.kafka.SamhandlerProducer
import no.nav.bidrag.samhandler.persistence.entity.Samhandler
import no.nav.bidrag.transport.samhandler.SamhandlerKafkaHendelsestype
import no.nav.bidrag.transport.samhandler.Samhandlerhendelse
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Service
class KafkaService(
    private val samhandlerProducer: SamhandlerProducer,
) {
    fun sendSamhandlerMelding(
        samhandler: Samhandler,
        hendelsestype: SamhandlerKafkaHendelsestype,
    ) {
        val samhandlerhendelse =
            Samhandlerhendelse(
                samhandler.ident!!,
                hendelsestype,
                MDC.get(MdcConstants.MDC_CALL_ID),
            )
        samhandlerProducer.sendSamhandlerMelding(samhandlerhendelse)
    }
}
