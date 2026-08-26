package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.generer.testdata.RandomTestData
import no.nav.bidrag.generer.testdata.adresse.Adressetilknytning
import no.nav.bidrag.generer.testdata.adresse.AdressetilknytningBuilder
import no.nav.bidrag.generer.testdata.adresse.TestAdresse
import no.nav.bidrag.generer.testdata.adresse.TestAdresseBuilder
import no.nav.bidrag.generer.testdata.adresse.genererAdresse
import no.nav.bidrag.generer.testdata.navn.NavnBuilder
import no.nav.bidrag.generer.testdata.navn.TestNavn
import no.nav.bidrag.generer.testdata.tid.DateBuilder
import no.nav.bidrag.generer.testdata.tid.DateUtils
import java.time.LocalDate

@Suppress("unused")
class TestPersonBuilder {
    private var identtyper: MutableList<IdentType?> = mutableListOf(IdentTyper.FNR)
    private val antallAktoerIder = 1
    private var kjønn: Kjønn? = null
    private var alderMin = 25
    private var alderMax = 80
    private var fødtDato: LocalDate? = null
    private var dødDato: LocalDate? = null
    private val navnhistorikk: MutableList<NavnBuilder> = ArrayList()

    private var foreldreBuilder: ForeldreBuilder? = null
    private var mor: TestPerson? = null
    private var far: TestPerson? = null
    private val familier: MutableList<FamilieBuilder> = ArrayList()

    private var gradering: Gradering? = null
    private var språk: Språk? = null

    private val adresser: MutableList<AdressetilknytningBuilder> = ArrayList()

    fun identType(vararg identtyper: IdentType?): TestPersonBuilder {
        this.identtyper = mutableListOf(*identtyper)
        return this
    }

    fun kjønn(kjønn: Kjønn?): TestPersonBuilder {
        this.kjønn = kjønn
        return this
    }

    fun fødtDato(fødtDato: LocalDate?): TestPersonBuilder {
        this.fødtDato = fødtDato
        return this
    }

    fun født(dateBuilder: DateBuilder): TestPersonBuilder = fødtDato(dateBuilder.get())

    fun fødtDato(fødtDatoStr: String?): TestPersonBuilder {
        this.fødtDato = DateUtils.parseDate(fødtDatoStr)
        return this
    }

    fun alder(
        alderMin: Int,
        alderMax: Int,
    ): TestPersonBuilder {
        this.fødtDato = null
        this.alderMin = alderMin
        this.alderMax = alderMax
        return this
    }

    fun alder(alder: Int): TestPersonBuilder = alder(alder, alder)

    fun barnAlder(): TestPersonBuilder = alder(0, 18)

    fun voksenAlder(): TestPersonBuilder = alder(25, 40)

    fun besteforeldreAlder(): TestPersonBuilder = alder(45, 60)

    fun dødDato(dødDato: LocalDate?): TestPersonBuilder {
        this.dødDato = dødDato
        return this
    }

    fun erDød(): TestPersonBuilder = dødDato(LocalDate.now().minusDays(3))

    fun fornavn(fornavn: String?): TestPersonBuilder {
        sisteNavn().fornavn(fornavn)
        return this
    }

    fun etternavn(etternavn: String?): TestPersonBuilder {
        sisteNavn().etternavn(etternavn)
        return this
    }

    private fun sisteNavn(): NavnBuilder {
        if (navnhistorikk.isEmpty()) {
            navnhistorikk.add(NavnBuilder())
        }
        return navnhistorikk[navnhistorikk.size - 1]
    }

    fun med(navn: NavnBuilder): TestPersonBuilder {
        navnhistorikk.add(navn)
        return this
    }

    fun mor(
        mor: TestPerson?,
        vararg relasjoner: Relasjon,
    ): TestPersonBuilder {
        this.mor = mor
        return relasjonTil(mor, *relasjoner)
    }

    fun far(
        far: TestPerson?,
        vararg relasjoner: Relasjon,
    ): TestPersonBuilder {
        this.far = far
        return relasjonTil(far, *relasjoner)
    }

    fun med(foreldre: ForeldreBuilder?): TestPersonBuilder {
        this.foreldreBuilder = foreldre
        return this
    }

    fun med(familie: FamilieBuilder?): TestPersonBuilder {
        this.familier.add(familie!!)
        return this
    }

    fun relasjonTil(
        person: TestPerson?,
        vararg relasjoner: Relasjon,
    ): TestPersonBuilder {
        for (relasjon in relasjoner) {
            relasjon.relater(person, this)
        }
        return this
    }

    fun withGradering(gradering: Gradering?): TestPersonBuilder {
        this.gradering = gradering
        return this
    }

    fun språk(språk: Språk?): TestPersonBuilder {
        this.språk = språk
        return this
    }

    fun boadresse(boadresse: TestAdresse?): TestPersonBuilder = med(AdressetilknytningBuilder(boadresse, null, Adressetype.BOSTEDSADRESSE))

