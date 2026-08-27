package no.nav.bidrag.domene.util

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import org.junit.jupiter.api.Test

class EnumutilsTest {
    @Test
    fun `skal hente enum-verdi basert på navn`() {
        val resultat = Transaksjonskode.entries.valueOfOrNull("A1")

        resultat.shouldNotBeNull()
        resultat shouldBe Transaksjonskode.A1
    }

    @Test
    fun `skal returnere null når enum-verdi ikke finnes`() {
        val resultat = Transaksjonskode.entries.valueOfOrNull("FINNES_IKKE")

        Transaksjonskode.valueOf("A1")
        resultat.shouldBeNull()
    }

    @Test
    fun `skal finne enum-verdi basert på parameterverdi`() {
        val resultat = Transaksjonskode.entries.finnMedParameterverdi("A3") { it.korreksjonskode }

        resultat.shouldNotBeNull()
        resultat shouldBe Transaksjonskode.A1
    }

    @Test
    fun `skal finne enum-verdi basert på beskrivelse`() {
        val resultat = Transaksjonskode.entries.finnMedParameterverdi("Bidragsforskudd") { it.beskrivelse }

        resultat.shouldNotBeNull()
        resultat shouldBe Transaksjonskode.A1
    }

    @Test
    fun `skal finne første enum-verdi basert på boolean parameterverdi`() {
        val resultat = Transaksjonskode.entries.finnMedParameterverdi(true) { it.negativtBeløp }

        resultat.shouldNotBeNull()
        resultat shouldBe Transaksjonskode.A3
    }

    @Test
    fun `skal returnere null når parameterverdi ikke matcher noen enum-verdi`() {
        val resultat = Transaksjonskode.entries.finnMedParameterverdi("FINNES_IKKE") { it.korreksjonskode }

        resultat.shouldBeNull()
    }

    @Test
    fun `skal finne alle enum-verdier med gitt parameterverdi`() {
        val resultat = Transaksjonskode.entries.finnAlleMedParameterverdi(true) { it.negativtBeløp }

        resultat.shouldNotBeNull()
        resultat shouldHaveSize 8
        resultat shouldContain Transaksjonskode.A3
        resultat shouldContain Transaksjonskode.B3
        resultat shouldContain Transaksjonskode.D3
    }

    @Test
    fun `skal finne alle enum-verdier med false negativtBeløp`() {
        val alleTransaksjonskoder = Transaksjonskode.entries.size
        val medNegativtBeløp = Transaksjonskode.entries.finnAlleMedParameterverdi(true) { it.negativtBeløp }.size
        val utenNegativtBeløp = Transaksjonskode.entries.finnAlleMedParameterverdi(false) { it.negativtBeløp }

        utenNegativtBeløp shouldHaveSize (alleTransaksjonskoder - medNegativtBeløp)
        utenNegativtBeløp shouldContain Transaksjonskode.A1
        utenNegativtBeløp shouldContain Transaksjonskode.B1
    }

    @Test
    fun `skal returnere tom liste når ingen enum-verdier matcher parameterverdi`() {
        val resultat = Transaksjonskode.entries.finnAlleMedParameterverdi("FINNES_IKKE") { it.beskrivelse }

        resultat.shouldNotBeNull()
        resultat.shouldBeEmpty()
    }

    @Test
    fun `skal håndtere enum med null-verdier i parameterverdi`() {
        val resultat = Transaksjonskode.entries.finnMedParameterverdi(null) { it.korreksjonskode }

        resultat.shouldNotBeNull()
        resultat shouldBe Transaksjonskode.A2
    }

    @Test
    fun `skal finne alle enum-verdier med null i parameterverdi`() {
        val resultat = Transaksjonskode.entries.finnAlleMedParameterverdi(null) { it.korreksjonskode }

        resultat.shouldNotBeNull()
        resultat.size shouldBe Transaksjonskode.entries.count { it.korreksjonskode == null }
        resultat shouldContain Transaksjonskode.A2
        resultat shouldContain Transaksjonskode.A4
    }

    @Test
    fun `skal returnere konsistente resultater ved flere kall`() {
        val resultat1 = Transaksjonskode.entries.finnMedParameterverdi("B3") { it.korreksjonskode }
        val resultat2 = Transaksjonskode.entries.finnMedParameterverdi("B3") { it.korreksjonskode }

        resultat1 shouldBe resultat2
        resultat1 shouldBe Transaksjonskode.B1
    }
}
