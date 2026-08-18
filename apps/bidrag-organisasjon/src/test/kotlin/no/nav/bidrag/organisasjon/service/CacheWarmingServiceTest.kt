package no.nav.bidrag.organisasjon.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class CacheWarmingServiceTest {
    private val organisasjonServiceMock: OrganisasjonService = mockk(relaxed = true)

    @Test
    fun `skal ikke varme cache når enhet liste er tom`() {
        val cacheWarmingService = CacheWarmingService(organisasjonServiceMock, "")

        cacheWarmingService.warmupCacheOnStartup()

        verify(exactly = 0) { organisasjonServiceMock.hentPersonerEnhet(any()) }
    }

    @Test
    fun `skal varme cache for alle konfigurerte enheter`() {
        val enhetList = "4806,4833,4817"
        every { organisasjonServiceMock.hentPersonerEnhet(any()) } returns emptyList()
        val cacheWarmingService = CacheWarmingService(organisasjonServiceMock, enhetList)

        cacheWarmingService.warmupCacheOnStartup()

        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4806") }
        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4833") }
        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4817") }
    }

    @Test
    fun `skal håndtere feil ved varming av en enhet og fortsette med neste`() {
        val enhetList = "4806,4833,4817"
        every { organisasjonServiceMock.hentPersonerEnhet("4806") } returns emptyList()
        every { organisasjonServiceMock.hentPersonerEnhet("4833") } throws RuntimeException("Test error")
        every { organisasjonServiceMock.hentPersonerEnhet("4817") } returns emptyList()
        val cacheWarmingService = CacheWarmingService(organisasjonServiceMock, enhetList)

        cacheWarmingService.warmupCacheOnStartup()

        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4806") }
        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4833") }
        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4817") }
    }

    @Test
    fun `skal ignorere whitespace i enhet liste`() {
        val enhetList = " 4806 , 4833 , 4817 "
        every { organisasjonServiceMock.hentPersonerEnhet(any()) } returns emptyList()
        val cacheWarmingService = CacheWarmingService(organisasjonServiceMock, enhetList)

        cacheWarmingService.warmupCacheOnStartup()

        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4806") }
        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4833") }
        verify(exactly = 1) { organisasjonServiceMock.hentPersonerEnhet("4817") }
    }
}
