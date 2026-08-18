package no.nav.bidrag.person.config

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.consumer.SkjermingConsumer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SkjermingConfig {
    @Bean
    @Primary
    fun skjermingConsumer(): SkjermingConsumer {
        val slot = CapturingSlot<Set<Personident>>()
        val skjermingConsumer: SkjermingConsumer = mockk(relaxed = true)
        every { skjermingConsumer.erPersonerSkjermet(capture(slot)) } answers { slot.captured.associateWith { false } }
        return skjermingConsumer
    }
}
