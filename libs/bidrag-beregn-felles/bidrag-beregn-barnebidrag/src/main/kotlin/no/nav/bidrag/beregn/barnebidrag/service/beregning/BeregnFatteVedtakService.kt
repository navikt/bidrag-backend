package no.nav.bidrag.beregn.barnebidrag.service.beregning

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.core.service.BeregnService
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningAndelAvBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningFatteVedtak
import no.nav.bidrag.transport.behandling.felles.grunnlag.FatteVedtakResultat
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.opprettDelberegningreferanse
import java.time.YearMonth

internal object BeregnFatteVedtakService : BeregnService() {

    // Sjekker om det skal fattes vedtak. Hvis søknadsbarnet er en del av opprinnelig behandling (ikke revurdering) skal det fattes vedtak.
    // Hvis søknadsbarnet ikke er del av opprinnelig behandling (= revurderingsbarn) sjekkes det om det blir FF. Hvis noen av søknadsbarna ikke har
    // full evne i perioden for beregningsperiode-til (basert på opprinnelig beregning med løpende bidrag for revurderingsbarna) og det samtidig ikke
    // er avslag for det aktuelle søknadsbarnet anbefales det å fatte vedtak for revurderingssøknaden.
    //
    // Det gjøres i tillegg en sjekk på om det finnes overlappende perioder mellom søknadsbarn og revurderingsbarn. Hvis det ikke finnes overlappende
    // perioder skal det ikke fattes vedtak for revurderingssøknaden.
    //
    // Denne sjekken blir den samme for alle revurderingssøknadene. Hvis alle søknadsbarna har full evne i denne perioden skal
    // revurderingssøknaden(e) trekkes. Den skal likevel beregnes "som normalt" fordi saksbehandler har mulighet til å overstyre.
    fun delberegningFatteVedtak(
        beregnGrunnlagGjeldendeBarn: BeregnGrunnlagJustert,
        beregnGrunnlagAlleBarnListe: List<BeregnGrunnlagJustert>,
        grunnlagOpprinneligBeregningListe: List<GrunnlagDto> = emptyList(),
        sjekkEvneMotPeriode: YearMonth = YearMonth.now(),
    ): List<GrunnlagDto> {
        var fatteVedtakResultat: FatteVedtakResultat

        val barnErDelAvOpprinneligBehandling = beregnGrunnlagGjeldendeBarn.erDelAvOpprinneligBehandling

        if (barnErDelAvOpprinneligBehandling) {
            // Hvis søknadsbarnet er del av opprinnelig behandling skal det alltid fattes vedtak.
            fatteVedtakResultat = FatteVedtakResultat(erRevurderingsbarn = false, skalFatteVedtak = true)
        } else {
            val grunnlagAlleSøknadsbarnListe = beregnGrunnlagAlleBarnListe
                .filter { it.erDelAvOpprinneligBehandling }
                .flatMap { it.beregnGrunnlag.grunnlagListe }

            val grunnlagAlleRevurderingsbarnListe = beregnGrunnlagAlleBarnListe
                .filter { !it.erDelAvOpprinneligBehandling }
                .flatMap { it.beregnGrunnlag.grunnlagListe }

            // Her sjekkes det mot resultatet av den opprinnelige beregningen (basert på løpende bidrag for revurderingsbarna)
            val søknadsbarnMedManglendeEvneISistePeriodeListe = grunnlagOpprinneligBeregningListe
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningAndelAvBidragsevne>(
                    Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE,
                )
                .filterNot { it.innhold.harBPFullEvne }
                .filter { it.innhold.periode.inneholder(sjekkEvneMotPeriode) }
                .map { it.gjelderBarnReferanse }

            val søknadsbarnSomIkkeHarAvslagISistePeriodeListe = grunnlagAlleSøknadsbarnListe
                .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(
                    Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
                )
                .filterNot { it.innhold.barnetErSelvforsørget || it.innhold.ikkeOmsorgForBarnet || it.innhold.resultatBeløp == null }
                .filter { it.innhold.periode.inneholder(sjekkEvneMotPeriode) }
                .map { it.gjelderBarnReferanse }

            // Perioder for søknadsbarna hvor det ikke er avslag
            val perioderHvorDetIkkeErAvslagForSøknadsbarnListe = grunnlagAlleSøknadsbarnListe
                .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(
                    Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
                )
                .filterNot { it.innhold.barnetErSelvforsørget || it.innhold.ikkeOmsorgForBarnet || it.innhold.resultatBeløp == null }
                .filter {
                    beregnGrunnlagGjeldendeBarn.virkningFraPeriode != null &&
                        it.innhold.periode.inneholder(beregnGrunnlagGjeldendeBarn.virkningFraPeriode!!)
                }
                .map { it.innhold.periode }

            // Perioder for revurderingsbarna hvor det ikke er avslag
            val perioderHvorDetIkkeErAvslagForRevurderingsbarnListe = grunnlagAlleRevurderingsbarnListe
                .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(
                    Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
                )
                .filterNot { it.innhold.barnetErSelvforsørget || it.innhold.ikkeOmsorgForBarnet || it.innhold.resultatBeløp == null }
                .filter {
                    beregnGrunnlagGjeldendeBarn.virkningFraPeriode != null &&
                        it.innhold.periode.inneholder(beregnGrunnlagGjeldendeBarn.virkningFraPeriode!!)
                }
                .map { it.innhold.periode }

            // Hvis revurderingsbarna ikke har noen perioder som overlapper med søknadsbarnas perioder er det ikke noe å fordele og
            // revurderingssøknaden skal avvises på lik linje med at alle søknadsbarna har avslag i siste periode.
            val ingenOverlappendePerioderMedSøknadsbarn = perioderHvorDetIkkeErAvslagForRevurderingsbarnListe.none { revurderingsbarnPeriode ->
                perioderHvorDetIkkeErAvslagForSøknadsbarnListe.any { søknadsbarnPeriode ->
                    søknadsbarnPeriode overlapper revurderingsbarnPeriode
                }
            }

            fatteVedtakResultat = FatteVedtakResultat(
                erRevurderingsbarn = true,
                skalFatteVedtak = søknadsbarnMedManglendeEvneISistePeriodeListe.any { it in søknadsbarnSomIkkeHarAvslagISistePeriodeListe },
                ingenOverlappendePerioderMedSøknadsbarn = ingenOverlappendePerioderMedSøknadsbarn,
            )
        }

        // Finner grunnlag som refereres direkte av delberegning fatte vedtak (bare relevant for revurderingsbarn)
        val grunnlagSomRefereres =
            grunnlagOpprinneligBeregningListe
                .filter { it.type == Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE } +
                beregnGrunnlagAlleBarnListe
                    .filter { !it.erDelAvOpprinneligBehandling }
                    .flatMap { it.beregnGrunnlag.grunnlagListe }
                    .filter { it.type == Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG }

        // Mapper ut grunnlag for delberegning fatte vedtak
        val delberegningFatteVedtak = mapDelberegningFatteVedtak(
            fatteVedtakResultat = fatteVedtakResultat,
            grunnlagSomRefereres = if (barnErDelAvOpprinneligBehandling) emptyList() else grunnlagSomRefereres,
            periode = beregnGrunnlagGjeldendeBarn.beregnGrunnlag.periode,
            søknadsbarnReferanse = beregnGrunnlagGjeldendeBarn.beregnGrunnlag.søknadsbarnReferanse,
            gjelderReferanse = beregnGrunnlagGjeldendeBarn.beregnGrunnlag.grunnlagListe.bidragsmottaker?.referanse ?: "",
        )

        return if (barnErDelAvOpprinneligBehandling) {
            listOf(
                delberegningFatteVedtak,
            )
        } else {
            (grunnlagOpprinneligBeregningListe + delberegningFatteVedtak)
        }
    }

    // Mapper ut DelberegningFatteVedtak
    private fun mapDelberegningFatteVedtak(
        fatteVedtakResultat: FatteVedtakResultat,
        grunnlagSomRefereres: List<GrunnlagDto>,
        periode: ÅrMånedsperiode,
        søknadsbarnReferanse: String,
        gjelderReferanse: String,
    ): GrunnlagDto = GrunnlagDto(
        referanse = opprettDelberegningreferanse(
            type = Grunnlagstype.DELBEREGNING_FATTE_VEDTAK,
            periode = periode,
            søknadsbarnReferanse = søknadsbarnReferanse,
        ),
        type = Grunnlagstype.DELBEREGNING_FATTE_VEDTAK,
        innhold = POJONode(
            DelberegningFatteVedtak(
                periode = ÅrMånedsperiode(
                    fom = periode.fom,
                    til = null,
                ),
                fatteVedtakResultat = fatteVedtakResultat,
            ),
        ),
        grunnlagsreferanseListe = grunnlagSomRefereres.map { it.referanse }.distinct(),
        gjelderReferanse = gjelderReferanse,
        gjelderBarnReferanse = søknadsbarnReferanse,
    )
}
