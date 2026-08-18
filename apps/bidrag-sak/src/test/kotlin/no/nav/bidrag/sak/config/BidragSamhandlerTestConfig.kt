package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.sak.integration.samhandler.BidragSamhandlerClient
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class BidragSamhandlerTestConfig {
    @Bean
    @Primary
    fun bidragSamhandlerClientMock(): BidragSamhandlerClient {
        val bidragPersonClient: BidragSamhandlerClient = mockk()
        val slot = slot<SamhandlerId>()
        every {
            bidragPersonClient.hentSamhandler(capture(slot))
        } answers {
            val answer =
                SamhandlerDto(offentligId = "", offentligIdType = OffentligIdType.DNR, samhandlerId = slot.captured, navn = "samhandler")
            slot.clear()
            answer
        }
        return bidragPersonClient
    }
}
