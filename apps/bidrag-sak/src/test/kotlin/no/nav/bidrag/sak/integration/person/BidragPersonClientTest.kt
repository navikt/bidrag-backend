package no.nav.bidrag.sak.integration.person

import io.kotest.matchers.shouldBe
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.junit.jupiter.api.Test

class BidragPersonClientTest {
    @Test
    fun `velger gjeldende folkeregisterident foran NPID og historiske identer`() {
        val ident = "01019012345"
        val gjeldendeFnr = "02029012345"
        val gjeldendeNpid = "03039012345"
        val personidenter = listOf(
            PersonidentDto(ident, historisk = true, gruppe = Identgruppe.FOLKEREGISTERIDENT),
            PersonidentDto(gjeldendeNpid, historisk = false, gruppe = Identgruppe.NPID),
            PersonidentDto(gjeldendeFnr, historisk = false, gruppe = Identgruppe.FOLKEREGISTERIDENT),
        )

        personidenter.gjeldendeIdent() shouldBe gjeldendeFnr
    }

    @Test
    fun `gir ingen ident når oppslaget bare har historiske identer`() {
        listOf(PersonidentDto("01019012345", historisk = true, gruppe = Identgruppe.FOLKEREGISTERIDENT))
            .gjeldendeIdent() shouldBe null
    }
}
