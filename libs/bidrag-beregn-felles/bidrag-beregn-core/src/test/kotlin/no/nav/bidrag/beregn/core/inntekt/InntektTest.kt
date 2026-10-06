package no.nav.bidrag.beregn.core.inntekt

import no.nav.bidrag.beregn.core.TestUtil.byggInntektGrunnlagUtvidetBarnetrygdFull
import no.nav.bidrag.beregn.core.TestUtil.byggInntektGrunnlagUtvidetBarnetrygdOvergang
import no.nav.bidrag.beregn.core.TestUtil.byggSjablontallGrunnlagUtvidetBarnetrygdFull
import no.nav.bidrag.beregn.core.TestUtil.byggSjablontallGrunnlagUtvidetBarnetrygdOvergang
import no.nav.bidrag.beregn.core.bo.Avvik
import no.nav.bidrag.beregn.core.util.InntektUtil.behandlUtvidetBarnetrygd
import no.nav.bidrag.beregn.core.util.InntektUtil.inneholderSkattefrieBarnetillegg
import no.nav.bidrag.beregn.core.util.InntektUtil.justerForSkattefrieBarnetillegg
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("InntektValidatorTest")
internal class InntektTest {
    private var inntektPeriodeGrunnlagListe = listOf<InntektPeriodeGrunnlag>()
    private var avvikListe = listOf<Avvik>()

    @Test
    @DisplayName("Utvidet barnetrygd - full test av regelverk")
    fun testUtvidetBarnetrygdFullTest() {
        val nyInntektGrunnlagListe =
            behandlUtvidetBarnetrygd(
                inntektPeriodeGrunnlagListe = byggInntektGrunnlagUtvidetBarnetrygdFull(),
                sjablonPeriodeListe = byggSjablontallGrunnlagUtvidetBarnetrygdFull(),
            )

        assertAll(
            { assertThat(nyInntektGrunnlagListe).isNotEmpty() },
            { assertThat(nyInntektGrunnlagListe.size).isEqualTo(14) },
            { assertThat(nyInntektGrunnlagListe[9].getPeriode().datoFom).isEqualTo(LocalDate.parse("2019-04-01")) },
            { assertThat(nyInntektGrunnlagListe[9].getPeriode().datoTil).isEqualTo(LocalDate.parse("2019-06-01")) },
            { assertThat(nyInntektGrunnlagListe[9].belop).isEqualTo(BigDecimal.valueOf(13000)) },
            { assertThat(nyInntektGrunnlagListe[10].getPeriode().datoFom).isEqualTo(LocalDate.parse("2019-06-01")) },
            { assertThat(nyInntektGrunnlagListe[10].getPeriode().datoTil).isEqualTo(LocalDate.parse("2019-08-01")) },
            { assertThat(nyInntektGrunnlagListe[10].belop).isEqualTo(BigDecimal.valueOf(6500)) },
            { assertThat(nyInntektGrunnlagListe[11].getPeriode().datoFom).isEqualTo(LocalDate.parse("2020-04-01")) },
            { assertThat(nyInntektGrunnlagListe[11].getPeriode().datoTil).isEqualTo(LocalDate.parse("2020-07-01")) },
            { assertThat(nyInntektGrunnlagListe[11].belop).isEqualTo(BigDecimal.valueOf(13000)) },
            { assertThat(nyInntektGrunnlagListe[12].getPeriode().datoFom).isEqualTo(LocalDate.parse("2020-07-01")) },
            { assertThat(nyInntektGrunnlagListe[12].getPeriode().datoTil).isEqualTo(LocalDate.parse("2020-08-01")) },
            { assertThat(nyInntektGrunnlagListe[12].belop).isEqualTo(BigDecimal.valueOf(14000)) },
            { assertThat(nyInntektGrunnlagListe[13].getPeriode().datoFom).isEqualTo(LocalDate.parse("2020-08-01")) },
            { assertThat(nyInntektGrunnlagListe[13].getPeriode().datoTil).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(nyInntektGrunnlagListe[13].belop).isEqualTo(BigDecimal.valueOf(7000)) },
        )
    }

