package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.generer.testdata.adresse.Adressetilknytning
import no.nav.bidrag.generer.testdata.adresse.TestAdresse
import no.nav.bidrag.generer.testdata.navn.TestNavn
import java.time.LocalDate

@Suppress("unused")
data class TestPerson(
    val personidenter: List<TestPersonIdent?>,
    val kjønn: Kjønn?,
    val fodselsdato: LocalDate,
    val dodDato: LocalDate?,
    val navnhistorikk: List<TestNavn?>,
    val mor: TestPerson?,
    val far: TestPerson?,
    val gradering: Gradering?,
    val språk: Språk?,
    val adressehistorikk: List<Adressetilknytning?>,
) {
    val barn: MutableList<TestPerson> = mutableListOf()

    init {
        mor?.barn?.add(this)
        far?.barn?.add(this)
    }

    val personIdent: String?
        get() = personidenter.filterNotNull().firstOrNull { it.aktiv && !it.aktoerId }?.ident

    val aktoerid: String?
        get() = personidenter.filterNotNull().firstOrNull { it.aktiv && it.aktoerId }?.ident

    val fornavn: String?
        get() = navn?.fornavn

    val etternavn: String?
        get() = navn?.etternavn

    val sammensattNavn: String
        get() = listOfNotNull(fornavn, etternavn).joinToString(" ")

    val navn: TestNavn?
        get() = navnhistorikk.filterNotNull().firstOrNull { it.overlapperMedIdag() }

    fun harBarn(): Boolean = barn.isNotEmpty()

    fun barn(barnNr: Int): TestPerson = barn[barnNr]

    fun denAndreForelderen(barnNr: Int): TestPerson? = denAndreForelderen(this, barn(barnNr))

    val postadresse: TestAdresse?
        get() {
            val today = LocalDate.now()
            return adressehistorikk
                .filterNotNull()
                .firstOrNull { it.type == Adressetype.KONTAKTADRESSE && it.overlapperMed(today) }
                ?.adresse
        }

    val boadresse: TestAdresse?
        get() {
            val today = LocalDate.now()
            return adressehistorikk
                .filterNotNull()
                .firstOrNull {
                    (it.type == Adressetype.BOSTEDSADRESSE || it.type == Adressetype.OPPHOLDSADRESSE) &&
                        it.overlapperMed(
                            today,
                        )
                }?.adresse
        }

    companion object {
        fun denAndreForelderen(
            forelder1: TestPerson,
            barn: TestPerson,
        ): TestPerson? =
            if (forelder1 == barn.far) {
                barn.mor
            } else {
                barn.far
            }
    }
}
