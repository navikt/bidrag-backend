package no.nav.bidrag.commons.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test

fun stubKodeverkProvider() {
    mockkObject(KodeverkProvider)
    mockkStatic(::finnVisningsnavnSkattegrunnlag)
    mockkStatic(::finnVisningsnavnLønnsbeskrivelse)
    mockkStatic(::finnVisningsnavnForKode)
    mockkStatic(::hentNavSkjemaKodeverk)
    every { finnVisningsnavnForKode(any(), any()) } returns "Visningsnavn"
    every { hentNavSkjemaKodeverk() } returns emptyMap()
    every {
        finnVisningsnavnLønnsbeskrivelse(any())
    } returns "Visningsnavn lønnsbeskrivelse"
    every { finnVisningsnavnSkattegrunnlag(any()) } returns "Visningsnavn skattegrunnlag"
}

class KodeverkProviderKtTest {
    @Test
    fun `skal hente visningsnavn`() {
        stubKodeverkProvider()
        finnVisningsnavn("overgangsstoenadTilEnsligForelder") shouldBe "Overgangsstønad"
        finnVisningsnavn("overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe31Mars2014EllerTidligere") shouldBe "Overgangsstønad"
        finnVisningsnavn("overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere") shouldBe "Overgangsstønad"
        finnVisningsnavn("overgangsstoenadTilGjenlevendeEktefelle") shouldBe "Overgangsstønad"
        finnVisningsnavn("annet") shouldBe "Visningsnavn"
    }
}
