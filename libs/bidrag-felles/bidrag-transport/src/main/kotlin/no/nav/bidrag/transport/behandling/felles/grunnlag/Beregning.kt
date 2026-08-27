package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.beløp.Beløp
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.AldersgruppeForskudd
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.Visningsnavn
import no.nav.bidrag.domene.util.lastVisningsnavnFraFil
import no.nav.bidrag.domene.util.visningsnavn
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.Year
import java.util.Collections.emptyList

data class SluttberegningForskudd(
    override val periode: ÅrMånedsperiode,
    val beløp: BigDecimal,
    val resultatKode: Resultatkode,
    val aldersgruppe: AldersgruppeForskudd,
) : Sluttberegning

data class SluttberegningSærbidrag(
    override val periode: ÅrMånedsperiode,
    val beregnetBeløp: BigDecimal,
    val resultatKode: Resultatkode,
    val resultatBeløp: BigDecimal?,
) : Sluttberegning

private val sluttberegningAvslagResultaterV2 =
    listOf(
        Resultatkode.IKKE_OMSORG,
        Resultatkode.BARNET_ER_SELVFORSØRGET,
    )

fun List<GrunnlagDto>.finnBPsEvne(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
    val gjelderSøknadsbarnReferanse = sluttberegning.gjelderBarnReferanse
    val delberegningBidragsevne =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragsevne>(
            Grunnlagstype.DELBEREGNING_BIDRAGSEVNE,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }
            ?: return null
    return delberegningBidragsevne.innhold.beløp
}

fun List<GrunnlagDto>.erBidragJustertNedTilEvne(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return false
    if (sluttberegning.erSluttberegningGammelStruktur()) {
        val sbInnhold = sluttberegning.innholdTilObjekt<SluttberegningBarnebidrag>()
        return sbInnhold.bidragJustertNedTil25ProsentAvInntekt
    }
    val gjelderSøknadsbarnReferanse = sluttberegning.gjelderBarnReferanse
    val andelAvBidragsevne =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningAndelAvBidragsevne>(
            Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse } ?: return false
    val delberegningBidragsevne =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragsevne>(
            Grunnlagstype.DELBEREGNING_BIDRAGSEVNE,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse } ?: return false
    return !andelAvBidragsevne.innhold.harBPFullEvne &&
        delberegningBidragsevne.innhold.beløp <= delberegningBidragsevne.innhold.sumInntekt25Prosent
}

fun List<GrunnlagDto>.erBidragJustertNedTil25ProsentAvInntekt(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return false
    if (sluttberegning.erSluttberegningGammelStruktur()) {
        val sbInnhold = sluttberegning.innholdTilObjekt<SluttberegningBarnebidrag>()
        return sbInnhold.bidragJustertNedTil25ProsentAvInntekt
    }
    val gjelderSøknadsbarnReferanse = sluttberegning.gjelderBarnReferanse
    val evne25prosentAvInntekt =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningEvne25ProsentAvInntekt>(
            Grunnlagstype.DELBEREGNING_EVNE_25PROSENTAVINNTEKT,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse } ?: return false
    val andelAvBidragsevne =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningAndelAvBidragsevne>(
            Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse } ?: return false
    return !andelAvBidragsevne.innhold.harBPFullEvne && evne25prosentAvInntekt.innhold.erEvneJustertNedTil25ProsentAvInntekt &&
        !erBidragJustertNedTilEvne(grunnlagsreferanseListe)
}

