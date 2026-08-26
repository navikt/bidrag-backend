package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.generer.testdata.RandomTestData

@Suppress("unused")
class ForeldreBuilder {
    private var morVisitor: (TestPersonBuilder.() -> Unit)? = null
    private var morRelasjoner: Array<out Relasjon> = emptyArray()
    private var farVisitor: (TestPersonBuilder.() -> Unit)? = null
    private var farRelasjoner: Array<out Relasjon> = emptyArray()
    private var relasjonerTilHverandre: Array<out Relasjon> = emptyArray()

    fun mor(
        vararg relasjoner: Relasjon,
        morVisitor: TestPersonBuilder.() -> Unit,
    ): ForeldreBuilder {
        this.morVisitor = morVisitor
        this.morRelasjoner = relasjoner
        return this
    }

    fun mor(morVisitor: TestPersonBuilder.() -> Unit) = mor(relasjoner = emptyArray(), morVisitor = morVisitor)

    fun far(
        vararg relasjoner: Relasjon,
        farVisitor: TestPersonBuilder.() -> Unit,
    ): ForeldreBuilder {
        this.farVisitor = farVisitor
        this.farRelasjoner = relasjoner
        return this
    }

    fun far(farVisitor: TestPersonBuilder.() -> Unit) = far(relasjoner = emptyArray(), farVisitor = farVisitor)

    fun relasjonTilHverandre(vararg relasjoner: Relasjon): ForeldreBuilder {
        this.relasjonerTilHverandre = relasjoner
        return this
    }

    fun get(tmpPerson: TestPerson): Foreldre {
        val mor = getForelder(Kjønn.KVINNE, morVisitor, tmpPerson, morRelasjoner, null, null)
        val far = getForelder(Kjønn.MANN, farVisitor, tmpPerson, farRelasjoner, mor, relasjonerTilHverandre)
        return Foreldre(mor, far)
    }

    private fun getForelder(
        kjønn: Kjønn,
        visitor: (TestPersonBuilder.() -> Unit)?,
        barn: TestPerson,
        relasjonerMedBarn: Array<out Relasjon>,
        annenForelder: TestPerson?,
        relasjonerMedAnnenForelder: Array<out Relasjon>?,
    ): TestPerson? =
        visitor?.let {
            val personBuilder =
                genererPerson()
                    .kjønn(kjønn)
                    .fødtDato(
                        RandomTestData
                            .random()
                            .dateBetween(barn.fodselsdato.minusYears(45), barn.fodselsdato.minusYears(18)),
                    )

            annenForelder?.let { Relasjon.relater(it, personBuilder, relasjonerMedAnnenForelder) }
            Relasjon.relater(barn, personBuilder, relasjonerMedBarn)

            personBuilder.apply(it).opprett()
        }

    companion object {
        @JvmStatic
        fun foreldre(): ForeldreBuilder = ForeldreBuilder()
    }
}
