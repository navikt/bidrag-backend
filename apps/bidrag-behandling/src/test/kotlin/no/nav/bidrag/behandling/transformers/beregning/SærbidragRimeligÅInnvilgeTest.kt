package no.nav.bidrag.behandling.transformers.beregning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SærbidragRimeligÅInnvilgeTest {
    @Test
    fun `begrunnelse mangler når den er null`() {
        null.manglerSynligTekst() shouldBe true
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "   ",
            "<p><br/></p>",
            "<p><br></p><p><br/></p>",
            "<p>&nbsp;</p>",
            "<p> </p>",
            "<p> </p>",
        ],
    )
    fun `begrunnelse mangler når den ikke har synlig tekst`(begrunnelse: String) {
        begrunnelse.manglerSynligTekst() shouldBe true
    }

    @ParameterizedTest
    @ValueSource(strings = ["Særlige forhold", "<p>Særlige forhold</p>", "<p><strong>x</strong></p>", "<p>&nbsp;a</p>"])
    fun `begrunnelse finnes når den har synlig tekst`(begrunnelse: String) {
        begrunnelse.manglerSynligTekst() shouldBe false
    }
}
