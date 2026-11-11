package no.nav.bidrag.person.hendelse.konfigurasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.kafka.KafkaRetryListener
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.kafka.KafkaException
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.ListenerUtils
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class KafkaOmstartFeilhåndterer : CommonContainerStoppingErrorHandler() {
    private val executor: Executor
    private val counter = AtomicInteger(0)
    private val lastError = AtomicLong(0)

    override fun handleRemaining(
        e: Exception,
        records: List<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        Thread.sleep(1000)

        if (records.isEmpty()) {
            LOGGER.warn(e) { "Feil ved konsumering av melding. Ingen records. ${consumer.subscription()}" }
            scheduleRestart(
                e,
                container,
                "Ukjent topic",
            )
        } else {
            records.first().run {
                kafkaRetryListener.logFailure(this, e)
                scheduleRestart(
                    e,
                    container,
                    this.topic(),
                )
            }
        }
    }

    private fun scheduleRestart(
        e: Exception,
        container: MessageListenerContainer,
        topic: String,
    ) {
        val now = System.currentTimeMillis()
        if (now - lastError.getAndSet(now) > COUNTER_RESET_TIME) { // Sjekker om perioden som det ventes er større enn counter_reset_time
            if (counter.get() > 0) {
                LOGGER.error {
                    "Feil ved prosessering av kafkamelding for $topic. Container har restartet ${counter.get()} ganger og " +
                        "man må se på hvorfor record ikke kan leses. " +
                        "Hvis denne meldingen gjentar seg hver ${Duration.ofMillis(MAX_RESTART_SLEEP)} så klarer ikke tjenesten å hente seg inn"
                }
            }
            counter.set(0)
        }
        val numErrors = counter.incrementAndGet()
        val stopTime = restartDelay(numErrors)
        executor.execute {
            try {
                Thread.sleep(stopTime)
                LOGGER.info { "Starter kafka container for $topic" }
                container.start()
            } catch (exception: Exception) {
                LOGGER.error(exception) { "Feil oppstod ved venting og oppstart av kafka container" }
            }
        }
        stopContainer(container) // i stedet for stopContainer i handleRemaining i parent som kaster error

        throw KafkaException(
            "Stopper kafka container ${counter.get()} for $topic i ${Duration.ofMillis(stopTime)} antall feil $numErrors",
            KafkaException.Level.WARN,
            e,
        )
    }

    /**
     * Stopper container. Hentet fra parent, men denne kaster WARN i stedet for ERROR for å begrense alarmer
     */
    private fun stopContainer(container: MessageListenerContainer) {
        this.executor.execute {
            container.stop {}
        }
        // isRunning is false before the container.stop() waits for listener thread
        try {
            ListenerUtils.stoppableSleep(container, 10000) // NOSONAR
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    internal fun restartDelay(numErrors: Int): Long = minOf(SHORT_SLEEP * numErrors, MAX_RESTART_SLEEP)

    companion object {
        private val LOGGER = KotlinLogging.logger {}

        private val MAX_RESTART_SLEEP = Duration.ofMinutes(1).toMillis()
        private val SHORT_SLEEP = Duration.ofSeconds(20).toMillis()
        private const val SLOW_ERROR_COUNT = 10
        private val COUNTER_RESET_TIME =
            SHORT_SLEEP * SLOW_ERROR_COUNT * 2 // 10 min
    }

    private val kafkaRetryListener = KafkaRetryListener()

    init {
        this.executor = SimpleAsyncTaskExecutor()
    }
}
