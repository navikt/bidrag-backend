package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.dokumentmaler.DokumentmalSluttberegningBarnebidragDetaljer
import java.math.BigDecimal

fun List<GrunnlagDto>.finnDelberegningBidragspliktigesAndelSærbidrag(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
): DelberegningBidragspliktigesAndel? {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
    val delberegningBidragspliktigesAndel =
        find {
            it.type == Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL &&
                sluttberegning.grunnlagsreferanseListe.contains(
                    it.referanse,
                )
        } ?: return null
    return delberegningBidragspliktigesAndel.innholdTilObjekt<DelberegningBidragspliktigesAndel>()
}

fun List<GrunnlagDto>.finnSluttberegningIReferanser(grunnlagsreferanseListe: List<Grunnlagsreferanse>) =
    find {
        listOf(
            Grunnlagstype.SLUTTBEREGNING_FORSKUDD,
            Grunnlagstype.SLUTTBEREGNING_SÆRBIDRAG,
            Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
            Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG_ALDERSJUSTERING,
            Grunnlagstype.SLUTTBEREGNING_INDEKSREGULERING,
        ).contains(it.type) &&
            grunnlagsreferanseListe.contains(it.referanse)
    }

fun List<GrunnlagDto>.finnTotalInntektForRolleEllerIdent(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    rolletype: Rolletype? = null,
    personIdent: String? = null,
): BigDecimal {
    val sluttberegning =
        finnSluttberegningIReferanser(grunnlagsreferanseListe)
            ?: return BigDecimal.ZERO
    val gjelderReferanse =
        hentAllePersoner()
            .find {
                (rolletype == null || it.type == rolletype.tilGrunnlagstype()) &&
                    (personIdent == null || it.personIdent == personIdent)
            }?.referanse
    val delberegningSumInntekter = finnGrunnlagSomErReferertAv(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, sluttberegning)
    val delberegningSumInntektForRolle =
        if (gjelderReferanse.isNullOrEmpty()) {
            delberegningSumInntekter.firstOrNull()
        } else {
            delberegningSumInntekter.find {
                it.gjelderReferanse ==
                    gjelderReferanse
            }
        }
    return delberegningSumInntektForRolle?.innholdTilObjekt<DelberegningSumInntekt>()?.totalinntekt
        ?: BigDecimal.ZERO
}

fun GrunnlagDto.sluttberegningPeriode(): ÅrMånedsperiode =
    if (erSluttberegningGammelStruktur()) {
        val sluttberegningObjekt = innholdTilObjekt<SluttberegningBarnebidrag>()
        sluttberegningObjekt.periode
    } else {
        val sluttberegningObjekt = innholdTilObjekt<SluttberegningBarnebidragV2>()
        sluttberegningObjekt.periode
    }

fun GrunnlagDto.hentBeregnetBeløp(): BigDecimal? =
    if (erSluttberegningNyStruktur()) {
        innholdTilObjekt<SluttberegningBarnebidragV2>().beregnetBeløp
    } else {
        innholdTilObjekt<SluttberegningBarnebidrag>().beregnetBeløp
    }

fun GrunnlagDto.hentResultatBeløp(): BigDecimal? =
    if (erSluttberegningNyStruktur()) {
        innholdTilObjekt<SluttberegningBarnebidragV2>().resultatBeløp
    } else {
        innholdTilObjekt<SluttberegningBarnebidrag>().resultatBeløp
    }

fun GrunnlagDto.erSluttberegningNyStruktur(): Boolean =
    type == Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG && !erSluttberegningGammelStruktur()

fun GrunnlagDto.erSluttberegningGammelStruktur(): Boolean =
    type == Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG &&
        if (innhold is POJONode) {
            innhold.pojo is SluttberegningBarnebidrag
        } else {
            (
                innhold.has("bruttoBidragEtterBarnetilleggBM") ||
                    innhold.has("bruttoBidragJustertForEvneOg25Prosent")
            )
        }

fun List<GrunnlagDto>.finnSamværsklasse(sluttberegningGrunnlag: GrunnlagDto): Samværsklasse =
    finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<SamværsperiodeGrunnlag>(
        Grunnlagstype.SAMVÆRSPERIODE,
        sluttberegningGrunnlag.grunnlagsreferanseListe,
    ).first().innhold.samværsklasse

