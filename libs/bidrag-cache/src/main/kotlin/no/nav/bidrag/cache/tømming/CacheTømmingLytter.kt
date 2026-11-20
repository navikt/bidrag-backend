package no.nav.bidrag.cache.tømming

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.SmartLifecycle
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CacheTømmingLytter(
    connectionFactory: RedisConnectionFactory,
    cacheTømmer: CacheTømmer,
) : SmartLifecycle {
    private val container =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            addMessageListener(cacheTømmer, ChannelTopic(CacheTømmer.KANAL))
            afterPropertiesSet()
        }
    private val planlegger =
        Executors.newSingleThreadScheduledExecutor { Thread(it, "bidrag-cache-tømming").apply { isDaemon = true } }

    @Volatile
    private var kjører = false

    override fun start() {
        kjører = true
        planlegger.execute(::forsøkÅLytte)
    }

    override fun stop() {
        kjører = false
        planlegger.shutdownNow()
        container.destroy()
    }

    override fun isRunning(): Boolean = kjører

    private fun forsøkÅLytte() {
        if (!kjører) return
        try {
            container.start()
            LOGGER.info { "Lytter på beskjeder om tømming av cache" }
        } catch (e: RuntimeException) {
            container.stop()
            LOGGER.warn { "Klarte ikke lytte på beskjeder om tømming (${e.javaClass.simpleName}). Prøver igjen om $NYTT_FORSØK." }
            planlegger.schedule(::forsøkÅLytte, NYTT_FORSØK.toMillis(), TimeUnit.MILLISECONDS)
        }
    }

    private companion object {
        val NYTT_FORSØK: Duration = Duration.ofSeconds(30)
        val LOGGER = KotlinLogging.logger { }
    }
}
