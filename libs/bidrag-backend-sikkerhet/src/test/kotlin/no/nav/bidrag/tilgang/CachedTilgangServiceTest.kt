package no.nav.bidrag.tilgang

import com.github.benmanes.caffeine.cache.Ticker
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.OpprinnelseTilgangsbeslutning
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponseDetaljer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertEquals

class CachedTilgangServiceTest {
    private val tilgangClient = mockk<TilgangClient> {
        every { ingenTilgangVedFeil(any(), any()) } returns TilgangClient.INGEN_TILGANG
    }
    private val tid = AtomicLong(0)
    private val ticker = Ticker { tid.get() }
    private var navIdent = "Z111111"
    private val service = CachedTilgangService(
        tilgangClient = tilgangClient,
        navIdentSupplier = { navIdent },
        ttl = Duration.ofMinutes(5),
        ticker = ticker,
    )

    private val saksnummer = Saksnummer("1234567")
    private val personident = Personident("12345678901")

    @Test
    fun `harTilgangSaksnummer bruker cache ved gjentatt kall`() {
        every { tilgangClient.hentTilgangSaksnummer(saksnummer) } returns TILGANG

        assertEquals(true, service.harTilgangSaksnummer(saksnummer))
        assertEquals(true, service.harTilgangSaksnummer(saksnummer))

        verify(exactly = 1) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `sjekkTilgangPerson returnerer hele svaret fra cache`() {
        every { tilgangClient.hentTilgangPerson(personident) } returns AVSLAG

        assertEquals(AVSLAG, service.sjekkTilgangPerson(personident))
        assertEquals(AVSLAG, service.sjekkTilgangPerson(personident))

        verify(exactly = 1) { tilgangClient.hentTilgangPerson(personident) }
    }

    @Test
    fun `harTilgang og sjekkTilgang deler cache`() {
        every { tilgangClient.hentTilgangSaksnummer(saksnummer) } returns TILGANG

        assertEquals(TILGANG, service.sjekkTilgangSaksnummer(saksnummer))
        assertEquals(true, service.harTilgangSaksnummer(saksnummer))

        verify(exactly = 1) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `avslag caches`() {
        every { tilgangClient.hentTilgangSaksnummer(saksnummer) } returns AVSLAG

        assertEquals(false, service.harTilgangSaksnummer(saksnummer))
        assertEquals(false, service.harTilgangSaksnummer(saksnummer))

        verify(exactly = 1) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `henter på nytt etter ttl`() {
        every { tilgangClient.hentTilgangSaksnummer(saksnummer) } returnsMany listOf(TILGANG, AVSLAG)

        assertEquals(true, service.harTilgangSaksnummer(saksnummer))
        tid.addAndGet(Duration.ofMinutes(4).toNanos())
        assertEquals(true, service.harTilgangSaksnummer(saksnummer))
        tid.addAndGet(Duration.ofMinutes(1).toNanos())
        assertEquals(false, service.harTilgangSaksnummer(saksnummer))

        verify(exactly = 2) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `cache deles ikke mellom saksbehandlere`() {
        every { tilgangClient.hentTilgangSaksnummer(saksnummer) } returnsMany listOf(TILGANG, AVSLAG)

        assertEquals(true, service.harTilgangSaksnummer(saksnummer))
        navIdent = "Z222222"
        assertEquals(false, service.harTilgangSaksnummer(saksnummer))

        verify(exactly = 2) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `sak og person med samme verdi caches hver for seg`() {
        val sakOgPersonVerdi = "12345678901"
        every { tilgangClient.hentTilgangSaksnummer(any()) } returns TILGANG
        every { tilgangClient.hentTilgangPerson(any()) } returns AVSLAG

        assertEquals(true, service.harTilgangSaksnummer(Saksnummer(sakOgPersonVerdi)))
        assertEquals(false, service.harTilgangPerson(Personident(sakOgPersonVerdi)))
    }

    @Test
    fun `feil gir ingen tilgang og caches ikke`() {
        every { tilgangClient.hentTilgangSaksnummer(saksnummer) } throws RuntimeException("nede") andThen TILGANG

        assertEquals(TilgangClient.INGEN_TILGANG, service.sjekkTilgangSaksnummer(saksnummer))
        assertEquals(true, service.harTilgangSaksnummer(saksnummer))

        verify(exactly = 2) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `kaster når NAV-ident mangler`() {
        navIdent = ""

        assertThrows<IllegalArgumentException> { service.harTilgangSaksnummer(saksnummer) }

        verify(exactly = 0) { tilgangClient.hentTilgangSaksnummer(any()) }
    }

    companion object {
        private val TILGANG = TilgangskontrollResponse(harTilgang = true)
        private val AVSLAG = TilgangskontrollResponse(
            harTilgang = false,
            detaljer = listOf(
                TilgangskontrollResponseDetaljer(false, "Skjermet", OpprinnelseTilgangsbeslutning.TILGANGSMASKIN),
            ),
        )
    }
}
