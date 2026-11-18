package no.nav.bidrag.person.hendelse.integrasjon.bidrag.bisys

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.jms.Queue
import no.nav.bidrag.person.hendelse.exception.OverføringFeiletException
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.ProducerCallback
import org.springframework.stereotype.Component

@Component
class BisysMeldingsprodusjon(
    private val jmsTemplate: JmsTemplate,
) {
    fun sendeMeldinger(
        mottakerkoe: String,
        hendelser: List<String>,
    ): Int {
        var antallOverført = 0

        val producerCallback =
            ProducerCallback { session, producer ->

                val destination: Queue = session.createQueue(mottakerkoe)
                for (hendelse in hendelser) {
                    producer.send(destination, session.createTextMessage(hendelse))
                    antallOverført++
                }
            }

        try {
            jmsTemplate.execute(producerCallback)
        } catch (e: Exception) {
            log.error(e) { "Sending av melding til WMQ feilet med feilmelding '${e.message}'" }
            throw e.message?.let { OverføringFeiletException(it) }!!
        }

        return antallOverført
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
