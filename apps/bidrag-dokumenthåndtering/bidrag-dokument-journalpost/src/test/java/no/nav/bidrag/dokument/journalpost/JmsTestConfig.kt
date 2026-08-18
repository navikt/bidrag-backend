package no.nav.bidrag.dokument.journalpost

import jakarta.jms.ConnectionFactory
import jakarta.jms.Queue
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.apache.activemq.artemis.jms.client.ActiveMQQueue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@TestConfiguration
class JmsTestConfig {
    @Bean
    @Profile("!nais")
    fun mqQueueConnectionFactory(): ConnectionFactory = ActiveMQConnectionFactory("vm://localhost?broker.persistent=false")

    @Bean
    fun brevkvitterinQueue(
        @Value($$"${BREVSERVER_KVITTERING_QUEUE}") queuename: String,
    ): Queue = ActiveMQQueue(queuename)
}
