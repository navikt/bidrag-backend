package no.nav.bidrag.dokument.bestilling.config.jms

import jakarta.jms.ExceptionListener
import jakarta.jms.JMSException
import org.slf4j.LoggerFactory

class JmsExceptionListener : ExceptionListener {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(JmsExceptionListener::class.java)
    }

    override fun onException(e: JMSException) {
        LOGGER.error("Det skjedde en feil ved parsing av JMS melding", e)
    }
}
