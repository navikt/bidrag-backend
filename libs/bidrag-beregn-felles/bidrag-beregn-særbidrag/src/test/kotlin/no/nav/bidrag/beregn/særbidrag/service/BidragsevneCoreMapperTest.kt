package no.nav.bidrag.beregn.særbidrag.service

import no.nav.bidrag.beregn.særbidrag.service.mapper.BidragsevneCoreMapper
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BidragsevneCoreMapperTest {
    @Test
    @DisplayName("Skattefrie barnetillegg (tiltakspenger og Forsvaret) tas ikke med når inntektene summeres")
    fun mapInntektUtenSkattefrieBarnetilleggTest() {
        val særbidragGrunnlag = BeregnGrunnlag(
            periode = ÅrMånedsperiode(fom = "2024-01", til = "2024-02"),
            søknadsbarnReferanse = "Person_Søknadsbarn",
            grunnlagListe = listOf(
                byggInntekt("Inntekt_Ainntekt", Inntektsrapportering.AINNTEKT, BigDecimal.valueOf(300000)),
                byggInntekt(
                    "Inntekt_Barnetillegg_Pensjon_Forsvaret",
                    Inntektsrapportering.BARNETILLEGG,
                    BigDecimal.valueOf(50400),
                    InntektsrapporteringPeriode.Inntektspost("Inntekt", Inntektstype.BARNETILLEGG_PENSJON, BigDecimal.valueOf(20400), InntektBeløpstype.ÅRSBELØP),
                    InntektsrapporteringPeriode.Inntektspost("Inntekt", Inntektstype.BARNETILLEGG_FORSVARET, BigDecimal.valueOf(2500), InntektBeløpstype.MÅNEDSBELØP),
                ),
                byggInntekt(
                    "Inntekt_Barnetillegg_Tiltakspenger",
                    Inntektsrapportering.BARNETILLEGG,
                    BigDecimal.valueOf(12000),
                    InntektsrapporteringPeriode.Inntektspost("Inntekt", Inntektstype.BARNETILLEGG_TILTAKSPENGER, BigDecimal.valueOf(1000), InntektBeløpstype.MÅNEDSBELØP),
                ),
            ),
        )

        val inntektPeriodeListe = BidragsevneCoreMapper.mapInntekt(
            beregnGrunnlag = særbidragGrunnlag,
            referanseTilRolle = "Person_Bidragspliktig",
            innslagKapitalinntektSjablonverdi = BigDecimal.valueOf(10000),
            erSærbidrag = true,
        )

        assertAll(
            { assertThat(inntektPeriodeListe).hasSize(1) },
            { assertThat(inntektPeriodeListe[0].beløp).isEqualByComparingTo(BigDecimal.valueOf(320400)) },
        )
    }

    private fun byggInntekt(
        referanse: String,
        inntektsrapportering: Inntektsrapportering,
        beløp: BigDecimal,
        vararg inntektsposter: InntektsrapporteringPeriode.Inntektspost,
    ) = GrunnlagDto(
        referanse = referanse,
        type = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
        gjelderReferanse = "Person_Bidragspliktig",
        innhold = commonObjectmapper.valueToTree(
            InntektsrapporteringPeriode(
                periode = ÅrMånedsperiode(fom = "2024-01", til = "2024-02"),
                manueltRegistrert = true,
                inntektsrapportering = inntektsrapportering,
                beløp = beløp,
                valgt = true,
                inntektspostListe = inntektsposter.toList(),
            ),
        ),
    )
}
