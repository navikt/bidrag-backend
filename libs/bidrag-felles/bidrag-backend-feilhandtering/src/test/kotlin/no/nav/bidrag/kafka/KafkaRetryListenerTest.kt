package no.nav.bidrag.kafka

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class KafkaRetryListenerTest {
    @Test
    fun `logger feiltype og offset uten meldingsinnhold i ordinaer logg`() {
        val logger = LoggerFactory.getLogger(KafkaRetryListener::class.java) as Logger
        val secureLogger = LoggerFactory.getLogger("secureLogger") as Logger
        val regularAppender = ListAppender<ILoggingEvent>().apply { start() }
        val secureAppender = ListAppender<ILoggingEvent>().apply { start() }
        val originalLevel = logger.level
        val originalSecureLevel = secureLogger.level
        val originalAdditivity = secureLogger.isAdditive
        logger.level = Level.WARN
        secureLogger.level = Level.ERROR
        secureLogger.isAdditive = false
        logger.addAppender(regularAppender)
        secureLogger.addAppender(secureAppender)
        try {
            val listener = KafkaRetryListener()
            val record = ConsumerRecord("topic", 2, 7L, "hemmelig nokkel", "hemmelig melding")
            val error = IllegalStateException("hemmelig feilmelding", IllegalArgumentException("sensitive detaljer"))
            listener.failedDelivery(record, error, 1)

            val message = regularAppender.list.single().formattedMessage
            assertTrue(message.contains("topic=topic, partisjon=2, offset=7, forsøk=1, feiltype=IllegalArgumentException"))
            assertFalse(message.contains("hemmelig"))
            assertFalse(message.contains("sensitive detaljer"))
            assertTrue(secureAppender.list.single().throwableProxy.className.contains("IllegalStateException"))

            listener.recovered(record, error)
            val recoveryMessage = regularAppender.list.last().formattedMessage
            assertTrue(recoveryMessage.contains("topic=topic, partisjon=2, offset=7"))
            assertFalse(recoveryMessage.contains("hemmelig"))

            listener.logFailure(record, error)
            val restartMessage = regularAppender.list.last().formattedMessage
            assertTrue(restartMessage.contains("topic=topic, partisjon=2, offset=7"))
            assertFalse(restartMessage.contains("forsøk="))
            assertFalse(restartMessage.contains("hemmelig"))
        } finally {
            logger.detachAppender(regularAppender)
            secureLogger.detachAppender(secureAppender)
            logger.level = originalLevel
            secureLogger.level = originalSecureLevel
            secureLogger.isAdditive = originalAdditivity
            regularAppender.stop()
            secureAppender.stop()
        }
    }
}
