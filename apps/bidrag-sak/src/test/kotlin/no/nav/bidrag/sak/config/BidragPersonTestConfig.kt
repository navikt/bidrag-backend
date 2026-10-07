package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Configuration
class BidragPersonTestConfig {
    @Bean
    @Primary
    fun bidragPersonClientMock(): BidragPersonClient {
        val bidragPersonClient: BidragPersonClient = mockk()
        val slot = slot<List<Personident>>()
        every {
            bidragPersonClient.hentFødselsdatoer(capture(slot))
        } answers {
            val answer =
                slot.captured.associateWith {
                    LocalDate.parse(it.verdi.subSequence(0, 6), DateTimeFormatter.ofPattern("ddMMyy"))
                }
            slot.clear()
            answer
        }
        every { bidragPersonClient.hentPersonidenter(any()) } answers {
            listOf(PersonidentDto(firstArg(), historisk = false, gruppe = Identgruppe.FOLKEREGISTERIDENT))
        }
        return bidragPersonClient
    }
}
