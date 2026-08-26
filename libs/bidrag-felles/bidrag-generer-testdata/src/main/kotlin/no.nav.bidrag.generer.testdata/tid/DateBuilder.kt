package no.nav.bidrag.generer.testdata.tid

import java.time.LocalDate
import java.util.function.Function
import kotlin.random.Random

// TODO(Skal denne klassen brukes i det hele tatt?)
@Suppress("unused")
class DateBuilder internal constructor(
    prepend: Function<in LocalDate?, out LocalDate?>?,
    parent: DateBuilder?,
    append: Function<in LocalDate?, out LocalDate?>?,
) {
    private val modifiers: MutableList<Function<in LocalDate?, out LocalDate?>> =
        ArrayList<Function<in LocalDate?, out LocalDate?>>()

    init {
        if (prepend != null) {
            modifiers.add(prepend)
        }
        if (parent != null) {
            modifiers.addAll(parent.modifiers)
        }
        if (append != null) {
            modifiers.add(append)
        }
    }

    class MaanedBuilder(
        private val dateBuilder: DateBuilder?,
    ) {
        fun iForrigeMaaned(): AarBuilder = AarBuilder(DateBuilder(Function { d: LocalDate? -> d!!.minusMonths(1) }, dateBuilder, null))

        fun iDenneMaaned(): AarBuilder = AarBuilder(dateBuilder)

        fun iNesteMaaned(): AarBuilder = AarBuilder(DateBuilder(Function { d: LocalDate? -> d!!.plusMonths(1) }, dateBuilder, null))

        fun omNMaaneder(antallMaaneder: Int): AarBuilder =
            AarBuilder(
                DateBuilder(
                    Function { d: LocalDate -> d.plusMonths(antallMaaneder.toLong()) },
                    dateBuilder,
                    null,
                ),
            )
    }

    class AarBuilder(
        private val dateBuilder: DateBuilder?,
    ) {
        fun nAarSiden(antallAarSiden: Int): DateBuilder =
            DateBuilder(Function { d: LocalDate -> d.minusYears(antallAarSiden.toLong()) }, dateBuilder, null)

        fun detteAaret(): DateBuilder = dateBuilder!!

        fun omNAar(antallAar: Int): DateBuilder =
            DateBuilder(Function { d: LocalDate -> d.plusYears(antallAar.toLong()) }, dateBuilder, null)
    }

    fun get(): LocalDate? {
        var date = LocalDate.now()
        for (modifier in modifiers) {
            date = modifier.apply(date)
        }
        return date
    }

    companion object {
        @JvmStatic
        fun forsteDag(): MaanedBuilder = MaanedBuilder(DateBuilder(Function { d: LocalDate? -> d!!.withDayOfMonth(1) }, null, null))

        fun denneDag(): MaanedBuilder = MaanedBuilder(DateBuilder(null, null, null))

        @JvmStatic
        fun sisteDag(): MaanedBuilder =
            MaanedBuilder(
                DateBuilder(
                    Function { d: LocalDate -> d.withDayOfMonth(d.lengthOfMonth()) },
                    null,
                    null,
                ),
            )

        fun tilfeldigDag(): MaanedBuilder =
            MaanedBuilder(
                DateBuilder(
                    Function { d: LocalDate ->
                        d.withDayOfMonth(
                            Random.nextInt(d.lengthOfMonth() + 1),
                        )
                    },
                    null,
                    null,
                ),
            )

        fun dag(dagNr: Int): MaanedBuilder = MaanedBuilder(DateBuilder(Function { d: LocalDate? -> d!!.withDayOfMonth(dagNr) }, null, null))

        fun iMorgen(): AarBuilder = AarBuilder(DateBuilder(Function { d: LocalDate? -> d!!.plusDays(1) }, null, null))
    }
}
