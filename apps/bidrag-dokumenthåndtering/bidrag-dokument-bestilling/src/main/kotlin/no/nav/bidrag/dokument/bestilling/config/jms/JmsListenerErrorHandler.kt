package no.nav.bidrag.dokument.bestilling.config.jms

import org.slf4j.LoggerFactory
import org.springframework.util.ErrorHandler

class JmsListenerErrorHandler : ErrorHandler {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(JmsListenerErrorHandler::class.java)
    }

    override fun handleError(t: Throwable) {
        LOGGER.error("Det skjedde en feil ved behandling av JMS melding", t)
    }
}
