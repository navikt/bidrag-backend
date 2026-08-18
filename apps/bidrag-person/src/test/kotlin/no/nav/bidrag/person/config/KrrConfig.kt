package no.nav.bidrag.person.config

import io.mockk.mockk
import no.nav.bidrag.person.consumer.KrrConsumer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class KrrConfig {
    @Bean
    @Primary
    fun krrConsumer(): KrrConsumer = mockk(relaxed = true)
}
