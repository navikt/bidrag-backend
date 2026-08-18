package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.sak.integration.organisasjon.BidragOrganisasjonClient
import no.nav.bidrag.transport.organisasjon.EnhetDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class BidragOrganisasjonTestConfig {
    @Bean
    @Primary
    fun bidragOrganisasjonClientMock(): BidragOrganisasjonClient {
        val bidragOrganisasjonClient: BidragOrganisasjonClient = mockk()

        every {
            bidragOrganisasjonClient.hentEnhetForArbeidsfordelingGeografiskTilknytning(any())
        } returns EnhetDto(Enhetsnummer(EIERFOGD_UTLAND), "Utland")

        return bidragOrganisasjonClient
    }

    companion object {
        val EIERFOGD_UTLAND = "4865"
    }
}