fun List<GrunnlagDto>.finnBidragTilFordeling(sluttberegningGrunnlag: GrunnlagDto): BigDecimal {
    if (sluttberegningGrunnlag.erSluttberegningGammelStruktur()) {
        val sluttberegningObjekt = sluttberegningGrunnlag.innholdTilObjekt<SluttberegningBarnebidrag>()
        return sluttberegningObjekt.bruttoBidragEtterBarnetilleggBM
    }
    return finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragTilFordeling>(
        Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING,
        sluttberegningGrunnlag.grunnlagsreferanseListe,
    ).first().innhold.bidragTilFordeling
}

fun List<GrunnlagDto>.finnBidragJustertForBarnetilleggBP(sluttberegningGrunnlag: GrunnlagDto): BigDecimal {
    if (sluttberegningGrunnlag.erSluttberegningGammelStruktur()) {
        val sluttberegningObjekt = sluttberegningGrunnlag.innholdTilObjekt<SluttberegningBarnebidrag>()
        return sluttberegningObjekt.bruttoBidragEtterBarnetilleggBP
    }
    return finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragJustertForBPBarnetillegg>(
        Grunnlagstype.DELBEREGNING_BIDRAG_JUSTERT_FOR_BP_BARNETILLEGG,
        sluttberegningGrunnlag.grunnlagsreferanseListe,
    ).firstOrNull()?.innhold?.bidragJustertForNettoBarnetilleggBP ?: BigDecimal.ZERO
}

