package no.nav.bidrag.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaConfig(
    @param:Value($$"${TOPIC_SAK}") val topicSak: String,
)
