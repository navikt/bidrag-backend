package no.nav.bidrag.generer.testdata.tid

import no.nav.bidrag.generer.testdata.tid.DateBuilder.Companion.forsteDag
import no.nav.bidrag.generer.testdata.tid.DateBuilder.Companion.sisteDag
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import java.time.LocalDate

class DateBuilderTest {
    @Test
    fun testIDag() {
        Tester(DateBuilder(null, null, null))
            .assertForDate(
                LocalDate.of(2022, 9, 24),
                Matchers.`is`<LocalDate?>(Matchers.equalTo(LocalDate.of(2022, 9, 24))),
            )
    }

    @Test
    fun testForsteDag() {
        Tester(forsteDag().iDenneMaaned().detteAaret())
            .assertForDate("2022-01-15", "2022-01-01")
            .assertForDate("2022-09-01", "2022-09-01")
            .assertForDate("2022-09-24", "2022-09-01")
            .assertForDate("2022-09-30", "2022-09-01")
    }

    @Test
    fun testSisteDag() {
        Tester(sisteDag().iDenneMaaned().detteAaret())
            .assertForDate("2022-01-15", "2022-01-31")
            .assertForDate("2022-09-01", "2022-09-30")
            .assertForDate("2022-09-24", "2022-09-30")
            .assertForDate("2022-09-30", "2022-09-30")
    }

    @Test
    fun testSisteDagForrigeMaaned() {
        Tester(sisteDag().iForrigeMaaned().detteAaret())
            .assertForDate("2020-03-31", "2020-02-29")
            .assertForDate("2022-01-15", "2021-12-31")
            .assertForDate("2022-03-31", "2022-02-28")
            .assertForDate("2022-09-01", "2022-08-31")
            .assertForDate("2022-09-24", "2022-08-31")
            .assertForDate("2022-09-30", "2022-08-31")
    }

    @Test
    fun testForsteDagNesteMaaned() {
        Tester(forsteDag().iNesteMaaned().detteAaret())
            .assertForDate("2020-03-31", "2020-04-01")
            .assertForDate("2022-01-15", "2022-02-01")
            .assertForDate("2022-03-31", "2022-04-01")
            .assertForDate("2022-09-01", "2022-10-01")
            .assertForDate("2022-09-24", "2022-10-01")
            .assertForDate("2022-09-30", "2022-10-01")
    }

    class Tester(
        private val dateBuilder: DateBuilder? = null,
    ) {
        fun getForDate(relativeTo: LocalDate?): LocalDate? = DateBuilder({ _: LocalDate? -> relativeTo }, dateBuilder, null).get()

        fun assertForDate(
            relativeTo: LocalDate?,
            matcher: Matcher<LocalDate?>,
        ): Tester {
            MatcherAssert.assertThat<LocalDate?>(getForDate(relativeTo), matcher)
            return this
        }

        fun assertForDate(
            relativeTo: String,
            expected: String?,
        ): Tester {
            MatcherAssert.assertThat(
                getForDate(LocalDate.parse(relativeTo)).toString(),
                Matchers.`is`(Matchers.equalTo<String?>(expected)),
            )
            return this
        }
    }
}
