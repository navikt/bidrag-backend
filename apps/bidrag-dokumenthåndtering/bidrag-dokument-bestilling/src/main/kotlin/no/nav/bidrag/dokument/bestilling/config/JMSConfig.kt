package no.nav.bidrag.dokument.bestilling.config

import com.ibm.mq.constants.CMQC
import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory
import com.ibm.msg.client.jakarta.jms.JmsConstants
import com.ibm.msg.client.jakarta.wmq.common.CommonConstants
import jakarta.jms.ConnectionFactory
import jakarta.jms.Destination
import jakarta.jms.JMSException
import jakarta.xml.bind.Marshaller
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.config.jms.LoggingMarshallingMessageConverter
import no.nav.bidrag.dokument.bestilling.config.jms.MQProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.connection.CachingConnectionFactory
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter
import org.springframework.jms.core.JmsTemplate
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import java.util.Locale

@Configuration
@EnableJms
@ConfigurationPropertiesScan
class JMSConfig(
    private val mqProperties: MQProperties,
) {
    fun createCachingConnectionFactory(mqQueueConnectionFactory: ConnectionFactory): CachingConnectionFactory {
        val cachingConnectionFactory = CachingConnectionFactory()
        cachingConnectionFactory.sessionCacheSize = 1
        cachingConnectionFactory.targetConnectionFactory = mqQueueConnectionFactory
        return cachingConnectionFactory
    }

    @Bean
    @Scope("prototype")
    fun baseJmsTemplate(mqQueueConnectionFactory: ConnectionFactory): JmsTemplate {
        val template = JmsTemplate()
        template.connectionFactory = createCachingConnectionFactory(mqQueueConnectionFactory)
        return template
    }

    @Bean
    @Profile("nais")
    fun replyDestinationQueue(
        @Value($$"${BREVSERVER_KVITTERING_QUEUE}") replyQueueName: String,
    ): Destination = MQQueue(replyQueueName)

    @Bean
    @Throws(JMSException::class)
    fun batchbrevTemplate(
        baseJmsTemplate: JmsTemplate,
        @Value($$"${BREVSERVER_BATCHBREV_QUEUE}") queueName: String,
        replyDestinationQueue: Destination,
    ): JmsTemplate {
        baseJmsTemplate.defaultDestinationName = queueName
        val jaxb2Marshaller = Jaxb2Marshaller()
        jaxb2Marshaller.setClassesToBeBound(BrevBestilling::class.java)
        jaxb2Marshaller.setMarshallerProperties(mapOf(Marshaller.JAXB_ENCODING to "ISO-8859-1"))
        baseJmsTemplate.messageConverter =
            LoggingMarshallingMessageConverter(jaxb2Marshaller, replyDestinationQueue)
        return baseJmsTemplate
    }

    @Bean
    @Throws(JMSException::class)
    fun onlinebrevTemplate(
        baseJmsTemplate: JmsTemplate,
        @Value($$"${BREVSERVER_ONLINEBREV_QUEUE}") queueName: String,
        replyDestinationQueue: Destination,
    ): JmsTemplate {
        baseJmsTemplate.defaultDestinationName = queueName
        val jaxb2Marshaller = Jaxb2Marshaller()
        jaxb2Marshaller.setClassesToBeBound(BrevBestilling::class.java)
        jaxb2Marshaller.setMarshallerProperties(mapOf(Marshaller.JAXB_ENCODING to "ISO-8859-1"))
        baseJmsTemplate.messageConverter =
            LoggingMarshallingMessageConverter(jaxb2Marshaller, replyDestinationQueue)
        return baseJmsTemplate
    }

    @Bean
    @Profile("nais")
    @Throws(JMSException::class)
    fun mqQueueConnectionFactory(
        @Value($$"${BREVSERVER_KVITTERING_QUEUE}") replyQueueName: String,
    ): ConnectionFactory {
        val connectionFactory = MQQueueConnectionFactory()
        connectionFactory.hostName = mqProperties.hostname
        connectionFactory.port = mqProperties.port
        connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
        connectionFactory.queueManager = mqProperties.name
        connectionFactory.channel = mqProperties.channel.uppercase(Locale.getDefault())
        connectionFactory.transportType = CommonConstants.WMQ_CM_CLIENT
        connectionFactory.setIntProperty(JmsConstants.JMS_IBM_ENCODING, CMQC.MQENC_S390)
        val credentialQueueConnectionFactory = UserCredentialsConnectionFactoryAdapter()
        credentialQueueConnectionFactory.setUsername(mqProperties.username)
        credentialQueueConnectionFactory.setPassword(mqProperties.password)
        credentialQueueConnectionFactory.setTargetConnectionFactory(connectionFactory)
        return credentialQueueConnectionFactory
    }
}
