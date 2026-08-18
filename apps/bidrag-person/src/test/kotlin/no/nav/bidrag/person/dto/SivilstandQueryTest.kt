package no.nav.bidrag.person.dto

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.person.query.HentSivilstand
import no.nav.bidrag.person.query.Sivilstand
import no.nav.bidrag.person.query.SivilstandResponse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SivilstandQueryTest {
    @Test
    fun `skal mappe PDL-respons til SivilstandPdlDto`() {
        val sivilstandResponse = createSivilstandResponse()
        val (sivilstand) = sivilstandResponse.mapToSivilstandPdlHistorikkDto()

        sivilstand[0].type shouldBe SivilstandskodePDL.GIFT
        sivilstand[0].gyldigFom shouldBe LocalDate.of(2021, 1, 1)
        sivilstand[0].relatertVedSivilstand shouldBe "12345678901"
        sivilstand[0].bekreftelsesdato shouldBe LocalDate.of(2021, 1, 1)
    }

    private fun createSivilstandResponse(): SivilstandResponse {
        val sivilstand = Sivilstand(SivilstandskodePDL.GIFT, LocalDate.of(2021, 1, 1), "12345678901", LocalDate.of(2021, 1, 1), null)
        val hentSivilstand = HentSivilstand(listOf(sivilstand))
        return SivilstandResponse(hentSivilstand)
    }
}
