package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

@Configuration
class KafkaTestConfig {
    @Bean
    fun kafka(): KafkaTemplate<String, String> {
        val kafkaTemplate: KafkaTemplate<String, String> = mockk(relaxed = true)
        val listenableFuture: CompletableFuture<SendResult<String, String>> = mockk(relaxed = true)

        every { kafkaTemplate.send(any(), any(), any()) } returns listenableFuture
        return kafkaTemplate
    }
}
