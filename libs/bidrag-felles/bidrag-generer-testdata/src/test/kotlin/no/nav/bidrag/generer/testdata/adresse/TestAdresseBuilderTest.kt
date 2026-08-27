package no.nav.bidrag.generer.testdata.adresse

import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.domene.enums.diverse.LandkoderIso3
import no.nav.bidrag.generer.testdata.person.genererPerson
import org.junit.jupiter.api.Test

class TestAdresseBuilderTest {
    @Test
    fun skalByggeAdresseObjekt() {
        val adresse = genererAdresse().opprett()
        adresse shouldNotBe null
        genererPerson()
        println(adresse)

        val adresse2 = genererAdresse().bolignummer("5").land(LandkoderIso3.AFG).opprett()
        adresse2 shouldNotBe null
        println(adresse2)
    }

    @Test
    fun skalGenererEnkelAdresseString() {
        val adresse = genererEnkelAdresse()
        adresse shouldNotBe null
        println(adresse)
    }

    @Test
    fun skalGenerereAdresseMedPostnummerOgSted() {
        val adresse =
            genererAdresse()
                .postnummerOgSted("5000", "Bergen")
                .opprett()
        adresse shouldNotBe null
        println(adresse)
    }

    @Test
    fun skalGenerereEnkelAdresseMedPostnummerOgStedString() {
        val adresse = genererEnkelAdresseMedPoststed()
        adresse shouldNotBe null
        println(adresse)
    }
}
