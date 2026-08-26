package no.nav.bidrag.generer.testdata.person

import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.generer.testdata.person.FamilieBuilder.Companion.familie
import no.nav.bidrag.generer.testdata.person.ForeldreBuilder.Companion.foreldre
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TestPersonBuilderTest {
    @Test
    fun testPersonIdent() {
        val person = genererPerson().identType(IdentTyper.FNR).opprett()

        person shouldNotBe null
        person.personIdent shouldNotBe null
        person.personIdent?.length shouldBe 11
        println(person)
    }

    @Test
    fun testKjønn() {
        genererPerson().opprett().kjønn shouldBeOneOf listOf(Kjønn.KVINNE, Kjønn.MANN)
        genererPerson().kjønn(Kjønn.KVINNE).opprett().kjønn shouldBe Kjønn.KVINNE
        genererPerson().kjønn(Kjønn.MANN).opprett().kjønn shouldBe Kjønn.MANN
    }

    @Test
    fun testFødtDato() {
        val person = genererPerson().alder(10).opprett()
        person.fodselsdato shouldBeAfter LocalDate.now().minusYears(11)
        person.fodselsdato shouldBeLessThanOrEqualTo LocalDate.now().minusYears(10)
        println(person)
    }

    @Test
    fun testFornavn() {
        genererPerson().opprett().fornavn shouldNotBe null
        genererPerson().fornavn("Ola").opprett().fornavn shouldBe "Ola"
    }

    @Test
    fun testEtternavn() {
        genererPerson().opprett().etternavn shouldNotBe null
        genererPerson().etternavn("Nordmann").opprett().etternavn shouldBe "Nordmann"
    }

    @Test
    fun testForeldre() {
        val person =
            genererPerson()
                .fornavn("Ola")
                .etternavn("Nordmann")
                .med(
                    foreldre()
                        .mor(Relasjon.SAMME_ETTERNAVN) {
                            fornavn("Kari")
                        }.far(Relasjon.SAMME_ETTERNAVN) {
                            fornavn("Per")
                        },
                ).opprett()

        val mor: TestPerson? = person.mor
        mor shouldNotBe null
        mor?.fornavn shouldBe "Kari"
        mor?.etternavn shouldBe "Nordmann"
        mor?.barn(0) shouldBe person

        val far: TestPerson? = person.far
        far shouldNotBe null
        far?.fornavn shouldBe "Per"
        far?.etternavn shouldBe "Nordmann"
        far?.barn(0) shouldBe person
        println(person)
    }

    @Test
    fun testFamilieMedPartner() {
        val person =
            genererPerson()
                .kjønn(Kjønn.MANN)
                .fornavn("Ola")
                .etternavn("Nordmann")
                .med(
                    familie()
                        .partner(Relasjon.SAMME_ETTERNAVN) {
                            fornavn("Kari")
                        }.barn(Relasjon.SAMME_ETTERNAVN) {},
                ).opprett()

        val barn: TestPerson = person.barn(0)
        barn shouldNotBe null
        barn.etternavn shouldBe "Nordmann"
        barn.far shouldBe person
        barn.mor shouldNotBe null
        barn.mor?.fornavn shouldBe "Kari"
        barn.mor?.etternavn shouldBe "Nordmann"
        println(person)
    }

    @Test
    fun testAdresse() {
        val person =
            genererPerson()
                .alder(24)
                .opprett()

        val adressehistorikk = person.adressehistorikk
        adressehistorikk shouldNotBe null // Person skal ha adressehistorikk
        adressehistorikk shouldHaveSize 2 // Person mellom 18 og 23 kan ha flyttet ut.
        // Barn 24 og eldre har alltid flyttet ut og har derfor 2 innslag i adressehistorikken.
        adressehistorikk[1]?.adresse shouldBe person.boadresse // Aktiv bostedsadresse er siste innslag i historikken
        println(person)
    }

    @Test
    fun testAktørid() {
        val aktørid = genererPerson().opprett().aktoerid
        val aktørid2 = genererAktørid()

        aktørid shouldNotBe null
        aktørid2 shouldNotBe null

        println(aktørid)
        println(aktørid2)
    }
}