fun List<GrunnlagDto>.byggSluttberegningBarnebidragDetaljer(
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
): DokumentmalSluttberegningBarnebidragDetaljer? {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
    if (sluttberegning.type != Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG) return null

    if (sluttberegning.erSluttberegningGammelStruktur()) {
        val sbInnhold = sluttberegning.innholdTilObjekt<SluttberegningBarnebidrag>()
        return DokumentmalSluttberegningBarnebidragDetaljer(
            bidragJustertNedTilEvne = sbInnhold.bidragJustertNedTilEvne,
            nettoBidragEtterSamværsfradrag = sbInnhold.nettoBidragEtterSamværsfradrag,
            uMinusNettoBarnetilleggBM = sbInnhold.uMinusNettoBarnetilleggBM,
            bruttoBidragEtterBegrensetRevurdering = sbInnhold.bruttoBidragEtterBegrensetRevurdering,
            løpendeForskudd = sbInnhold.løpendeForskudd,
            løpendeBidrag = sbInnhold.løpendeBidrag,
            bpSumAndelAvU = sbInnhold.bpSumAndelAvU,
            bpAndelAvUVedForholdsmessigFordelingFaktor = sbInnhold.bpAndelAvUVedForholdsmessigFordelingFaktor,
            bpEvneVedForholdsmessigFordeling = sbInnhold.bpEvneVedForholdsmessigFordeling,
            bidragJustertManueltTilForskuddssats = sbInnhold.bidragJustertManueltTilForskuddssats,
            bpAndelAvUVedDeltBostedBeløp = sbInnhold.bpAndelAvUVedDeltBostedBeløp,
            bpAndelAvUVedDeltBostedFaktor = sbInnhold.bpAndelAvUVedDeltBostedFaktor,
            bidragJustertForNettoBarnetilleggBP = sbInnhold.bidragJustertForNettoBarnetilleggBP,
            bruttoBidragEtterBarnetilleggBP = sbInnhold.bruttoBidragEtterBarnetilleggBP,
            bidragJustertNedTil25ProsentAvInntekt = sbInnhold.bidragJustertNedTil25ProsentAvInntekt,
            bruttoBidragJustertForEvneOg25Prosent = sbInnhold.bruttoBidragJustertForEvneOg25Prosent,
            begrensetRevurderingUtført = sbInnhold.begrensetRevurderingUtført,
            bidragJustertForDeltBosted = sbInnhold.bidragJustertForDeltBosted,
            ikkeOmsorgForBarnet = sbInnhold.ikkeOmsorgForBarnet,
            bidragJustertTilForskuddssats = sbInnhold.bidragJustertTilForskuddssats,
            barnetErSelvforsørget = sbInnhold.barnetErSelvforsørget,
            beregnetBeløp = sbInnhold.beregnetBeløp,
            resultatBeløp = sbInnhold.resultatBeløp,
            nettoBidragEtterBarnetilleggBM = sbInnhold.nettoBidragEtterBarnetilleggBM,
            bruttoBidragEtterBarnetilleggBM = sbInnhold.bruttoBidragEtterBarnetilleggBM,
            bidragJustertForNettoBarnetilleggBM = sbInnhold.bidragJustertForNettoBarnetilleggBM,
            resultat = resultatSluttberegning(grunnlagsreferanseListe),
            resultatVisningsnavn = sbInnhold.resultatVisningsnavn,
        )
    }
    val gjelderSøknadsbarnReferanse = sluttberegning.gjelderBarnReferanse
    val beregnetBeløp = sluttberegning.hentBeregnetBeløp()
    val resultatBeløp = sluttberegning.hentResultatBeløp()
    val andelDeltBosted =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragspliktigesAndelDeltBosted>(
            Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL_DELT_BOSTED,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }

    val bidragTilFordeling =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragTilFordeling>(
            Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }
    val andelAvBidragsevne =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningAndelAvBidragsevne>(
            Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }
    val bpsBarnetillegg =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragJustertForBPBarnetillegg>(
            Grunnlagstype.DELBEREGNING_BIDRAG_JUSTERT_FOR_BP_BARNETILLEGG,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }
    val samværsfradrag =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningSamværsfradrag>(
            Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }

    val nettoBidragEtterBarnetilleggBM =
        bidragTilFordeling?.innhold?.bidragTilFordeling?.subtract(samværsfradrag?.innhold?.beløp ?: BigDecimal.ZERO) ?: BigDecimal.ZERO

    val bidragJustertNedTil25ProsentAvInntekt = erBidragJustertNedTil25ProsentAvInntekt(grunnlagsreferanseListe)
    val sluttberegningInnhold = sluttberegning.innholdTilObjekt<SluttberegningBarnebidragV2>()
    val bidragJustertForDeltBosted = andelDeltBosted != null
    return DokumentmalSluttberegningBarnebidragDetaljer(
        bidragJustertNedTilEvne = erBidragJustertNedTilEvne(grunnlagsreferanseListe),
        nettoBidragEtterSamværsfradrag = beregnetBeløp ?: BigDecimal.ZERO,
        uMinusNettoBarnetilleggBM = bidragTilFordeling?.innhold?.uMinusNettoBarnetilleggBM ?: BigDecimal.ZERO,
        bpAndelAvUVedDeltBostedBeløp = andelDeltBosted?.innhold?.bpAndelAvUVedDeltBostedBeløp ?: BigDecimal.ZERO,
        bpAndelAvUVedDeltBostedFaktor = andelDeltBosted?.innhold?.bpAndelAvUVedDeltBostedFaktor ?: BigDecimal.ZERO,
        bidragJustertForDeltBosted = bidragJustertForDeltBosted,
        bidragJustertForNettoBarnetilleggBP = bpsBarnetillegg?.innhold?.erBidragJustertTilNettoBarnetilleggBP ?: false,
        bruttoBidragEtterBarnetilleggBP = bpsBarnetillegg?.innhold?.bidragJustertForNettoBarnetilleggBP ?: BigDecimal.ZERO,
        bidragJustertNedTil25ProsentAvInntekt = bidragJustertNedTil25ProsentAvInntekt,
        bruttoBidragJustertForEvneOg25Prosent = andelAvBidragsevne?.innhold?.bruttoBidragJustertForEvneOg25Prosent ?: BigDecimal.ZERO,
        barnetErSelvforsørget = sluttberegningInnhold.barnetErSelvforsørget,
        ikkeOmsorgForBarnet = sluttberegningInnhold.ikkeOmsorgForBarnet,
        beregnetBeløp = beregnetBeløp,
        resultatBeløp = resultatBeløp,
        nettoBidragEtterBarnetilleggBM = nettoBidragEtterBarnetilleggBM,
        bruttoBidragEtterBarnetilleggBM = bidragTilFordeling?.innhold?.bidragTilFordeling ?: BigDecimal.ZERO,
        bidragJustertForNettoBarnetilleggBM = bidragTilFordeling?.innhold?.uMinusNettoBarnetilleggBM == nettoBidragEtterBarnetilleggBM,
        resultat = resultatSluttberegning(grunnlagsreferanseListe),
        resultatVisningsnavn = tilResultatVisningsnavn(grunnlagsreferanseListe),
    )
}
