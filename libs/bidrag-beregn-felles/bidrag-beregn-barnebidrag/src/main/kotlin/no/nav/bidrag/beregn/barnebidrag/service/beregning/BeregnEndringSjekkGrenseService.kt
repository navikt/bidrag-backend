package no.nav.bidrag.beregn.barnebidrag.service.beregning

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.barnebidrag.beregning.EndringSjekkGrenseBeregning
import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrenseBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.mapper.EndringSjekkGrenseMapper
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.beregn.core.service.BeregnService
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.avrundetMedToDesimaler
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningEndringSjekkGrense
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.opprettDelberegningreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.tilInnholdMedReferanse

internal object BeregnEndringSjekkGrenseService : BeregnService() {

    fun delberegningEndringSjekkGrense(mottattGrunnlag: BeregnGrunnlag, åpenSluttperiode: Boolean = true): List<GrunnlagDto> {
        // Switch for å bestemme om ny eller gammel beregningsregel skal brukes
        val ny12ProsentRegel = BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT.isEnabled

        // Mapper ut grunnlag som skal brukes i beregningen
        val periodeGrunnlag = EndringSjekkGrenseMapper.mapEndringSjekkGrenseGrunnlag(mottattGrunnlag)

        val beregningGrunnlagListe = mutableListOf<EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag>()

        // Bygger opp grunnlag til beregningen
        periodeGrunnlag.endringSjekkGrensePeriodePeriodeGrunnlagListe.forEach {
            beregningGrunnlagListe.add(
                EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag(
                    referanse = it.referanse,
                    periode = it.endringSjekkGrensePeriodePeriode.periode,
                    endringErOverGrense = it.endringSjekkGrensePeriodePeriode.endringErOverGrense,
                    løpendeBidragBeløp = it.endringSjekkGrensePeriodePeriode.løpendeBidragBeløp,
                    beregnetBidragBeløp = it.endringSjekkGrensePeriodePeriode.beregnetBidragBeløp?.avrundetMedToDesimaler,
                ),
            )
        }

        // Beregningen kalles en gang med alle perioder som input
        val beregningResultatListe = (
            if (ny12ProsentRegel) {
                EndringSjekkGrenseBeregning.beregnV2(beregningGrunnlagListe)
            } else {
                EndringSjekkGrenseBeregning.beregn(beregningGrunnlagListe)
            }
            ).toMutableList()

        // Setter til-periode i siste element til null hvis det ikke allerede er det og åpenSluttperiode er true
        if (ny12ProsentRegel) {
            if (beregningResultatListe.isNotEmpty()) {
                val sisteElement = beregningResultatListe.last()
                if (sisteElement.periode != null) {
                    if (sisteElement.periode.til?.equals(mottattGrunnlag.periode.til) == true && åpenSluttperiode) {
                        beregningResultatListe[beregningResultatListe.size - 1] = sisteElement.copy(periode = sisteElement.periode.copy(til = null))
                    } else if (sisteElement.periode.til != null && mottattGrunnlag.opphørsdato != null) {
                        beregningResultatListe[beregningResultatListe.size - 1] =
                            sisteElement.copy(periode = sisteElement.periode.copy(til = mottattGrunnlag.opphørsdato))
                    }
                }
            }
        }

        // Mapper ut grunnlag som er brukt i beregningen (mottatte grunnlag)
        val resultatGrunnlagListe = mapDelberegningResultatGrunnlag(
            grunnlagReferanseListe = beregningResultatListe
                .flatMap { it.grunnlagsreferanseListe }
                .distinct(),
            mottattGrunnlag = mottattGrunnlag,
            sjablonGrunnlag = emptyList(),
        )

        // Mapper ut grunnlag for delberegning endring sjekk grense
        resultatGrunnlagListe.addAll(
            mapDelberegningEndringSjekkGrense(
                beregningResultatListe = beregningResultatListe,
                mottattGrunnlag = mottattGrunnlag,
                åpenSluttperiode = åpenSluttperiode,
            ),
        )

        // Mapper ut grunnlag for Person-objekter som er brukt
        resultatGrunnlagListe.addAll(
            mapPersonobjektGrunnlag(
                resultatGrunnlagListe = resultatGrunnlagListe,
                personobjektGrunnlagListe = mottattGrunnlag.grunnlagListe,
            ),
        )

        return slåSammenLikeObjekter(
            grunnlagsliste = resultatGrunnlagListe.distinctBy { it.referanse }.sortedBy { it.referanse },
            grunnlagstype = Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE,
            utvidPeriodeTil = ::utvidEndringSjekkGrensePeriodeTil,
        )
    }

    // Spesifikk implementasjon for å utvide periode for DelberegningEndringSjekkGrense. Slår sammen fra-periode fra forrige objekt og til-periode fra
    // nåværende objekt og beholder resten av innholdet fra forrige objekt.
    private fun utvidEndringSjekkGrensePeriodeTil(forrigeGrunnlag: GrunnlagDto, nåværendeGrunnlag: GrunnlagDto): GrunnlagDto {
        val forrigeEndringSjekkGrense = forrigeGrunnlag.tilInnholdMedReferanse<DelberegningEndringSjekkGrense>()
        val nåværendeEndringSjekkGrense = nåværendeGrunnlag.tilInnholdMedReferanse<DelberegningEndringSjekkGrense>()

        val utvidetPeriode = forrigeEndringSjekkGrense.innhold.periode.copy(
            til = nåværendeEndringSjekkGrense.innhold.periode.til,
        )
        val utvidetInnhold = forrigeEndringSjekkGrense.innhold.copy(periode = utvidetPeriode)

        return forrigeGrunnlag.copy(innhold = POJONode(utvidetInnhold))
    }

    // Mapper ut DelberegningEndringSjekkGrense
    private fun mapDelberegningEndringSjekkGrense(
        beregningResultatListe: List<EndringSjekkGrenseBeregningResultat>,
        mottattGrunnlag: BeregnGrunnlag,
        åpenSluttperiode: Boolean,
    ): List<GrunnlagDto> = beregningResultatListe
        .map {
            GrunnlagDto(
                referanse = opprettDelberegningreferanse(
                    type = Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE,
                    periode = ÅrMånedsperiode(fom = it.periode?.fom ?: mottattGrunnlag.periode.fom, til = null),
                    søknadsbarnReferanse = mottattGrunnlag.søknadsbarnReferanse,
                ),
                type = Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE,
                innhold = POJONode(
                    DelberegningEndringSjekkGrense(
                        periode = ÅrMånedsperiode(
                            fom = it.periode?.fom ?: mottattGrunnlag.periode.fom,
                            til = if (it.periode != null) {
                                it.periode.til
                            } else if (åpenSluttperiode) {
                                null
                            } else {
                                mottattGrunnlag.periode.til
                            },
                        ),
                        endringErOverGrense = it.endringErOverGrense,
                    ),
                ),
                grunnlagsreferanseListe = it.grunnlagsreferanseListe,
                gjelderBarnReferanse = mottattGrunnlag.søknadsbarnReferanse,
            )
        }
}
