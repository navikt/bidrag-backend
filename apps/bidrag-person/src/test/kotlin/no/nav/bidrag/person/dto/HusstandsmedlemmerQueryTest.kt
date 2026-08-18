package no.nav.bidrag.person.dto

import io.kotest.matchers.shouldBe
import no.nav.bidrag.person.query.HusstandsmedlemmerResponse
import no.nav.bidrag.person.testdata.lagBostedsadresse
import no.nav.bidrag.person.testdata.lagHentHusstandsmedlemmerListe
import org.junit.jupiter.api.Test

class HusstandsmedlemmerQueryTest {
    @Test
    fun `skal mappe PDL-respons til HusstandsmedlemmerDto, sjekk på at kun adresse som matcher med hentet bostedsadresse brukes i respons`() {
        val husstandsmedlemmerResponse = createHusstandsmedlemmerResponse()
        val husstandsmedlemListe = husstandsmedlemmerResponse.mapToHusstandsmedlemmer(lagBostedsadresse())

        husstandsmedlemListe.size shouldBe 1
        husstandsmedlemListe[0].gyldigFraOgMed shouldBe null
        husstandsmedlemListe[0].gyldigTilOgMed shouldBe null
        husstandsmedlemListe[0].navn shouldBe "Fet Lettbrus"
    }

    private fun createHusstandsmedlemmerResponse(): HusstandsmedlemmerResponse {
        val husstandsmedlemListe = lagHentHusstandsmedlemmerListe()
        return HusstandsmedlemmerResponse(100L, husstandsmedlemListe)
    }
}
