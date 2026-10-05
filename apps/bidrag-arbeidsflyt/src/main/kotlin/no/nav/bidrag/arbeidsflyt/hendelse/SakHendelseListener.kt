package no.nav.bidrag.arbeidsflyt.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.arbeidsflyt.PROFILE_KAFKA_TEST
import no.nav.bidrag.arbeidsflyt.PROFILE_NAIS
import no.nav.bidrag.arbeidsflyt.service.BehandleSakHendelseService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.sak.SakHendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
@Profile(value = [PROFILE_KAFKA_TEST, PROFILE_NAIS])
class SakHendelseListener(
    private val service: BehandleSakHendelseService,
) {
    @KafkaListener(
        groupId = $$"${NAIS_APP_NAME}",
        topics = [$$"${TOPIC_SAK_HENDELSE}"],
        properties = ["auto.offset.reset=latest"],
    )
    fun lesHendelse(consumerRecord: ConsumerRecord<String, String>) {
        val hendelse = lesHendelse(consumerRecord.value())
        secureLogger.info { "Behandler sakhendelse $hendelse" }
        service.behandleHendelse(hendelse)
    }

    fun lesHendelse(hendelse: String): SakHendelse = commonObjectmapper.readValue<SakHendelse>(hendelse)
}
