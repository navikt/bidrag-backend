package no.nav.bidrag.dokument.bestilling

import jakarta.jms.ConnectionFactory
import jakarta.jms.Destination
import jakarta.jms.Queue
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.apache.activemq.artemis.jms.client.ActiveMQDestination
import org.apache.activemq.artemis.jms.client.ActiveMQQueue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@TestConfiguration
class JmsTestConfig {
    init {
        val config = ConfigurationImpl()
        config.isPersistenceEnabled = false
        config.isSecurityEnabled = false
        config.addAcceptorConfiguration("in-vm", "vm://0")
        val server = EmbeddedActiveMQ()
        server.setConfiguration(config)
        server.start()
    }

    @Bean
    @Profile("!nais")
    fun jmsConnectionFactory(): ConnectionFactory = ActiveMQConnectionFactory("vm://0")

    @Bean
    @Profile("!nais")
    fun replyDestinationQueue(
        @Value($$"${BREVSERVER_KVITTERING_QUEUE}") replyQueueName: String,
    ): Destination = ActiveMQDestination.createDestination(replyQueueName, ActiveMQDestination.TYPE.QUEUE)

    @Bean
    fun onlineBrevQueue(
        @Value($$"${BREVSERVER_ONLINEBREV_QUEUE}") queuename: String,
    ): Queue = ActiveMQQueue(queuename)

    @Bean
    fun batchBrevQueue(
        @Value($$"${BREVSERVER_BATCHBREV_QUEUE}") queuename: String,
    ): Queue = ActiveMQQueue(queuename)

    @Bean
    @Profile("test")
    fun cacheManager(): CacheManager = NoOpCacheManager()
}
