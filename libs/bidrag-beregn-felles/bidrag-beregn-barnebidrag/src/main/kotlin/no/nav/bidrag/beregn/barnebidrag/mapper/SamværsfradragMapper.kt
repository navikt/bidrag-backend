package no.nav.bidrag.beregn.barnebidrag.mapper

import no.nav.bidrag.beregn.barnebidrag.bo.SamværsfradragPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SamværsklassePeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SøknadsbarnPeriodeGrunnlag
import no.nav.bidrag.beregn.core.service.mapper.CoreMapper
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtalePeriodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SamværsklassePeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse

internal object SamværsfradragMapper : CoreMapper() {
    fun mapSamværsfradragGrunnlag(
        mottattGrunnlag: BeregnGrunnlag,
        sjablonGrunnlag: List<GrunnlagDto>,
        erLøpendeBidrag: Boolean = false,
        erPrivatAvtale: Boolean = false,
    ): SamværsfradragPeriodeGrunnlag = SamværsfradragPeriodeGrunnlag(
        beregningsperiode = mottattGrunnlag.periode,
        søknadsbarnPeriodeGrunnlag = mapSøknadsbarn(mottattGrunnlag),
        samværsklassePeriodeGrunnlagListe = if (erLøpendeBidrag) {
            mapSamværsklasseLøpendeBidrag(mottattGrunnlag)
        } else if (erPrivatAvtale) {
            mapSamværsklassePrivatAvtale(mottattGrunnlag)
        } else {
            mapSamværsklasse(mottattGrunnlag)
        },
        sjablonSamværsfradragPeriodeGrunnlagListe = mapSjablonSamværsfradrag(sjablonGrunnlag),
    )

    private fun mapSøknadsbarn(beregnGrunnlag: BeregnGrunnlag): SøknadsbarnPeriodeGrunnlag {
        try {
            val søknadsbarnGrunnlag =
                beregnGrunnlag.grunnlagListe
                    .filtrerOgKonverterBasertPåEgenReferanse<Person>(referanse = beregnGrunnlag.søknadsbarnReferanse)

            return SøknadsbarnPeriodeGrunnlag(
                referanse = søknadsbarnGrunnlag[0].referanse,
                fødselsdato = søknadsbarnGrunnlag[0].innhold.fødselsdato,
                ident = søknadsbarnGrunnlag[0].innhold.ident?.verdi,
            )
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Ugyldig input ved beregning av barnebidrag. Feil i grunnlag som inneholder søknadsbarn: " + e.message,
            )
        }
    }

    private fun mapSamværsklasse(beregnGrunnlag: BeregnGrunnlag): List<SamværsklassePeriodeGrunnlag> {
        try {
            return beregnGrunnlag.grunnlagListe
                .filtrerOgKonverterBasertPåEgenReferanse<SamværsklassePeriode>(grunnlagType = Grunnlagstype.SAMVÆRSPERIODE)
                .map {
                    SamværsklassePeriodeGrunnlag(
                        referanse = it.referanse,
                        samværsklassePeriode = it.innhold,
                    )
                }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Ugyldig input ved beregning av barnebidrag. Innhold i Grunnlagstype.SAMVÆRSPERIODE er ikke gyldig: " + e.message,
            )
        }
    }

    private fun mapSamværsklasseLøpendeBidrag(beregnGrunnlag: BeregnGrunnlag): List<SamværsklassePeriodeGrunnlag> {
        try {
            return beregnGrunnlag.grunnlagListe
                .filtrerOgKonverterBasertPåEgenReferanse<LøpendeBidragPeriode>(grunnlagType = Grunnlagstype.LØPENDE_BIDRAG_PERIODE)
                .filter { it.innhold.samværsklasse != null }
                .map {
                    SamværsklassePeriodeGrunnlag(
                        referanse = it.referanse,
                        samværsklassePeriode = SamværsklassePeriode(it.innhold.periode, it.innhold.samværsklasse!!, it.innhold.manueltRegistrert),
                    )
                }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Ugyldig input ved beregning av barnebidrag. Innhold i Grunnlagstype.LØPENDE_BIDRAG_PERIODE er ikke gyldig: " + e.message,
            )
        }
    }

    private fun mapSamværsklassePrivatAvtale(beregnGrunnlag: BeregnGrunnlag): List<SamværsklassePeriodeGrunnlag> {
        try {
            return beregnGrunnlag.grunnlagListe
                .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtalePeriodeGrunnlag>(grunnlagType = Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG)
                .filter { it.innhold.samværsklasse != null }
                .map {
                    SamværsklassePeriodeGrunnlag(
                        referanse = it.referanse,
                        samværsklassePeriode = SamværsklassePeriode(it.innhold.periode, it.innhold.samværsklasse!!, it.innhold.manueltRegistrert),
                    )
                }
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Ugyldig input ved beregning av barnebidrag. Innhold i Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG er ikke gyldig: " +
                    e.message,
            )
        }
    }
}
