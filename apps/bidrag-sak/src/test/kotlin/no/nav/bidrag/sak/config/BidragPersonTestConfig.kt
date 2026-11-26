package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.transport.person.Graderingsinfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class BidragPersonTestConfig {
    @Bean
    @Primary
    fun bidragPersonClientMock(): BidragPersonClient {
        val bidragPersonClient: BidragPersonClient = mockk()
        every { bidragPersonClient.hentGraderingsinfo(any()) } answers { ingenSkjermet(firstArg()) }
        return bidragPersonClient
    }

    companion object {
        fun ingenSkjermet(identer: Set<Personident>) = Graderingsinfo(
            identerTilGradering = identer.associateWith { null },
            identerTilSkjerming = identer.associateWith { false },
        )
    }
}
