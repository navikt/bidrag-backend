@file:Suppress("unused")

package no.nav.bidrag.domene.tid

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.YearMonth

data class ÅrMånedsperiode(
    @Schema(example = "2023-01", type = "String", pattern = "YYYY-MM") override val fom: YearMonth,
    @Schema(example = "2023-01", type = "String", pattern = "YYYY-MM") override val til: YearMonth?,
) : Periode<YearMonth>() {
    init {
        validate()
    }

    constructor(fom: LocalDate, til: LocalDate?) : this(YearMonth.from(fom), til?.let { YearMonth.from(til) })

    constructor(fom: String, til: String) : this(YearMonth.parse(fom), YearMonth.parse(til))

    constructor(periode: Pair<String, String>) : this(periode.first, periode.second)

    override fun lagPeriode(
        fom: YearMonth,
        til: YearMonth?,
    ): ÅrMånedsperiode = ÅrMånedsperiode(fom, til)

    override infix fun union(annen: Periode<YearMonth>): ÅrMånedsperiode = super.union(annen) as ÅrMånedsperiode

    override infix fun snitt(annen: Periode<YearMonth>): ÅrMånedsperiode? = super.snitt(annen) as ÅrMånedsperiode?

    override infix fun påfølgesAv(påfølgende: Periode<YearMonth>): Boolean = this.til?.plusMonths(1) == påfølgende.fom

    override fun tilEllerMax(): YearMonth = til ?: YearMonth.from(LocalDate.MAX)

    override fun lengdeIHeleMåneder(): Long =
        (tilEllerMax().minusMonths(1).year * 12 + tilEllerMax().monthValue) - (fom.year * 12 + fom.monthValue) + 1L

    fun toDatoperiode() = Datoperiode(fom, til)
}
