package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.sak.integration.kodeverk.KodeverkClient
import no.nav.bidrag.sak.integration.kodeverk.dto.BeskrivelseDto
import no.nav.bidrag.sak.integration.kodeverk.dto.BetydningDto
import no.nav.bidrag.sak.integration.kodeverk.dto.KodeverkDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.LocalDate

@Configuration
class KodeverkTestConfig {
    @Bean
    @Primary
    fun kodeverkClientMock(): KodeverkClient {
        val kodeverkClient: KodeverkClient = mockk()
        every { kodeverkClient.hentLandkoder() } returns mockLandkoder()
        return kodeverkClient
    }

    private fun mockLandkoder(): KodeverkDto = KodeverkDto(
        mapOf(
            "SWE" to
                listOf(
                    BetydningDto(
                        LocalDate.MIN,
                        LocalDate.MAX,
                        mapOf("nb" to BeskrivelseDto("Sverige", "sve")),
                    ),
                ),
        ),
    )
}