fun List<GrunnlagDto>.resultatSluttberegning(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Resultatkode? {
    val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
    if (sluttberegning.erSluttberegningGammelStruktur()) {
        val sbInnhold = sluttberegning.innholdTilObjekt<SluttberegningBarnebidrag>()
        return Resultatkode.fraKode(sbInnhold.bisysResultatkode)
    }
    val gjelderSøknadsbarnReferanse = sluttberegning.gjelderBarnReferanse
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
    val bpsAndel =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningBidragspliktigesAndel>(
            Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }
    val samværsfradrag =
        finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<DelberegningSamværsfradrag>(
            Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
            sluttberegning.grunnlagsreferanseListe,
        ).firstOrNull { gjelderSøknadsbarnReferanse == null || it.gjelderBarnReferanse == gjelderSøknadsbarnReferanse }
    val nettoBidragEtterBarnetilleggBM =
        bidragTilFordeling?.innhold?.bidragTilFordeling?.subtract(samværsfradrag?.innhold?.beløp ?: BigDecimal.ZERO) ?: BigDecimal.ZERO
    val bidragJustertNedTilEvne = erBidragJustertNedTilEvne(grunnlagsreferanseListe)
    val bpsEvne = finnBPsEvne(grunnlagsreferanseListe)
    val bidragJustertNedTil25ProsentAvInntekt = erBidragJustertNedTil25ProsentAvInntekt(grunnlagsreferanseListe)
    val bidragJustertForDeltBosted = andelDeltBosted != null
    val sluttberegningInnhold = sluttberegning.innholdTilObjekt<SluttberegningBarnebidragV2>()
    return when {
        sluttberegningInnhold.ikkeOmsorgForBarnet -> {
            Resultatkode.IKKE_OMSORG
        }

        bpsBarnetillegg?.innhold?.erBidragJustertTilNettoBarnetilleggBP == true -> {
            Resultatkode.BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BP
        }

        // TODO: Hvordan hente informasjom om forskudssats?
//        bidragJustertManueltTilForskuddssats -> Resultatkode.BIDRAG_JUSTERT_MANUELT_TIL_FORSKUDDSSATS
//        bidragJustertTilForskuddssats -> Resultatkode.BIDRAG_JUSTERT_TIL_FORSKUDDSSATS
        sluttberegningInnhold.barnetErSelvforsørget -> {
            Resultatkode.BARNET_ER_SELVFORSØRGET
        }

        bidragJustertForDeltBosted && bidragJustertNedTilEvne -> {
            Resultatkode.MANGLER_BIDRAGSEVNE
        }

        bidragJustertForDeltBosted && bidragJustertNedTil25ProsentAvInntekt -> {
            Resultatkode.MAKS_25_PROSENT_AV_INNTEKT
        }

        bidragJustertForDeltBosted -> {
            Resultatkode.BIDRAG_JUSTERT_FOR_DELT_BOSTED
        }

        bpsEvne != null && bpsEvne <= BigDecimal.ZERO -> {
            Resultatkode.INGEN_BIDRAGSEVNE
        }

        bidragJustertNedTilEvne -> {
            Resultatkode.MANGLER_BIDRAGSEVNE
        }

        bidragJustertNedTil25ProsentAvInntekt -> {
            Resultatkode.MAKS_25_PROSENT_AV_INNTEKT
        }

        bidragTilFordeling?.innhold?.uMinusNettoBarnetilleggBM == nettoBidragEtterBarnetilleggBM
            -> {
            Resultatkode.BIDRAG_JUSTERT_FOR_NETTO_BARNETILLEGG_BM
        }

        else -> {
            Resultatkode.KOSTNADSBEREGNET_BIDRAG
        }
    }
}

fun List<GrunnlagDto>.tilResultatVisningsnavn(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Visningsnavn? = resultatSluttberegning(grunnlagsreferanseListe)?.visningsnavn

fun List<GrunnlagDto>.tilBisysResultatkode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String = resultatSluttberegning(grunnlagsreferanseListe)?.legacyKode ?: "KBB"

fun List<GrunnlagDto>.erResultatAvslag(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean = sluttberegningAvslagResultaterV2.contains(resultatSluttberegning(grunnlagsreferanseListe))

data class SluttberegningBarnebidragAldersjustering(
    override val periode: ÅrMånedsperiode,
    val beregnetBeløp: BigDecimal,
    val resultatBeløp: BigDecimal,
    val bpAndelBeløp: BigDecimal,
    val bpAndelFaktorVedDeltBosted: BigDecimal? = null,
    val deltBosted: Boolean = false,
) : Sluttberegning {
    @get:JsonIgnore
    val resultat
        get() =
            when {
                deltBosted -> SluttberegningBarnebidrag::bidragJustertForDeltBosted.name
                else -> "kostnadsberegnet"
            }

    @get:JsonIgnore
    val bisysResultatkode
        get() = sluttberegningBisyskodeMap[resultat] ?: "KBB"

    @get:JsonIgnore
    val resultatVisningsnavn get() = lastVisningsnavnFraFil("sluttberegningBarnebidrag.yaml")[resultat]
}

data class SluttberegningIndeksregulering(
    override val periode: ÅrMånedsperiode,
    val beløp: Beløp,
    val originaltBeløp: Beløp,
    val nesteIndeksreguleringsår: Year? = null,
) : Sluttberegning

@Deprecated("", replaceWith = ReplaceWith("DelberegningSumInntekt"))
data class DelberegningInntekt(
    override val periode: ÅrMånedsperiode,
    val summertBeløp: BigDecimal,
) : Delberegning

data class DelberegningSumInntekt(
    override val periode: ÅrMånedsperiode,
    val totalinntekt: BigDecimal,
    val kontantstøtte: BigDecimal? = null,
    val skattepliktigInntekt: BigDecimal? = null,
    val barnetillegg: BigDecimal? = null,
    val utvidetBarnetrygd: BigDecimal? = null,
    val småbarnstillegg: BigDecimal? = null,
) : Delberegning

data class DelberegningBarnIHusstand(
    override val periode: ÅrMånedsperiode,
    val antallBarn: Double,
) : Delberegning

data class DelberegningBidragsevne(
    override val periode: ÅrMånedsperiode,
    val beløp: BigDecimal,
    val skatt: Skatt,
    val underholdBarnEgenHusstand: BigDecimal,
    val sumInntekt25Prosent: BigDecimal = BigDecimal.ZERO,
) : Delberegning {
    data class Skatt(
        val minstefradrag: BigDecimal,
        val skattAlminneligInntekt: BigDecimal,
        val trinnskatt: BigDecimal,
        val trygdeavgift: BigDecimal,
        val sumSkatt: BigDecimal,
        val sumSkattFaktor: BigDecimal = BigDecimal.ZERO,
    )
}

data class DelberegningVoksneIHusstand(
    override val periode: ÅrMånedsperiode,
    val borMedAndreVoksne: Boolean,
) : Delberegning

typealias DelberegningVoksneIHustand = DelberegningVoksneIHusstand

data class DelberegningBoforhold(
    override val periode: ÅrMånedsperiode,
    val antallBarn: Double,
    val borMedAndreVoksne: Boolean,
) : Delberegning

data class DelberegningBidragspliktigesAndel(
    override val periode: ÅrMånedsperiode,
    @JsonAlias("andelFaktor", "andelProsent")
    val endeligAndelFaktor: BigDecimal,
    val andelBeløp: BigDecimal,
    val beregnetAndelFaktor: BigDecimal,
    val barnEndeligInntekt: BigDecimal,
    val barnetErSelvforsørget: Boolean,
) : Delberegning {
    @get:JsonIgnore
    val andelProsent: BigDecimal
        get() =
            if (endeligAndelFaktor < BigDecimal.ONE) {
                endeligAndelFaktor
                    .multiply(BigDecimal(100))
                    .round(MathContext(4))
            } else {
                endeligAndelFaktor.round(MathContext(4))
            }

    @get:JsonIgnore
    val erAndelRedusert: Boolean
        get() = endeligAndelFaktor < beregnetAndelFaktor
}

data class DelberegningUtgift(
    override val periode: ÅrMånedsperiode,
    val sumBetaltAvBp: BigDecimal,
    val sumGodkjent: BigDecimal,
) : Delberegning

data class DelberegningBidragspliktigesBeregnedeTotalbidrag(
    override val periode: ÅrMånedsperiode,
    @JsonAlias("sum")
    val bidragspliktigesBeregnedeTotalbidrag: BigDecimal,
    val beregnetBidragPerBarnListe: List<BeregnetBidragPerBarn> = emptyList(),
) : Delberegning

data class BeregnetBidragPerBarn(
    val gjelderBarn: Grunnlagsreferanse,
    val saksnummer: Saksnummer,
    val løpendeBeløp: BigDecimal,
    val valutakode: String = "NOK",
    val samværsklasse: Samværsklasse,
    val samværsfradrag: BigDecimal,
    val beregnetBeløp: BigDecimal,
    val faktiskBeløp: BigDecimal,
    val reduksjonUnderholdskostnad: BigDecimal,
    val beregnetBidrag: BigDecimal,
    val vedtaksid: Int? = null,
    val bidragJustertForNettoBarnetilleggBP: Boolean? = false,
    val bruttoBidragEtterBarnetilleggBM: BigDecimal? = null,
    val bruttoBidragEtterBarnetilleggBP: BigDecimal? = null,
    val erVedtakKildeBBM: Boolean = true,
)

data class DelberegningSamværsfradrag(
    override val periode: ÅrMånedsperiode,
    val beløp: BigDecimal,
) : Delberegning

data class DelberegningNettoTilsynsutgift(
    override val periode: ÅrMånedsperiode,
    val totalTilsynsutgift: BigDecimal,
    val erBegrensetAvMaksTilsyn: Boolean,
    val bruttoTilsynsutgift: BigDecimal,
    val justertBruttoTilsynsutgift: BigDecimal,
    val andelTilsynsutgiftFaktor: BigDecimal,
    val antallBarnBMUnderTolvÅr: Int,
    val antallBarnBMBeregnet: Int = antallBarnBMUnderTolvÅr,
    val antallBarnMedTilsynsutgifter: Int = antallBarnBMBeregnet,
    val skattefradrag: BigDecimal,
    val skattefradragPerBarn: BigDecimal,
    val skattefradragTotalTilsynsutgift: BigDecimal,
    val skattefradragMaksfradrag: BigDecimal,
    val nettoTilsynsutgift: BigDecimal,
    val tilsynsutgiftBarnListe: List<TilsynsutgiftBarn>,
) : Delberegning

data class TilsynsutgiftBarn(
    val gjelderBarn: Grunnlagsreferanse,
    val sumTilsynsutgifter: BigDecimal,
    val endeligSumTilsynsutgifter: BigDecimal,
)

fun List<GrunnlagInnhold>.filtrerDelberegninger() = filterIsInstance<Delberegning>()

data class DelberegningUnderholdskostnad(
    override val periode: ÅrMånedsperiode,
    val forbruksutgift: BigDecimal,
    val boutgift: BigDecimal,
    val barnetilsynMedStønad: BigDecimal?,
    val nettoTilsynsutgift: BigDecimal?,
    val barnetrygd: BigDecimal,
    val underholdskostnad: BigDecimal,
    val forpleining: BigDecimal? = null,
) : Delberegning

data class FaktiskUtgiftPeriode(
    override val periode: ÅrMånedsperiode,
    val fødselsdatoBarn: LocalDate,
    val faktiskUtgiftBeløp: BigDecimal,
    val kostpengerBeløp: BigDecimal,
    val kommentar: String? = null,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold

data class TilleggsstønadPeriode(
    override val periode: ÅrMånedsperiode,
    @Deprecated("Bruk beløp og beløpstype", replaceWith = ReplaceWith("beløp"))
    val beløpDagsats: BigDecimal? = null,
    val beløp: BigDecimal = beløpDagsats ?: BigDecimal.ZERO,
    val beløpstype: InntektBeløpstype = InntektBeløpstype.DAGSATS,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold

data class DelberegningFaktiskTilsynsutgift(
    override val periode: ÅrMånedsperiode,
    val beregnetBeløp: BigDecimal,
) : Delberegning

data class DelberegningTilleggsstønad(
    override val periode: ÅrMånedsperiode,
    val beregnetBeløp: BigDecimal,
) : Delberegning

data class DelberegningEndringSjekkGrensePeriode(
    override val periode: ÅrMånedsperiode,
    val løpendeBidragBeløp: BigDecimal? = null,
    val løpendeBidragFraPrivatAvtale: Boolean = false,
    val beregnetBidragBeløp: BigDecimal? = null,
    val faktiskEndringFaktor: BigDecimal? = null,
    val endringErOverGrense: Boolean,
) : Delberegning

data class DelberegningEndringSjekkGrense(
    override val periode: ÅrMånedsperiode,
    val endringErOverGrense: Boolean,
) : Delberegning

data class DelberegningBidragspliktigesAndelDeltBosted(
    override val periode: ÅrMånedsperiode,
    val bpAndelAvUVedDeltBostedFaktor: BigDecimal,
    val bpAndelAvUVedDeltBostedBeløp: BigDecimal,
) : Delberegning

data class DelberegningBidragTilFordeling(
    override val periode: ÅrMånedsperiode,
    val bidragTilFordeling: BigDecimal,
    @JsonProperty("uMinusNettoBarnetilleggBM")
    @JsonAlias("uminusNettoBarnetilleggBM")
    val uMinusNettoBarnetilleggBM: BigDecimal,
    val bpAndelAvUMinusSamværsfradrag: BigDecimal,
    val nettoBidragEtterBarnetilleggBM: BigDecimal = BigDecimal.ZERO,
    val bruttoBidragEtterBarnetilleggBM: BigDecimal = BigDecimal.ZERO,
    val erBidragJustertForNettoBarnetilleggBM: Boolean = false,
) : Delberegning

data class DelberegningSumBidragTilFordeling(
    override val periode: ÅrMånedsperiode,
    val sumBidragTilFordeling: BigDecimal,
    val sumPrioriterteBidragTilFordeling: BigDecimal = BigDecimal.ZERO,
    val erKompletteGrunnlagForAlleLøpendeBidrag: Boolean = true,
) : Delberegning

data class DelberegningEvne25ProsentAvInntekt(
    override val periode: ÅrMånedsperiode,
    val evneJustertFor25ProsentAvInntekt: BigDecimal,
    val erEvneJustertNedTil25ProsentAvInntekt: Boolean = false,
) : Delberegning

data class DelberegningAndelAvBidragsevne(
    override val periode: ÅrMånedsperiode,
    val sumBidragTilFordelingJustertForPrioriterteBidrag: BigDecimal = BigDecimal.ZERO,
    val evneJustertForPrioriterteBidrag: BigDecimal = BigDecimal.ZERO,
    val andelAvSumBidragTilFordelingFaktor: BigDecimal,
    val andelAvEvneBeløp: BigDecimal,
    val bidragEtterFordeling: BigDecimal,
    val bruttoBidragJustertForEvneOg25Prosent: BigDecimal = BigDecimal.ZERO,
    val harBPFullEvne: Boolean = true,
) : Delberegning

data class DelberegningBidragJustertForBPBarnetillegg(
    override val periode: ÅrMånedsperiode,
    val bidragJustertForNettoBarnetilleggBP: BigDecimal,
    val erBidragJustertTilNettoBarnetilleggBP: Boolean = false,
) : Delberegning

data class SluttberegningBarnebidragV2(
    override val periode: ÅrMånedsperiode,
    val beregnetBeløp: BigDecimal?,
    val resultatBeløp: BigDecimal?,
    val barnetErSelvforsørget: Boolean = false,
    val ikkeOmsorgForBarnet: Boolean = false,
) : Sluttberegning

data class DelberegningBidragTilFordelingLøpendeBidrag(
    override val periode: ÅrMånedsperiode,
    val valutakode: Valutakode = Valutakode.NOK,
    @Schema(description = "Reduksjon underholdskostnad i valuta")
    val reduksjonUnderholdskostnad: BigDecimal,
    @Schema(description = "Samværsfradrag i valuta")
    val samværsfradrag: BigDecimal? = null,
    @Schema(description = "Indeksregulert beløp i valuta")
    val bidragTilFordeling: BigDecimal,
    @Schema(description = "Bidrag til fordeling i NOK")
    val bidragTilFordelingNOK: BigDecimal = bidragTilFordeling,
    @Schema(description = "Er dette et norsk bidrag?")
    val erNorskBidrag: Boolean = true,
    @Schema(description = "Er dette et oppfostringsbidrag?")
    val erOppfostringsbidrag: Boolean = false,
) : Delberegning

data class FatteVedtakResultat(
    val erRevurderingsbarn: Boolean = false,
    val skalFatteVedtak: Boolean = true,
    val ingenOverlappendePerioderMedSøknadsbarn: Boolean = false,
)

data class DelberegningFatteVedtak(
    override val periode: ÅrMånedsperiode,
    val fatteVedtakResultat: FatteVedtakResultat,
) : Delberegning

// ---------- Deprekerte verdier. Skal ikke slettes helt til vedtak databasen er konvertert i PROD --------------------------

private val sluttberegningAvslagResultater =
    listOf(
        SluttberegningBarnebidrag::ikkeOmsorgForBarnet.name,
        SluttberegningBarnebidrag::barnetErSelvforsørget.name,
    )

// Rekkefølge har ikke noe å si her. Dette er bare konvertering av resultatet til bisyskode.
private val sluttberegningBisyskodeMap =
    mapOf(
        SluttberegningBarnebidrag::ikkeOmsorgForBarnet.name to "AIO",
        SluttberegningBarnebidrag::barnetErSelvforsørget.name to "5SF",
        SluttberegningBarnebidrag::bidragJustertForDeltBosted.name to "8DN",
        SluttberegningBarnebidrag::bidragJustertForNettoBarnetilleggBP.name to "101",
        SluttberegningBarnebidrag::bidragJustertForNettoBarnetilleggBM.name to "102",
        SluttberegningBarnebidrag::bidragJustertNedTilEvne.name to "6MB",
        SluttberegningBarnebidrag::bidragJustertNedTil25ProsentAvInntekt.name to "7M",
        SluttberegningBarnebidrag::bidragJustertTilForskuddssats.name to "RFO",
        SluttberegningBarnebidrag::bidragJustertManueltTilForskuddssats.name to "RFO",
        "kostnadsberegnet" to "KBB",
    )

@Deprecated(
    "Bruk heller SluttberegningBarnebidragV2 og hjelpemetoder for å hente resultatkode og visningsnavn",
    ReplaceWith("SluttberegningBarnebidragV2"),
)
data class SluttberegningBarnebidrag(
    override val periode: ÅrMånedsperiode,
    val beregnetBeløp: BigDecimal?,
    val resultatBeløp: BigDecimal?,
    @JsonAlias("uminusNettoBarnetilleggBM")
    val uMinusNettoBarnetilleggBM: BigDecimal,
    val bruttoBidragEtterBarnetilleggBM: BigDecimal,
    val nettoBidragEtterBarnetilleggBM: BigDecimal,
    val bruttoBidragJustertForEvneOg25Prosent: BigDecimal,
    val bruttoBidragEtterBegrensetRevurdering: BigDecimal = BigDecimal.ZERO,
    val bruttoBidragEtterBarnetilleggBP: BigDecimal,
    val nettoBidragEtterSamværsfradrag: BigDecimal,
    val bpAndelAvUVedDeltBostedFaktor: BigDecimal,
    val bpAndelAvUVedDeltBostedBeløp: BigDecimal,
    val løpendeForskudd: BigDecimal? = null,
    val løpendeBidrag: BigDecimal? = null,
    val barnetErSelvforsørget: Boolean = false,
    val bidragJustertForDeltBosted: Boolean = false,
    val bidragJustertForNettoBarnetilleggBP: Boolean = false,
    val bidragJustertForNettoBarnetilleggBM: Boolean = false,
    val bidragJustertNedTilEvne: Boolean = false,
    val bidragJustertNedTil25ProsentAvInntekt: Boolean = false,
    val bidragJustertTilForskuddssats: Boolean = false,
    val bidragJustertManueltTilForskuddssats: Boolean = false,
    val begrensetRevurderingUtført: Boolean = false,
    val ikkeOmsorgForBarnet: Boolean = false,
    // Brukes bare ved overføring av bisys vedtak
    val tilleggsbidrag: BigDecimal? = null,
    // Brukes bare ved overføring av bisys vedtak
    val forsvaretsBarnetillegg: Boolean? = null,
    // Beregnet evne til BP etter FF
    val bpEvneVedForholdsmessigFordeling: BigDecimal? = null,
    // Andel av U basert på fordeling fra FF
    val bpAndelAvUVedForholdsmessigFordelingFaktor: BigDecimal? = null,
    val bpSumAndelAvU: BigDecimal? = null,
) : Sluttberegning {
    @get:JsonIgnore
    val resultat
        get() =
            // Rekkefølgen bestemmer hvilken som slår ut for sluttresultatet. Øverste har høyest prioritet.
            when {
                ikkeOmsorgForBarnet -> {
                    SluttberegningBarnebidrag::ikkeOmsorgForBarnet.name
                }

                bidragJustertForNettoBarnetilleggBP -> {
                    SluttberegningBarnebidrag::bidragJustertForNettoBarnetilleggBP.name
                }

                bidragJustertManueltTilForskuddssats -> {
                    SluttberegningBarnebidrag::bidragJustertManueltTilForskuddssats.name
                }

                bidragJustertTilForskuddssats -> {
                    SluttberegningBarnebidrag::bidragJustertTilForskuddssats.name
                }

                barnetErSelvforsørget -> {
                    SluttberegningBarnebidrag::barnetErSelvforsørget.name
                }

                bidragJustertForDeltBosted && bidragJustertNedTilEvne -> {
                    SluttberegningBarnebidrag::bidragJustertNedTilEvne.name
                }

                bidragJustertForDeltBosted && bidragJustertNedTil25ProsentAvInntekt -> {
                    SluttberegningBarnebidrag::bidragJustertNedTil25ProsentAvInntekt.name
                }

                bidragJustertForDeltBosted -> {
                    SluttberegningBarnebidrag::bidragJustertForDeltBosted.name
                }

                bidragJustertNedTilEvne -> {
                    SluttberegningBarnebidrag::bidragJustertNedTilEvne.name
                }

                bidragJustertNedTil25ProsentAvInntekt -> {
                    SluttberegningBarnebidrag::bidragJustertNedTil25ProsentAvInntekt.name
                }

                bidragJustertForNettoBarnetilleggBM -> {
                    SluttberegningBarnebidrag::bidragJustertForNettoBarnetilleggBM.name
                }

                else -> {
                    "kostnadsberegnet"
                }
            }

    @get:JsonIgnore
    val erResultatAvslag get() = sluttberegningAvslagResultater.contains(resultat)

    @get:JsonIgnore
    val bisysResultatkode
        get() = sluttberegningBisyskodeMap[resultat] ?: "KBB"

    @get:JsonIgnore
    val resultatVisningsnavn get() = lastVisningsnavnFraFil("sluttberegningBarnebidrag.yaml")[resultat]
}