    @Test
    @DisplayName("Utvidet barnetrygd - test av overgang mellom regelverk for skatteklasse 2 og fordel særfradrag")
    fun testUtvidetBarnetrygdOvergangSkatteklasse2FordelSærfradrag() {
        val nyInntektGrunnlagListe =
            behandlUtvidetBarnetrygd(
                inntektPeriodeGrunnlagListe = byggInntektGrunnlagUtvidetBarnetrygdOvergang(),
                sjablonPeriodeListe = byggSjablontallGrunnlagUtvidetBarnetrygdOvergang(),
            )

        assertAll(
            { assertThat(nyInntektGrunnlagListe).isNotEmpty() },
            { assertThat(nyInntektGrunnlagListe.size).isEqualTo(5) },
            { assertThat(nyInntektGrunnlagListe[2].getPeriode().datoFom).isEqualTo(LocalDate.parse("2012-06-01")) },
            { assertThat(nyInntektGrunnlagListe[2].getPeriode().datoTil).isEqualTo(LocalDate.parse("2012-07-01")) },
            { assertThat(nyInntektGrunnlagListe[2].belop).isEqualTo(BigDecimal.valueOf(7500)) },
            { assertThat(nyInntektGrunnlagListe[3].getPeriode().datoFom).isEqualTo(LocalDate.parse("2012-07-01")) },
            { assertThat(nyInntektGrunnlagListe[3].getPeriode().datoTil).isEqualTo(LocalDate.parse("2013-01-01")) },
            { assertThat(nyInntektGrunnlagListe[3].belop).isEqualTo(BigDecimal.valueOf(8500)) },
            { assertThat(nyInntektGrunnlagListe[4].getPeriode().datoFom).isEqualTo(LocalDate.parse("2013-01-01")) },
            { assertThat(nyInntektGrunnlagListe[4].getPeriode().datoTil).isEqualTo(LocalDate.parse("2013-06-01")) },
            { assertThat(nyInntektGrunnlagListe[4].belop).isEqualTo(BigDecimal.valueOf(12500)) },
        )
    }

    @Test
    @DisplayName("Barnetillegg fra Forsvaret er skattefritt og skal trekkes ut av inntekten som summeres")
    fun testSkattefrittBarnetilleggForsvaretTrekkesUtAvInntekt() {
        val barnetillegg = byggBarnetilleggInntekt(
            InntektsrapporteringPeriode.Inntektspost("Inntekt", Inntektstype.BARNETILLEGG_PENSJON, BigDecimal.valueOf(20400), InntektBeløpstype.ÅRSBELØP),
            InntektsrapporteringPeriode.Inntektspost("Inntekt", Inntektstype.BARNETILLEGG_FORSVARET, BigDecimal.valueOf(2500), InntektBeløpstype.MÅNEDSBELØP),
        )

        assertAll(
            { assertThat(inneholderSkattefrieBarnetillegg(barnetillegg)).isTrue() },
            { assertThat(justerForSkattefrieBarnetillegg(barnetillegg)).isEqualByComparingTo(BigDecimal.valueOf(20400)) },
        )
    }

    @Test
    @DisplayName("Barnetillegg som ikke er skattefritt skal ikke trekkes ut av inntekten")
    fun testSkattepliktigBarnetilleggTrekkesIkkeUtAvInntekt() {
        val barnetillegg = byggBarnetilleggInntekt(
            InntektsrapporteringPeriode.Inntektspost("Inntekt", Inntektstype.BARNETILLEGG_PENSJON, BigDecimal.valueOf(20400), InntektBeløpstype.ÅRSBELØP),
        )

        assertThat(inneholderSkattefrieBarnetillegg(barnetillegg)).isFalse()
    }

    private fun byggBarnetilleggInntekt(vararg inntektsposter: InntektsrapporteringPeriode.Inntektspost) = InntektsrapporteringPeriode(
        periode = ÅrMånedsperiode("2024-08", "2024-09"),
        manueltRegistrert = true,
        inntektsrapportering = Inntektsrapportering.BARNETILLEGG,
        beløp = inntektsposter.sumOf { if (it.beløpstype == InntektBeløpstype.MÅNEDSBELØP) it.beløp.multiply(BigDecimal.valueOf(12)) else it.beløp },
        valgt = true,
        inntektspostListe = inntektsposter.toList(),
    )
}
