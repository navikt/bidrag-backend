package no.nav.bidrag.beregn.barnebidrag.beregning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EndringSjekkGrenseBeregningTest {
    private fun grunnlag(
        referanse: String,
        fom: YearMonth,
        tom: YearMonth?,
        overGrense: Boolean,
    ) = EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag(
        referanse = referanse,
        periode = ÅrMånedsperiode(fom, tom),
        endringErOverGrense = overGrense,
        løpendeBidragBeløp = BigDecimal(1000),
        beregnetBidragBeløp = BigDecimal(2000),
    )

    @Test
    fun `skal returnere tomt resultat uten perioder`() {
        EndringSjekkGrenseBeregning.beregnV2(emptyList()).shouldBeEmpty()
    }

    @Test
    fun `skal sette alle perioder over grense hvis første periode er over grense`() {
        val resultat =
            EndringSjekkGrenseBeregning.beregnV2(
                listOf(
                    grunnlag("a", YearMonth.of(2024, 1), YearMonth.of(2024, 6), true),
                    grunnlag("b", YearMonth.of(2024, 6), null, false),
                ),
            )

        resultat shouldHaveSize 2
        resultat.all { it.endringErOverGrense } shouldBe true
    }

    @Test
    fun `skal sette alle perioder under grense hvis ingen perioder er over grense`() {
        val resultat =
            EndringSjekkGrenseBeregning.beregnV2(
                listOf(
                    grunnlag("a", YearMonth.of(2024, 1), YearMonth.of(2024, 6), false),
                    grunnlag("b", YearMonth.of(2024, 6), null, false),
                ),
            )

        resultat.none { it.endringErOverGrense } shouldBe true
    }

    @Test
    fun `skal sette over grense fra første periode over grense`() {
        val resultat =
            EndringSjekkGrenseBeregning.beregnV2(
                listOf(
                    grunnlag("a", YearMonth.of(2024, 1), YearMonth.of(2024, 6), false),
                    grunnlag("b", YearMonth.of(2024, 6), null, true),
                ),
            )

        resultat.map { it.endringErOverGrense } shouldBe listOf(false, true)
    }
}
