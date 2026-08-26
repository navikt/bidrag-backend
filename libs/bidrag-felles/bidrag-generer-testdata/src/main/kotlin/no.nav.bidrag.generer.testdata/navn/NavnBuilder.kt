package no.nav.bidrag.generer.testdata.navn

import no.nav.bidrag.generer.testdata.person.Kjønn
import no.nav.bidrag.generer.testdata.tid.DateUtils
import java.time.LocalDate
import java.util.stream.Collectors

@Suppress("unused")
class NavnBuilder(
    private var fornavn: String? = null,
    private var etternavn: String? = null,
    private var periodeFra: LocalDate? = null,
    private var periodeTil: LocalDate? = null,
    private var tilUbestemt: Boolean = false,
) {
    constructor(base: NavnBuilder) : this(
        base.fornavn,
        base.etternavn,
        base.periodeFra,
        base.periodeTil,
        base.tilUbestemt,
    )

    fun fornavn(fornavn: String?): NavnBuilder {
        this.fornavn = fornavn
        return this
    }

    fun etternavn(etternavn: String?): NavnBuilder {
        this.etternavn = etternavn
        return this
    }

    fun fra(periodeFra: LocalDate?): NavnBuilder {
        this.periodeFra = periodeFra
        return this
    }

    fun fra(periodeFra: String?): NavnBuilder = fra(DateUtils.parseDate(periodeFra))

    fun til(periodeTil: LocalDate?): NavnBuilder {
        this.periodeTil = periodeTil
        this.tilUbestemt = false
        return this
    }

    fun til(periodeTil: String?): NavnBuilder = til(DateUtils.parseDate(periodeTil))

    fun tilUbestemt(): NavnBuilder {
        this.periodeTil = null
        this.tilUbestemt = true
        return this
    }

    fun opprett(
        kjønn: Kjønn?,
        fraDato: LocalDate?,
    ): TestNavn {
        val fornavn =
            if (this.fornavn != null) {
                this.fornavn
            } else {
                genererFornavn(kjønn)
            }

        val etternavn =
            if (this.etternavn != null) {
                this.etternavn
            } else {
                genererEtternavn()
            }

        return TestNavn(fornavn, etternavn, periodeFra, periodeTil)
    }

    companion object {
        fun navn(): NavnBuilder = NavnBuilder()

        fun lagNavnhistorikk(
            navnBuilderListe: MutableCollection<NavnBuilder>,
            kjønn: Kjønn?,
            fraDato: LocalDate?,
        ): MutableList<TestNavn?> =
            periodiser(navnBuilderListe, fraDato)
                .stream()
                .map { b: NavnBuilder? -> b!!.opprett(kjønn, fraDato) }
                .collect(Collectors.toList())

        private fun periodiser(
            navnBuilderListe: MutableCollection<NavnBuilder>,
            fraDato: LocalDate?,
        ): MutableList<NavnBuilder?> {
            val periodisert = ArrayList<NavnBuilder?>()
            var forrigeBuilder: NavnBuilder? = null
            for (baseNavnBuilder in navnBuilderListe) {
                val builder = NavnBuilder(baseNavnBuilder)

                if ((forrigeBuilder == null) && (builder.periodeFra == null)) {
                    builder.periodeFra = fraDato
                }
                requireNotNull(builder.periodeFra) { "Periode fra må være satt for adressetilknytning (unntatt første)" }
                if (builder.periodeFra!!.isBefore(fraDato)) {
                    builder.periodeFra = fraDato
                }
                if (forrigeBuilder != null && !forrigeBuilder.tilUbestemt && forrigeBuilder.periodeTil == null) {
                    forrigeBuilder.periodeTil = builder.periodeFra
                }
                periodisert.add(builder)
                forrigeBuilder = builder
            }
            return periodisert
        }
    }
}
