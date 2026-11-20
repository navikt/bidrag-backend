package no.nav.bidrag.beregn.core.service

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutaPar
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutakursGrunnlag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.YearMonth

internal class BeregnServiceTest {
    private val service = object : BeregnService() {}

    @ParameterizedTest
    @ValueSource(strings = ["2024-07", "2025-01"])
    fun `skal velge kurs for halvåret uavhengig av rekkefølge og tilstøtende perioder`(halvårsstart: YearMonth) {
        val riktig = kursgrunnlag(ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(6)))
        val grunnlag = listOf(
            kursgrunnlag(ÅrMånedsperiode(halvårsstart.plusMonths(6), halvårsstart.plusMonths(12))),
            kursgrunnlag(ÅrMånedsperiode(halvårsstart.minusMonths(6), halvårsstart)),
            riktig,
        )

        service.hentValutakursGrunnlag(grunnlag, ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(6)), Valutakode.EUR) shouldBe riktig
    }

    @Test
    fun `skal returnere uten kursgrunnlag for NOK`() {
        service.hentValutakursGrunnlag(emptyList(), ÅrMånedsperiode("2024-07", "2024-08"), Valutakode.NOK) shouldBe null
    }

    @Test
    fun `skal avvise valutakursgrunnlag uten periode ved beregning`() {
        val grunnlag = GrunnlagDto(
            referanse = "innhentet_valutakurs_grunnlag",
            type = Grunnlagstype.VALUTAKURS_GRUNNLAG,
            innhold = POJONode(ValutakursGrunnlag(valutakursListe = listOf(ValutaPar(Valutakode.EUR, Valutakode.NOK, BigDecimal.TEN)))),
        )

        shouldThrow<IllegalArgumentException> {
            service.hentValutakursGrunnlag(listOf(grunnlag), ÅrMånedsperiode("2024-07", "2024-08"), Valutakode.EUR)
        }.message shouldBe "Valutakursgrunnlaget må ha en periode ved beregning"
    }

    @Test
    fun `skal avvise manglende eller feil valutapar`() {
        val periode = ÅrMånedsperiode("2024-07", "2024-08")
        val feilValuta = kursgrunnlag(periode, par = ValutaPar(Valutakode.USD, Valutakode.NOK, BigDecimal.TEN))
        val inversKurs = kursgrunnlag(periode, par = ValutaPar(Valutakode.NOK, Valutakode.EUR, BigDecimal("0.1")))

        listOf(emptyList(), listOf(feilValuta), listOf(inversKurs)).forEach { grunnlag ->
            shouldThrow<IllegalArgumentException> {
                service.hentValutakursGrunnlag(grunnlag, periode, Valutakode.EUR)
            }.message shouldBe "Valutakursgrunnlag mangler for valuta og beregningsperiode"
        }
    }

    @Test
    fun `skal velge grunnlaget for riktig valuta når flere valutaer har samme periode`() {
        val periode = ÅrMånedsperiode("2024-07", "2025-01")
        val euro = kursgrunnlag(periode)
        val dollar = kursgrunnlag(periode, par = ValutaPar(Valutakode.USD, Valutakode.NOK, BigDecimal.TEN))

        service.hentValutakursGrunnlag(listOf(dollar, euro), periode, Valutakode.EUR) shouldBe euro
    }

    @Test
    fun `skal avvise overlappende grunnlag for samme valuta`() {
        val periode = ÅrMånedsperiode("2024-07", "2024-08")
        val grunnlag = listOf(
            kursgrunnlag(ÅrMånedsperiode("2024-01", "2025-01")),
            kursgrunnlag(ÅrMånedsperiode("2024-07", "2025-01")),
        )

        shouldThrow<IllegalArgumentException> {
            service.hentValutakursGrunnlag(grunnlag, periode, Valutakode.EUR)
        }.message shouldBe "Flere valutakursgrunnlag dekker samme valuta og beregningsperiode"
    }

    @Test
    fun `skal avvise kurs som bare dekker deler av beregningsperioden`() {
        val grunnlag = listOf(kursgrunnlag(ÅrMånedsperiode("2024-07", "2024-08")))

        listOf(ÅrMånedsperiode("2024-06", "2024-08"), ÅrMånedsperiode("2024-07", "2024-09")).forEach { periode ->
            shouldThrow<IllegalArgumentException> {
                service.hentValutakursGrunnlag(grunnlag, periode, Valutakode.EUR)
            }.message shouldBe "Valutakursgrunnlaget må dekke hele beregningsperioden"
        }
    }

    @Test
    fun `skal kreve åpen kursperiode når beregningsperioden er åpen`() {
        val periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null)
        val åpentGrunnlag = kursgrunnlag(periode)

        service.hentValutakursGrunnlag(listOf(åpentGrunnlag), periode, Valutakode.EUR) shouldBe åpentGrunnlag
        shouldThrow<IllegalArgumentException> {
            service.hentValutakursGrunnlag(
                listOf(kursgrunnlag(ÅrMånedsperiode("2024-07", "2025-01"))),
                periode,
                Valutakode.EUR,
            )
        }.message shouldBe "Valutakursgrunnlaget må dekke hele beregningsperioden"
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-1"])
    fun `skal avvise kurs som ikke er positiv`(kurs: String) {
        val periode = ÅrMånedsperiode("2024-07", "2025-01")
        val grunnlag = listOf(kursgrunnlag(periode, par = ValutaPar(Valutakode.EUR, Valutakode.NOK, BigDecimal(kurs))))

        shouldThrow<IllegalArgumentException> {
            service.hentValutakursGrunnlag(grunnlag, periode, Valutakode.EUR)
        }.message shouldBe "Valutakurs til NOK må være større enn null"
    }

    private fun kursgrunnlag(
        periode: ÅrMånedsperiode,
        par: ValutaPar = ValutaPar(Valutakode.EUR, Valutakode.NOK, BigDecimal.TEN),
    ) = GrunnlagDto(
        referanse = "kurs_${par.valutakode1}_${periode.fom}",
        type = Grunnlagstype.VALUTAKURS_GRUNNLAG,
        innhold = POJONode(ValutakursGrunnlag(periode = periode, valutakursListe = listOf(par))),
    )
}
