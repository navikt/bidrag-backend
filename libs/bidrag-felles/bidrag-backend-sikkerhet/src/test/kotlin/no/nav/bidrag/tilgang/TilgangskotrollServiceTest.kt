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

class TilgangskotrollServiceTest {
    private val tilgangskontrollClient = mockk<TilgangskontrollClient>()
    private val tid = AtomicLong(0)
    private val ticker = Ticker { tid.get() }
    private var navIdent = "Z111111"
    private val service = TilgangskontrollService(
        tilgangskontrollClient = tilgangskontrollClient,
        navIdentSupplier = { navIdent },
        ttl = Duration.ofMinutes(5),
        ticker = ticker,
    )

    private val saksnummer = Saksnummer("1234567")
    private val personident = Personident("12345678901")

    @Test
    fun `hentTilgangSaksnummer bruker cache ved gjentatt kall`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returns TILGANG

        assertEquals(true, service.hentTilgangSaksnummer(saksnummer).harTilgang)
        assertEquals(true, service.hentTilgangSaksnummer(saksnummer).harTilgang)

        verify(exactly = 1) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `sjekkTilgangPerson returnerer hele svaret fra cache`() {
        every { tilgangskontrollClient.sjekkTilgangPerson(personident) } returns AVSLAG

        assertEquals(AVSLAG, service.hentTilgangPerson(personident))
        assertEquals(AVSLAG, service.hentTilgangPerson(personident))

        verify(exactly = 1) { tilgangskontrollClient.sjekkTilgangPerson(personident) }
    }

    @Test
    fun `hentTilgang og sjekkTilgang deler cache`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returns TILGANG

        assertEquals(TILGANG, service.hentTilgangSaksnummer(saksnummer))
        service.sjekkTilgangSaksnummer(saksnummer)

        verify(exactly = 1) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `avslag caches`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returns AVSLAG

        assertEquals(false, service.hentTilgangSaksnummer(saksnummer).harTilgang)
        assertEquals(false, service.hentTilgangSaksnummer(saksnummer).harTilgang)

        verify(exactly = 1) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `henter på nytt etter ttl`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returnsMany listOf(TILGANG, AVSLAG)

        assertEquals(true, service.hentTilgangSaksnummer(saksnummer).harTilgang)
        tid.addAndGet(Duration.ofMinutes(4).toNanos())
        assertEquals(true, service.hentTilgangSaksnummer(saksnummer).harTilgang)
        tid.addAndGet(Duration.ofMinutes(1).toNanos())
        assertEquals(false, service.hentTilgangSaksnummer(saksnummer).harTilgang)

        verify(exactly = 2) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `cache deles ikke mellom saksbehandlere`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returnsMany listOf(TILGANG, AVSLAG)

        assertEquals(true, service.hentTilgangSaksnummer(saksnummer).harTilgang)
        navIdent = "Z222222"
        assertEquals(false, service.hentTilgangSaksnummer(saksnummer).harTilgang)

        verify(exactly = 2) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `sak og person med samme verdi caches hver for seg`() {
        val sakOgPersonVerdi = "12345678901"
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(any()) } returns TILGANG
        every { tilgangskontrollClient.sjekkTilgangPerson(any()) } returns AVSLAG

        assertEquals(true, service.hentTilgangSaksnummer(Saksnummer(sakOgPersonVerdi)).harTilgang)
        assertEquals(false, service.hentTilgangPerson(Personident(sakOgPersonVerdi)).harTilgang)
    }

    @Test
    fun `feil gir ingen tilgang og caches ikke`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } throws RuntimeException("nede") andThen TILGANG

        assertEquals(false, service.hentTilgangSaksnummer(saksnummer).harTilgang)
        assertEquals(true, service.hentTilgangSaksnummer(saksnummer).harTilgang)

        verify(exactly = 2) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
    }

    @Test
    fun `sjekkTilgangSaksnummer kaster ikke ved tilgang`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returns TILGANG

        service.sjekkTilgangSaksnummer(saksnummer)
    }

    @Test
    fun `sjekkTilgangSaksnummer kaster TilgangskontrollException med detaljer ved avslag`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } returns AVSLAG

        val exception = assertThrows<TilgangskontrollException> { service.sjekkTilgangSaksnummer(saksnummer) }

        assertEquals(403, exception.statusCode.value())
        assertEquals(AVSLAG.detaljer, exception.body.properties?.get("detaljer"))
    }

    @Test
    fun `sjekkTilgangSaksnummer kaster TilgangskontrollException ved feil mot tilgangskontroll`() {
        every { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) } throws RuntimeException("nede")

        val exception = assertThrows<TilgangskontrollException> { service.sjekkTilgangSaksnummer(saksnummer) }

        assertEquals(OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL, exception.response.detaljer.single().opprinnelseTilgangsbeslutning)
    }

    @Test
    fun `sjekkTilgangPerson kaster ikke ved tilgang`() {
        every { tilgangskontrollClient.sjekkTilgangPerson(personident) } returns TILGANG

        service.sjekkTilgangPerson(personident)
    }

    @Test
    fun `sjekkTilgangPerson kaster TilgangskontrollException med detaljer ved avslag`() {
        every { tilgangskontrollClient.sjekkTilgangPerson(personident) } returns AVSLAG

        val exception = assertThrows<TilgangskontrollException> { service.sjekkTilgangPerson(personident) }

        assertEquals(403, exception.statusCode.value())
        assertEquals(AVSLAG.detaljer, exception.body.properties?.get("detaljer"))
    }

    @Test
    fun `sjekkTilgangPerson kaster TilgangskontrollException ved feil mot tilgangskontroll`() {
        every { tilgangskontrollClient.sjekkTilgangPerson(personident) } throws RuntimeException("nede")

        val exception = assertThrows<TilgangskontrollException> { service.sjekkTilgangPerson(personident) }

        assertEquals(OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL, exception.response.detaljer.single().opprinnelseTilgangsbeslutning)
    }

    @Test
    fun `kaster når NAV-ident mangler`() {
        navIdent = ""

        assertThrows<IllegalArgumentException> { service.hentTilgangSaksnummer(saksnummer).harTilgang }

        verify(exactly = 0) { tilgangskontrollClient.sjekkTilgangSaksnummer(any()) }
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
