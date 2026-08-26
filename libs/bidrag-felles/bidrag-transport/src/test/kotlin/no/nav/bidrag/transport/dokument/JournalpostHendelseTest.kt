package no.nav.bidrag.transport.dokument

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JournalpostHendelseTest {
    @Test
    fun `skal lage saksbehandlerInfo`() {
        val sporingsdata = Sporingsdata("correlationId", "Z99999", "Etternavn, Fornavn", "4888")
        sporingsdata.lagSaksbehandlerInfo() shouldBe "Etternavn, Fornavn (Z99999, 4888)"
    }

    @Test
    fun `skal lage saksbehandlerInfo uten ident`() {
        val sporingsdata = Sporingsdata("correlationId", null, "Etternavn, Fornavn", "4888")
        sporingsdata.lagSaksbehandlerInfo() shouldBe "Etternavn, Fornavn (4888)"
    }

    @Test
    fun `skal lage saksbehandlerInfo uten enhet og ident`() {
        val sporingsdata = Sporingsdata("correlationId", null, "Etternavn, Fornavn", null)
        sporingsdata.lagSaksbehandlerInfo() shouldBe "Etternavn, Fornavn (ukjent enhet)"
    }

    @Test
    fun `skal lage saksbehandlerInfo uten enhet`() {
        val sporingsdata = Sporingsdata("correlationId", "Z99999", "Etternavn, Fornavn", null)
        sporingsdata.lagSaksbehandlerInfo() shouldBe "Etternavn, Fornavn (Z99999, ukjent enhet)"
    }
}
