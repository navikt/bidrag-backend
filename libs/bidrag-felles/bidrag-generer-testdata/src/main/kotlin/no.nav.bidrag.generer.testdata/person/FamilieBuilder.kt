package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.generer.testdata.RandomTestData
import java.time.LocalDate

@Suppress("unused")
class FamilieBuilder {
    private var partnerVisitor: (TestPersonBuilder.() -> Unit)? = null
    private var partnerRelasjoner: Array<out Relasjon> = emptyArray()
    private val barnListe: MutableList<BarnBuilder> = ArrayList()

    fun partner(
        vararg relasjoner: Relasjon,
        partnerVisitor: TestPersonBuilder.() -> Unit,
    ): FamilieBuilder {
        this.partnerVisitor = partnerVisitor
        this.partnerRelasjoner = relasjoner
        return this
    }

    fun partner(partnerVisitor: TestPersonBuilder.() -> Unit) = partner(relasjoner = emptyArray(), partnerVisitor = partnerVisitor)

    fun barn(
        vararg relasjoner: Relasjon,
        barnVisitor: TestPersonBuilder.() -> Unit,
    ): FamilieBuilder {
        val relasjonArray =
            if (relasjoner.isEmpty()) arrayOf(Relasjon.SAMME_ETTERNAVN, Relasjon.SAMME_ADRESSE) else relasjoner
        this.barnListe.add(BarnBuilder(barnVisitor, relasjonArray, null))
        return this
    }

    fun barn(barnVisitor: TestPersonBuilder.() -> Unit): FamilieBuilder = barn(relasjoner = emptyArray(), barnVisitor = barnVisitor)

    fun barnMedRelasjonTilPartner(
        vararg relasjoner: Relasjon,
        barnVisitor: TestPersonBuilder.() -> Unit,
    ): FamilieBuilder {
        val relasjonArray =
            if (relasjoner.isEmpty()) arrayOf(Relasjon.SAMME_ETTERNAVN, Relasjon.SAMME_ADRESSE) else relasjoner
        this.barnListe.add(BarnBuilder(barnVisitor, null, relasjonArray))
        return this
    }

    fun barnMedRelasjonTilPartner(barnVisitor: TestPersonBuilder.() -> Unit): FamilieBuilder =
        barnMedRelasjonTilPartner(relasjoner = emptyArray(), barnVisitor = barnVisitor)

    fun get(person: TestPerson) {
        val partner = getPartner(person)
        barnListe.forEach { it.get(person, partner) }
    }

    private fun getPartner(person: TestPerson): TestPerson? =
        partnerVisitor?.let {
            val builder =
                genererPerson()
                    .fødtDato(
                        RandomTestData
                            .random()
                            .dateBetween(person.fodselsdato.minusYears(2), person.fodselsdato.plusYears(2)),
                    ).kjønn(if (Kjønn.MANN == person.kjønn) Kjønn.KVINNE else Kjønn.MANN)

            Relasjon.relater(person, builder, partnerRelasjoner)
            builder.apply(it).opprett()
        }

    private class BarnBuilder(
        private val visitor: TestPersonBuilder.() -> Unit,
        private val relasjoner: Array<out Relasjon>?,
        private val relasjonerTilPartner: Array<out Relasjon>?,
    ) {
        fun get(
            person: TestPerson,
            partner: TestPerson?,
        ) {
            val yngsteForeldreFodtDato =
                if (partner != null && partner.fodselsdato.isAfter(person.fodselsdato)) {
                    partner.fodselsdato
                } else {
                    person.fodselsdato
                }

            val mor = if (Kjønn.KVINNE == person.kjønn) person else partner
            val far = if (Kjønn.KVINNE == person.kjønn) partner else person

            val builder =
                genererPerson()
                    .fødtDato(
                        RandomTestData.random().dateBetween(yngsteForeldreFodtDato.plusYears(18), LocalDate.now()),
                    ).mor(mor)
                    .far(far)

            Relasjon.relater(person, builder, relasjoner)
            partner?.let { Relasjon.relater(it, builder, relasjonerTilPartner) }

            builder.apply(visitor).opprett()
        }
    }

    companion object {
        @JvmStatic
        fun familie(): FamilieBuilder = FamilieBuilder()
    }
}
