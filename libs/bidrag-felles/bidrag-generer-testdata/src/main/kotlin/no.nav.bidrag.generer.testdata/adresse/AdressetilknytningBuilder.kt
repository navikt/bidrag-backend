package no.nav.bidrag.generer.testdata.adresse

import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.generer.testdata.tid.DateUtils
import java.time.LocalDate

@Suppress("unused")
class AdressetilknytningBuilder(
    private val adresse: TestAdresse? = null,
    private val adresseBuilder: TestAdresseBuilder? = null,
    private val adressetype: Adressetype? = null,
    private var periodeFra: LocalDate? = null,
    private var periodeTil: LocalDate? = null,
    private var tilUbestemt: Boolean = false,
) {
    constructor(base: AdressetilknytningBuilder) : this(
        base.adresse,
        base.adresseBuilder,
        base.adressetype,
        base.periodeFra,
        base.periodeTil,
        base.tilUbestemt,
    )

    fun fra(periodeFra: LocalDate?): AdressetilknytningBuilder {
        this.periodeFra = periodeFra
        return this
    }

    fun fra(periodeFra: String?): AdressetilknytningBuilder = fra(DateUtils.parseDate(periodeFra))

    fun til(periodeTil: LocalDate?): AdressetilknytningBuilder {
        this.periodeTil = periodeTil
        this.tilUbestemt = false
        return this
    }

    fun til(periodeTil: String?): AdressetilknytningBuilder = til(DateUtils.parseDate(periodeTil))

    fun tilUbestemt(): AdressetilknytningBuilder {
        this.periodeTil = null
        this.tilUbestemt = true
        return this
    }

    fun opprett(): Adressetilknytning =
        opprett(null)
            ?: throw IllegalArgumentException("PeriodeTil er ikke etter PeriodeFra.")

    fun opprett(tidligsteFra: LocalDate?): Adressetilknytning? {
        val adresse = this.adresse ?: adresseBuilder?.opprett() ?: return null
        val periodeFra = this.periodeFra ?: tidligsteFra ?: LocalDate.now()

        if (periodeTil != null && !periodeTil!!.isAfter(periodeFra)) {
            return null
        }

        return Adressetilknytning(adresse, adressetype, periodeFra, periodeTil)
    }

    companion object {
        fun lagAdressehistorikk(
            adresser: Iterable<AdressetilknytningBuilder>,
            fraDato: LocalDate?,
        ): MutableList<Adressetilknytning> {
            val adressehistorikk = mutableListOf<Adressetilknytning>()
            for (builder in periodiser(adresser, fraDato)) {
                builder
                    .opprett(fraDato)
                    ?.let { adressehistorikk.add(it) }
            }
            return adressehistorikk
        }

        private fun periodiser(
            adresser: Iterable<AdressetilknytningBuilder>,
            fraDato: LocalDate?,
        ): MutableList<AdressetilknytningBuilder> {
            val adressehistorikk = mutableListOf<AdressetilknytningBuilder>()
            val forrigePeriodePrType: MutableMap<Adressetype?, AdressetilknytningBuilder?> =
                mutableMapOf()
            for (baseBuilder in adresser) {
                val builder = AdressetilknytningBuilder(baseBuilder)
                val forrigeBuilder = forrigePeriodePrType[builder.adressetype]
                if (forrigeBuilder == null && builder.periodeFra == null) {
                    builder.periodeFra = fraDato
                }
                requireNotNull(builder.periodeFra) { "Periode fra må være satt for adressetilknytning (unntatt første)" }
                if (builder.periodeFra!!.isBefore(fraDato)) {
                    builder.periodeFra = fraDato
                }
                if (forrigeBuilder != null && !forrigeBuilder.tilUbestemt && forrigeBuilder.periodeTil == null) {
                    forrigeBuilder.periodeTil = builder.periodeFra
                }
                adressehistorikk.add(builder)
                forrigePeriodePrType[builder.adressetype] = builder
            }
            return adressehistorikk
        }
    }
}
