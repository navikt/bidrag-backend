package no.nav.bidrag.beregn.barnebidrag.beregning

import no.nav.bidrag.beregn.barnebidrag.bo.AndelAvBidragsevneBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.AndelAvBidragsevneBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.BidragJustertForBPBarnetilleggBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragJustertForBPBarnetilleggBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingLøpendeBidragBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingLøpendeBidragBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingPrivatAvtaleBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingPrivatAvtaleBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.BidragspliktigesAndelDeltBostedBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragspliktigesAndelDeltBostedBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.Evne25ProsentAvInntektBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.Evne25ProsentAvInntektBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.SluttberegningBarnebidragV2BeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SluttberegningBarnebidragV2BeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.SumBidragTilFordelingBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SumBidragTilFordelingBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.ValutakursGrunnlagTilBeregning
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.util.avrundetMedTiDesimaler
import no.nav.bidrag.domene.util.avrundetMedToDesimaler
import no.nav.bidrag.domene.util.avrundetTilNærmesteTier
import java.math.BigDecimal
import java.math.RoundingMode

internal object EndeligBidragBeregningV2 {

    fun beregnBidragspliktigesAndelDeltBosted(
        grunnlag: BidragspliktigesAndelDeltBostedBeregningGrunnlag,
    ): BidragspliktigesAndelDeltBostedBeregningResultat {
        var bpAndelAvUVedDeltBostedFaktor: BigDecimal? = null
        var bpAndelAvUVedDeltBostedBeløp: BigDecimal? = null
        if (grunnlag.deltBostedBeregningGrunnlag?.deltBosted == true) {
            bpAndelAvUVedDeltBostedFaktor =
                (grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.andelFaktor - BigDecimal.valueOf(0.5)).coerceAtLeast(BigDecimal.ZERO)
            bpAndelAvUVedDeltBostedBeløp = grunnlag.underholdskostnadBeregningGrunnlag.beløp.multiply(bpAndelAvUVedDeltBostedFaktor)
        }

        return BidragspliktigesAndelDeltBostedBeregningResultat(
            bpAndelAvUVedDeltBostedFaktor = bpAndelAvUVedDeltBostedFaktor?.avrundetMedTiDesimaler,
            bpAndelAvUVedDeltBostedBeløp = bpAndelAvUVedDeltBostedBeløp?.avrundetMedToDesimaler,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.underholdskostnadBeregningGrunnlag.referanse,
                grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.referanse,
                grunnlag.deltBostedBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun beregnBidragTilFordeling(grunnlag: BidragTilFordelingBeregningGrunnlag): BidragTilFordelingBeregningResultat {
        val erDeltBosted = grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag != null
        val samværsfradrag = if (erDeltBosted) BigDecimal.ZERO else grunnlag.samværsfradragBeregningGrunnlag.beløp
        val underholdskostnad = grunnlag.underholdskostnadBeregningGrunnlag.beløp
        val bpAndelBeløp =
            if (erDeltBosted) {
                grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag.bpAndelAvUVedDeltBostedBeløp
            } else {
                grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.andelBeløp
            }
        val nettoBarnetilleggBM = if (erDeltBosted) BigDecimal.ZERO else grunnlag.barnetilleggBMBeregningGrunnlag?.beløp ?: BigDecimal.ZERO

        val uMinusNettoBarnetilleggBM = underholdskostnad - nettoBarnetilleggBM
        val bpAndelAvUMinusSamværsfradrag = bpAndelBeløp - samværsfradrag
        val bidragTilFordeling = minOf(uMinusNettoBarnetilleggBM, bpAndelAvUMinusSamværsfradrag) + samværsfradrag
        val nettoBidragEtterBarnetilleggBM = maxOf(bidragTilFordeling - samværsfradrag, BigDecimal.ZERO)
        val erBidragJustertForNettoBarnetilleggBM = uMinusNettoBarnetilleggBM == bidragTilFordeling - samværsfradrag

        // Sjekker om perioden vil gi avslag. Dette vil resultere i at det ikke mappes ut noe grunnlagsobjekt for denne perioden.
        val barnetErSelvforsørget = grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.barnetErSelvforsørget
        val søknadsbarnetBorHosBP = grunnlag.søknadsbarnetBorHosBpGrunnlag.søknadsbarnetBorHosBp

        return BidragTilFordelingBeregningResultat(
            uMinusNettoBarnetilleggBM = uMinusNettoBarnetilleggBM.avrundetMedToDesimaler,
            bpAndelAvUMinusSamværsfradrag = bpAndelAvUMinusSamværsfradrag.avrundetMedToDesimaler,
            bidragTilFordeling = bidragTilFordeling.avrundetMedToDesimaler,
            nettoBidragEtterBarnetilleggBM = nettoBidragEtterBarnetilleggBM.avrundetMedToDesimaler,
            bruttoBidragEtterBarnetilleggBM = bidragTilFordeling.avrundetMedToDesimaler,
            erBidragJustertForNettoBarnetilleggBM = erBidragJustertForNettoBarnetilleggBM,
            erAvslag = barnetErSelvforsørget || søknadsbarnetBorHosBP,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.underholdskostnadBeregningGrunnlag.referanse,
                grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.referanse,
                grunnlag.barnetilleggBMBeregningGrunnlag?.referanse,
                grunnlag.samværsfradragBeregningGrunnlag.referanse,
                grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun beregnSumBidragTilFordeling(grunnlag: SumBidragTilFordelingBeregningGrunnlag) = SumBidragTilFordelingBeregningResultat(
        sumBidragTilFordeling = (
            grunnlag.bidragTilFordelingBeregningGrunnlagListe.sumOf { it.bidragTilFordeling } +
                grunnlag.bidragTilFordelingLøpendeBidragBeregningGrunnlagListe.sumOf { it.bidragTilFordeling } +
                grunnlag.bidragTilFordelingPrivatAvtaleBeregningGrunnlagListe.sumOf { it.bidragTilFordeling }
            ).avrundetMedToDesimaler,
        sumPrioriterteBidragTilFordeling = (
            grunnlag.bidragTilFordelingLøpendeBidragBeregningGrunnlagListe.filter { !it.erNorskBidrag || it.erOppfostringsbidrag }
                .sumOf { it.bidragTilFordeling } +
                grunnlag.bidragTilFordelingPrivatAvtaleBeregningGrunnlagListe.filterNot { it.erNorskBidrag }.sumOf { it.bidragTilFordeling }
            ).avrundetMedToDesimaler,
        erKompletteGrunnlagForAlleLøpendeBidrag = grunnlag.bidragTilFordelingLøpendeBidragBeregningGrunnlagListe.isEmpty(),
        grunnlagsreferanseListe =
        grunnlag.bidragTilFordelingBeregningGrunnlagListe.map { it.referanse } +
            grunnlag.bidragTilFordelingLøpendeBidragBeregningGrunnlagListe.map { it.referanse } +
            grunnlag.bidragTilFordelingPrivatAvtaleBeregningGrunnlagListe.map { it.referanse },
    )

    fun beregnEvne25ProsentAvInntekt(grunnlag: Evne25ProsentAvInntektBeregningGrunnlag) = Evne25ProsentAvInntektBeregningResultat(
        evneJustertFor25ProsentAvInntekt = minOf(
            grunnlag.bidragsevneBeregningGrunnlag.beløp,
            grunnlag.bidragsevneBeregningGrunnlag.sumInntekt25Prosent,
        ).avrundetMedToDesimaler,
        erEvneJustertNedTil25ProsentAvInntekt =
        grunnlag.bidragsevneBeregningGrunnlag.sumInntekt25Prosent < grunnlag.bidragsevneBeregningGrunnlag.beløp,
        grunnlagsreferanseListe = listOfNotNull(grunnlag.bidragsevneBeregningGrunnlag.referanse),
    )

    fun beregnAndelAvBidragsevne(grunnlag: AndelAvBidragsevneBeregningGrunnlag): AndelAvBidragsevneBeregningResultat {
        val bidragTilFordeling = grunnlag.bidragTilFordelingBeregningGrunnlag?.bidragTilFordeling ?: BigDecimal.ZERO
        val sumBidragTilFordeling = grunnlag.sumBidragTilFordelingBeregningGrunnlag.sumBidragTilFordeling
        val sumPrioriterteBidragTilFordeling = grunnlag.sumBidragTilFordelingBeregningGrunnlag.sumPrioriterteBidragTilFordeling
        val sumBidragTilFordelingJustertForPrioriterteBidrag = sumBidragTilFordeling - sumPrioriterteBidragTilFordeling
        val andelAvSumBidragTilFordeling =
            if (sumBidragTilFordelingJustertForPrioriterteBidrag.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal.ZERO
            } else {
                bidragTilFordeling.divide(sumBidragTilFordelingJustertForPrioriterteBidrag, 10, RoundingMode.HALF_UP)
            }

        val bidragsevne = grunnlag.evne25ProsentAvInntektBeregningGrunnlag.evneJustertFor25ProsentAvInntekt
        val evneJustertForPrioriterteBidrag = maxOf(bidragsevne - sumPrioriterteBidragTilFordeling, BigDecimal.ZERO)
        val andelAvEvneBeløp = evneJustertForPrioriterteBidrag * andelAvSumBidragTilFordeling
        val bidragEtterFordeling = minOf(bidragTilFordeling, andelAvEvneBeløp)
        val harBPFullEvne = (andelAvEvneBeløp >= bidragTilFordeling) &&
            (!(bidragTilFordeling.compareTo(BigDecimal.ZERO) == 0 && sumBidragTilFordeling > BigDecimal.ZERO))

        val bruttoBidragJustertForEvneOg25Prosent = minOf(bidragTilFordeling, evneJustertForPrioriterteBidrag)

        return AndelAvBidragsevneBeregningResultat(
            sumBidragTilFordelingJustertForPrioriterteBidrag = sumBidragTilFordelingJustertForPrioriterteBidrag.avrundetMedToDesimaler,
            evneJustertForPrioriterteBidrag = evneJustertForPrioriterteBidrag.avrundetMedToDesimaler,
            andelAvSumBidragTilFordelingFaktor = andelAvSumBidragTilFordeling.avrundetMedTiDesimaler,
            andelAvEvneBeløp = andelAvEvneBeløp.avrundetMedToDesimaler,
            bidragEtterFordeling = bidragEtterFordeling.avrundetMedToDesimaler,
            bruttoBidragJustertForEvneOg25Prosent = bruttoBidragJustertForEvneOg25Prosent.avrundetMedToDesimaler,
            harBPFullEvne = harBPFullEvne,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.evne25ProsentAvInntektBeregningGrunnlag.referanse,
                grunnlag.sumBidragTilFordelingBeregningGrunnlag.referanse,
                grunnlag.bidragTilFordelingBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun beregnBidragTilFordelingLøpendeBidrag(
        grunnlag: BidragTilFordelingLøpendeBidragBeregningGrunnlag,
    ): BidragTilFordelingLøpendeBidragBeregningResultat {
        val valutakode = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.valutakode
        val valutakursFraNOK = finnValutakurs(
            valutakursBeregningGrunnlag = grunnlag.valutakursBeregningGrunnlag,
            valutakodeFra = Valutakode.NOK,
            valutakodeTil = valutakode,
        )
        val valutakursTilNOK = finnValutakurs(
            valutakursBeregningGrunnlag = grunnlag.valutakursBeregningGrunnlag,
            valutakodeFra = valutakode,
            valutakodeTil = Valutakode.NOK,
        )

        val beregnetBeløpValuta = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.beregnetBeløp
        val faktiskBeløpValuta = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.faktiskBeløp
        val løpendeBeløpValuta = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.løpendeBeløp
        val erVedtakKildeBBM = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.erVedtakKildeBBM
        val bruttoBidragEtterBarnetilleggBM = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.bruttoBidragEtterBarnetilleggBM ?: BigDecimal.ZERO
        val bruttoBidragEtterBarnetilleggBP =
            grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.bruttoBidragEtterBarnetilleggBP ?: BigDecimal.ZERO

        val samværsfradragNOK = grunnlag.samværsfradragBeregningGrunnlag?.beløp
        val samværsfradragValuta = samværsfradragNOK?.times(valutakursFraNOK)

        val reduksjonUnderholdskostnad = if (erVedtakKildeBBM) {
            (beregnetBeløpValuta - faktiskBeløpValuta).coerceAtLeast(BigDecimal.ZERO)
        } else {
            (bruttoBidragEtterBarnetilleggBM - bruttoBidragEtterBarnetilleggBP).coerceAtLeast(BigDecimal.ZERO)
        }

        var bidragTilFordelingValuta = løpendeBeløpValuta + (samværsfradragValuta ?: BigDecimal.ZERO)
        if (BarnebidragUnleashFeatures.BIDRAG_REDUKSJON_UNDERHOLDSKOSTNAD.isEnabled) {
            bidragTilFordelingValuta = bidragTilFordelingValuta.add(reduksjonUnderholdskostnad)
        }
        val bidragTilFordelingNOK = bidragTilFordelingValuta * valutakursTilNOK

        val erNorskBidrag =
            valutakode == Valutakode.NOK && grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.sakskategori == Sakskategori.NASJONAL
        val erOppfostringsbidrag = grunnlag.løpendeBidragBeregningGrunnlag.løpendeBidrag.stønadstype == Stønadstype.OPPFOSTRINGSBIDRAG

        return BidragTilFordelingLøpendeBidragBeregningResultat(
            valutakode = valutakode,
            reduksjonUnderholdskostnad = reduksjonUnderholdskostnad.avrundetMedToDesimaler,
            samværsfradrag = samværsfradragValuta?.avrundetMedToDesimaler,
            bidragTilFordeling = bidragTilFordelingValuta.avrundetMedToDesimaler,
            bidragTilFordelingNOK = bidragTilFordelingNOK.avrundetMedToDesimaler,
            erNorskBidrag = erNorskBidrag,
            erOppfostringsbidrag = erOppfostringsbidrag,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.løpendeBidragBeregningGrunnlag.referanse,
                grunnlag.samværsfradragBeregningGrunnlag?.referanse,
                grunnlag.valutakursBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun beregnBidragTilFordelingPrivatAvtale(
        grunnlag: BidragTilFordelingPrivatAvtaleBeregningGrunnlag,
    ): BidragTilFordelingPrivatAvtaleBeregningResultat {
        val valutakode = grunnlag.indeksreguleringPrivatAvtaleBeregningGrunnlag.valutakode
        val valutakursFraNOK = finnValutakurs(
            valutakursBeregningGrunnlag = grunnlag.valutakursBeregningGrunnlag,
            valutakodeFra = Valutakode.NOK,
            valutakodeTil = valutakode,
        )
        val valutakursTilNOK = finnValutakurs(
            valutakursBeregningGrunnlag = grunnlag.valutakursBeregningGrunnlag,
            valutakodeFra = valutakode,
            valutakodeTil = Valutakode.NOK,
        )

        val indeksregulertBeløpValuta = grunnlag.indeksreguleringPrivatAvtaleBeregningGrunnlag.indeksregulertBeløp

        val samværsfradragNOK = grunnlag.samværsfradragBeregningGrunnlag?.beløp
        val samværsfradragValuta = samværsfradragNOK?.times(valutakursFraNOK)

        val bidragTilFordelingValuta = indeksregulertBeløpValuta + (samværsfradragValuta ?: BigDecimal.ZERO)
        val bidragTilFordelingNOK = bidragTilFordelingValuta * valutakursTilNOK

        val erNorskBidrag = valutakode == Valutakode.NOK && grunnlag.privatAvtaleBeregningGrunnlag.sakskategori == Sakskategori.NASJONAL

        return BidragTilFordelingPrivatAvtaleBeregningResultat(
            valutakode = valutakode,
            indeksregulertBeløp = indeksregulertBeløpValuta.avrundetMedToDesimaler,
            samværsfradrag = samværsfradragValuta?.avrundetMedToDesimaler,
            bidragTilFordeling = bidragTilFordelingValuta.avrundetMedToDesimaler,
            bidragTilFordelingNOK = bidragTilFordelingNOK.avrundetMedToDesimaler,
            erNorskBidrag = erNorskBidrag,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.privatAvtaleBeregningGrunnlag.referanse,
                grunnlag.indeksreguleringPrivatAvtaleBeregningGrunnlag.referanse,
                grunnlag.samværsfradragBeregningGrunnlag?.referanse,
                grunnlag.valutakursBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun beregnBidragJustertForBPBarnetillegg(
        grunnlag: BidragJustertForBPBarnetilleggBeregningGrunnlag,
    ): BidragJustertForBPBarnetilleggBeregningResultat {
        val erDeltBosted = grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag != null
        val nettoBarnetilleggBP = grunnlag.barnetilleggBPBeregningGrunnlag?.beløp ?: BigDecimal.ZERO
        val bidragEtterFordelingAvBidragsevne = grunnlag.andelAvBidragsevneBeregningGrunnlag.bidragEtterFordeling
        val bidragJustertForNettoBarnetilleggBP: BigDecimal

        if (erDeltBosted) {
            bidragJustertForNettoBarnetilleggBP = bidragEtterFordelingAvBidragsevne
        } else if (nettoBarnetilleggBP > bidragEtterFordelingAvBidragsevne) {
            bidragJustertForNettoBarnetilleggBP = nettoBarnetilleggBP
        } else {
            bidragJustertForNettoBarnetilleggBP = bidragEtterFordelingAvBidragsevne
        }

        val erBidragJustertTilNettoBarnetilleggBP = bidragJustertForNettoBarnetilleggBP == nettoBarnetilleggBP

        return BidragJustertForBPBarnetilleggBeregningResultat(
            bidragJustertForNettoBarnetilleggBP = bidragJustertForNettoBarnetilleggBP.avrundetMedToDesimaler,
            erBidragJustertTilNettoBarnetilleggBP = erBidragJustertTilNettoBarnetilleggBP,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.andelAvBidragsevneBeregningGrunnlag.referanse,
                grunnlag.barnetilleggBPBeregningGrunnlag?.referanse,
                grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun beregnSluttberegningBarnebidrag(grunnlag: SluttberegningBarnebidragV2BeregningGrunnlag): SluttberegningBarnebidragV2BeregningResultat {
        // Hvis søknadsbarnet bor hos BP skal det resultere i avslag og bidragsbeløp settes til null
        if (grunnlag.søknadsbarnetBorHosBpGrunnlag.søknadsbarnetBorHosBp) {
            return SluttberegningBarnebidragV2BeregningResultat(
                ikkeOmsorgForBarnet = true,
                beregnetBeløp = null,
                resultatBeløp = null,
                grunnlagsreferanseListe = listOf(grunnlag.søknadsbarnetBorHosBpGrunnlag.referanse),
            )
        }

        // Hvis søknadsbarnet er selvforsørget skal det resultere i avslag og bidragsbeløp settes til null
        if (grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.barnetErSelvforsørget) {
            return SluttberegningBarnebidragV2BeregningResultat(
                barnetErSelvforsørget = true,
                beregnetBeløp = null,
                resultatBeløp = null,
                grunnlagsreferanseListe = listOf(grunnlag.bpAndelUnderholdskostnadBeregningGrunnlag.referanse),
            )
        }

        val erDeltBosted = grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag != null
        val bidragJustertForNettoBarnetilleggBP =
            grunnlag.bidragJustertForBPBarnetilleggBeregningGrunnlag.bidragJustertForNettoBarnetilleggBP
        val samværsfradrag = if (erDeltBosted) BigDecimal.ZERO else grunnlag.samværsfradragBeregningGrunnlag.beløp
        val beregnetBeløp = maxOf((bidragJustertForNettoBarnetilleggBP - samværsfradrag), BigDecimal.ZERO)

        return SluttberegningBarnebidragV2BeregningResultat(
            beregnetBeløp = beregnetBeløp.avrundetMedToDesimaler,
            resultatBeløp = beregnetBeløp.avrundetTilNærmesteTier,
            grunnlagsreferanseListe = listOfNotNull(
                grunnlag.bidragJustertForBPBarnetilleggBeregningGrunnlag.referanse,
                grunnlag.samværsfradragBeregningGrunnlag.referanse,
                grunnlag.bidragspliktigesAndelDeltBostedBeregningGrunnlag?.referanse,
            ),
        )
    }

    fun finnValutakurs(
        valutakursBeregningGrunnlag: ValutakursGrunnlagTilBeregning?,
        valutakodeFra: Valutakode,
        valutakodeTil: Valutakode,
    ): BigDecimal {
        if (valutakodeFra == valutakodeTil) {
            return BigDecimal.ONE
        }
        if (valutakursBeregningGrunnlag == null) {
            throw IllegalArgumentException("Ikke mulig å finne valutakurs for omregning mellom $valutakodeFra og $valutakodeTil")
        }
        return valutakursBeregningGrunnlag.valutakursListe
            .firstOrNull { it.valutakode1 == valutakodeFra && it.valutakode2 == valutakodeTil }
            ?.valutakurs ?: throw IllegalArgumentException("Ikke mulig å finne valutakurs for omregning mellom $valutakodeFra og $valutakodeTil")
    }
}
