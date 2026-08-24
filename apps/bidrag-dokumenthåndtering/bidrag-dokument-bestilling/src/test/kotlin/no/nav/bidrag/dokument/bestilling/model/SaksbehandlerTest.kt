package no.nav.bidrag.dokument.bestilling.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SaksbehandlerTest {
    @Test
    fun skalReturnereFornavnEtternavn() {
        val saksbehandler = Saksbehandler("Z123123", "Etternavn, Fornavn Mellomnavn")

        saksbehandler.fornavnEtternavn shouldBe "Fornavn Mellomnavn Etternavn"
        saksbehandler.navn shouldBe "Etternavn, Fornavn Mellomnavn"
        saksbehandler.ident shouldBe "Z123123"
    }

    @Test
    fun skalReturnereNavnHvisNavnIkkeErKommaSeparert() {
        val saksbehandler = Saksbehandler("Z123123", "Fornavn Mellomnavn Etternavn")

        saksbehandler.fornavnEtternavn shouldBe "Fornavn Mellomnavn Etternavn"
        saksbehandler.navn shouldBe "Fornavn Mellomnavn Etternavn"
        saksbehandler.ident shouldBe "Z123123"
    }

    @Test
    fun skalReturnereTomStringHvisNull() {
        val saksbehandler = Saksbehandler("Z123123", null)

        saksbehandler.fornavnEtternavn shouldBe ""
        saksbehandler.navn shouldBe null
        saksbehandler.ident shouldBe "Z123123"
    }
}