    fun boadresse(adresseBuilder: TestAdresseBuilder?): TestPersonBuilder =
        med(AdressetilknytningBuilder(null, adresseBuilder, Adressetype.BOSTEDSADRESSE))

    fun postadresse(boadresse: TestAdresse?): TestPersonBuilder =
        med(AdressetilknytningBuilder(boadresse, null, Adressetype.KONTAKTADRESSE))

    fun postadresse(adresseBuilder: TestAdresseBuilder?): TestPersonBuilder =
        med(AdressetilknytningBuilder(null, adresseBuilder, Adressetype.KONTAKTADRESSE))

    fun med(adressetilknytningBuilder: AdressetilknytningBuilder): TestPersonBuilder {
        this.adresser.add(adressetilknytningBuilder)
        return this
    }

    fun opprett(): TestPerson {
        val fødtDato = beregnFødtDato()
        val kjønn = this.kjønn ?: Kjønn.entries.random()
        val personidenter = opprettIdenter(kjønn, fødtDato)
        val navnhistorikk = opprettNavnhistorikk(kjønn, fødtDato)
        val adressehistorikk = opprettAdressehistorikk(fødtDato)

        val foreldre =
            finnForeldre(
                TestPerson(
                    personidenter,
                    kjønn,
                    fødtDato,
                    dødDato,
                    navnhistorikk,
                    mor,
                    far,
                    gradering,
                    språk,
                    adressehistorikk,
                ),
            )

        val person =
            TestPerson(
                personidenter,
                kjønn,
                fødtDato,
                dødDato,
                navnhistorikk,
                foreldre.mor,
                foreldre.far,
                gradering,
                språk,
                adressehistorikk,
            )

        for (familie in familier) {
            familie.get(person)
        }
        return person
    }

    private fun opprettIdenter(
        kjønn: Kjønn?,
        fodtDato: LocalDate,
    ): List<TestPersonIdent?> {
        val personIdenter =
            identtyper.mapIndexed { index, identType ->
                TestPersonIdent(identType!!.generer(fodtDato, kjønn), index == identtyper.size - 1, false)
            }

        val aktørIder =
            (0 until antallAktoerIder).map { i ->
                TestPersonIdent(
                    (100000000000L + RandomTestData.random().nextLong(200000000000L)).toString(),
                    i == 0,
                    true,
                )
            }

        return personIdenter + aktørIder
    }

    private fun opprettNavnhistorikk(
        kjønn: Kjønn?,
        fraDato: LocalDate?,
    ): MutableList<TestNavn?> {
        if (navnhistorikk.isEmpty()) {
            return NavnBuilder.lagNavnhistorikk(mutableListOf(NavnBuilder()), kjønn, fraDato)
        }
        return NavnBuilder.lagNavnhistorikk(navnhistorikk, kjønn, fraDato)
    }

    private fun opprettAdressehistorikk(fodtDato: LocalDate): MutableList<Adressetilknytning> {
        if (adresser.isEmpty()) {
            val adresser: MutableList<AdressetilknytningBuilder> = ArrayList()
            adresser.add(genererAdresse().tilknytning(Adressetype.BOSTEDSADRESSE))
            val utflyttingsdato = RandomTestData.random().dateBetween(fodtDato.plusYears(18), fodtDato.plusYears(23))
            if (!utflyttingsdato.isAfter(LocalDate.now())) {
                adresser.add(
                    genererAdresse()
                        .tilknytning(Adressetype.BOSTEDSADRESSE)
                        .fra(utflyttingsdato),
                )
            }
            return AdressetilknytningBuilder.lagAdressehistorikk(adresser, fodtDato)
        }
        return AdressetilknytningBuilder.lagAdressehistorikk(adresser, fodtDato)
    }

    fun opprett(antall: Int): MutableList<TestPerson?> {
        val personer = ArrayList<TestPerson?>(antall)
        for (i in 0..<antall) {
            personer.add(opprett())
        }
        return personer
    }

    private fun finnForeldre(tmpPerson: TestPerson): Foreldre =
        if (foreldreBuilder != null) {
            foreldreBuilder!!.get(tmpPerson)
        } else {
            Foreldre(null, null)
        }

    private fun beregnFødtDato(): LocalDate {
        if (fødtDato != null) {
            return fødtDato!!
        }
        val tomorrow = LocalDate.now().plusDays(1)
        return RandomTestData.random().dateBetween(
            tomorrow.minusYears((alderMax + 1).toLong()),
            tomorrow.minusYears(alderMin.toLong()),
        )
    }

    class RelasjonBuilder {
        private val personBuilder: TestPersonBuilder? = null
        private val relasjoner: Array<Relasjon?> = emptyArray()
    }
}

@Suppress("unused")
fun genererPerson(): TestPersonBuilder = TestPersonBuilder()
