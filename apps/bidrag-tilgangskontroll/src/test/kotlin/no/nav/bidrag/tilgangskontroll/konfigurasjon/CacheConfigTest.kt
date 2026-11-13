package no.nav.bidrag.tilgangskontroll.konfigurasjon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.bidrag.commons.security.utils.TokenUtils
import org.junit.jupiter.api.Test

class CacheConfigTest {
    private val brukerIdentifikator = CacheConfig().cacheBrukerIdentifikator()

    @Test
    fun `skal bruke saksbehandlerident for saksbehandler`() = mockkStatic(TokenUtils::class) {
        every { TokenUtils.erApplikasjonsbruker() } returns false
        every { TokenUtils.hentSaksbehandlerIdent() } returns "Z999999"

        brukerIdentifikator.hentBrukerIdent() shouldBe "Z999999"
    }

    @Test
    fun `skal bruke felles systembruker for applikasjonskall`() = mockkStatic(TokenUtils::class) {
        every { TokenUtils.erApplikasjonsbruker() } returns true

        brukerIdentifikator.hentBrukerIdent() shouldBe CacheConfig.SYSTEMBRUKER_ID
    }

    @Test
    fun `skal feile når saksbehandlerident mangler`() = mockkStatic(TokenUtils::class) {
        every { TokenUtils.erApplikasjonsbruker() } returns false
        every { TokenUtils.hentSaksbehandlerIdent() } returns null

        shouldThrow<IllegalStateException> { brukerIdentifikator.hentBrukerIdent() }
    }
}
