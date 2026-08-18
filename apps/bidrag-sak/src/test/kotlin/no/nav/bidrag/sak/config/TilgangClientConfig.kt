package no.nav.bidrag.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.transport.tilgang.Sporingsdata
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class TilgangClientConfig {
    @Bean
    @Primary
    fun tilgangClientMock(): TilgangClient {
        val tilgangClient: TilgangClient = mockk(relaxed = true)
        every { tilgangClient.harTilgangSaksnummer(any()) } returns true
        every { tilgangClient.harTilgangPerson(any()) } returns true
        every { tilgangClient.hentSporingsdataSak(any()) } returns Sporingsdata("", true)
        every { tilgangClient.hentSporingsdataPerson(any()) } returns Sporingsdata("", true)
        return tilgangClient
    }
}
