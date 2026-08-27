package no.nav.bidrag.beregn.barnebidrag.mapper

import no.nav.bidrag.beregn.barnebidrag.bo.BarnetilleggPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.NettoBarnetilleggPeriodeGrunnlag
import no.nav.bidrag.beregn.core.service.mapper.CoreMapper
import no.nav.bidrag.beregn.core.util.InntektUtil.beløpTilÅrsbeløp
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.BarnetilleggPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåFremmedReferanse

internal object NettoBarnetilleggMapper : CoreMapper() {
    fun mapNettoBarnetilleggGrunnlag(mottattGrunnlag: BeregnGrunnlag, referanseTilRolle: String): NettoBarnetilleggPeriodeGrunnlag =
        NettoBarnetilleggPeriodeGrunnlag(
            beregningsperiode = mottattGrunnlag.periode,
            barnetilleggPeriodeGrunnlagListe = mapBarnetillegg(beregnGrunnlag = mottattGrunnlag, referanseTilRolle),
        )

    private fun mapBarnetillegg(beregnGrunnlag: BeregnGrunnlag, referanseTilRolle: String): List<BarnetilleggPeriodeGrunnlag> {
        try {
            return beregnGrunnlag.grunnlagListe
                .filtrerOgKonverterBasertPåFremmedReferanse<InntektsrapporteringPeriode>(
                    grunnlagType = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
                    referanse = referanseTilRolle,
                )
                .filter { it.innhold.inntektsrapportering == Inntektsrapportering.BARNETILLEGG }
                .filter { it.innhold.gjelderBarn == beregnGrunnlag.søknadsbarnReferanse }
                .flatMap {
                    it.innhold.inntektspostListe.mapNotNull { inntektspost ->
                        inntektspost.inntektstype?.let { inntektstype ->
                            BarnetilleggPeriodeGrunnlag(
                                referanse = it.referanse,
                                barnetilleggPeriode = BarnetilleggPeriode(
                                    periode = it.innhold.periode,
                                    type = inntektstype,
                                    beløp = inntektspost.beløp.beløpTilÅrsbeløp(inntektspost.beløpstype),
                                    skattefaktor = inntektspost.skattefaktor,
                                    manueltRegistrert = false,
                                ),
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Ugyldig input ved beregning av barnebidrag. Innhold i Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE - BARNETILLEGG er ikke gyldig: " +
                    e.message,
            )
        }
    }
}
