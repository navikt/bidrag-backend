package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.generer.testdata.RandomTestData
import java.time.LocalDate

@Suppress("unused")
enum class IdentTyper : IdentType {
    FNR {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .withPersonnummer(kjønn, fodtDato.year)
                .toString()
    },

    DNR {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .increaseDigit(0, 4)
                .withPersonnummer(kjønn, fodtDato.year)
                .toString()
    },

    BNR {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .increaseDigit(2, 2)
                .withPersonnummer(kjønn, fodtDato.year)
                .toString()
    },

    NPID {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(
                    RandomTestData.random().dateBetween(LocalDate.of(1859, 1, 1), LocalDate.of(2040, 1, 1)),
                ).increaseDigit(2, 2)
                .withPersonnummer(kjønn, fodtDato.year)
                .toString()
    },

    DOLLY {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .increaseDigit(2, 4)
                .withPersonnummer(kjønn, fodtDato.year)
                .toString()
    },

    TENOR {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .increaseDigit(2, 8)
                .withPersonnummer(kjønn, fodtDato.year)
                .toString()
    },

    DODFODT {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .withDodfodtNr(++IdentTyper.dodfodtNr)
                .toString()
    },

    UGYLDIG {
        override fun generer(
            fodtDato: LocalDate,
            kjønn: Kjønn?,
        ): String =
            IdentBuffer()
                .withDate(fodtDato)
                .withPersonnummer(kjønn, fodtDato.year)
                ?.set(2, 2, 19)
                .toString()
    }, ;

    companion object {
        private var dodfodtNr = 0
    }
}
