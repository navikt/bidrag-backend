package no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.periode

import no.nav.bidrag.beregn.core.bo.Periode
import no.nav.bidrag.beregn.core.periode.Periodiserer
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.beregning.BPsBeregnedeTotalbidragBeregning
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.bo.BeregnBPsBeregnedeTotalbidragResultat
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.bo.ResultatPeriode
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.dto.LøpendeBidragCore
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.dto.LøpendeBidragGrunnlagCore
import no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.dto.ValutakursPeriodeCore
import no.nav.bidrag.beregn.særbidrag.core.felles.FellesPeriode
import no.nav.bidrag.domene.enums.samhandler.Valutakode

class BPsBeregnedeTotalbidragPeriode(
    private val bPsBeregnedeTotalbidragBeregning: BPsBeregnedeTotalbidragBeregning = BPsBeregnedeTotalbidragBeregning(),
) : FellesPeriode() {

    fun beregnPerioder(grunnlag: LøpendeBidragGrunnlagCore): BeregnBPsBeregnedeTotalbidragResultat {
        val valutaer = grunnlag.løpendeBidragCoreListe.map { it.valutakode }.filter { it != Valutakode.NOK }.toSet()
        val valutakurser = grunnlag.valutakursPeriodeListe.filter {
            it.valutakursListe.any { par -> par.valutakode1 in valutaer && par.valutakode2 == Valutakode.NOK }
        }
        val periodiserer = Periodiserer()
            .addBruddpunkt(grunnlag.beregnDatoFra)
            .addBruddpunkt(grunnlag.beregnDatoTil)
        valutakurser.forEach {
            val datoTil = it.periode.datoTil
            require(datoTil == null || datoTil.isAfter(it.periode.datoFom)) {
                "Valutakursgrunnlaget må ha en gyldig periode"
            }
            periodiserer.addBruddpunkt(it.periode.datoFom)
            datoTil?.let { til -> periodiserer.addBruddpunkt(til) }
        }
        val resultater = periodiserer.finnPerioder(grunnlag.beregnDatoFra, grunnlag.beregnDatoTil).map { periode ->
            val periodegrunnlag = grunnlag.copy(
                beregnDatoFra = periode.datoFom,
                beregnDatoTil = requireNotNull(periode.datoTil),
                løpendeBidragCoreListe = grunnlag.løpendeBidragCoreListe.map { bidrag ->
                    finnKursForPeriode(bidrag, periode, valutakurser)
                },
            )
            ResultatPeriode(
                periode = periode,
                resultat = bPsBeregnedeTotalbidragBeregning.beregn(periodegrunnlag),
                grunnlag = periodegrunnlag,
            )
        }

        return BeregnBPsBeregnedeTotalbidragResultat(resultater)
    }

    private fun finnKursForPeriode(
        bidrag: LøpendeBidragCore,
        periode: Periode,
        valutakurser: List<ValutakursPeriodeCore>,
    ): LøpendeBidragCore {
        if (bidrag.valutakode == Valutakode.NOK) {
            return bidrag.copy(valutakursTilNOK = null, valutakursGrunnlagsreferanse = null)
        }
        val kurser = valutakurser.filter {
            val datoTil = it.periode.datoTil
            !it.periode.datoFom.isAfter(periode.datoFom) &&
                (datoTil == null || !datoTil.isBefore(requireNotNull(periode.datoTil)))
        }.flatMap { grunnlag ->
            grunnlag.valutakursListe
                .filter { it.valutakode1 == bidrag.valutakode && it.valutakode2 == Valutakode.NOK }
                .map { grunnlag.referanse to it.valutakurs }
        }
        require(kurser.isNotEmpty()) { "Kurs kreves for å regne ut bidrag i NOK" }
        require(kurser.size == 1) { "Flere valutakurser dekker samme valuta og beregningsperiode" }
        val (referanse, kurs) = kurser.single()
        require(kurs.signum() > 0) { "Valutakurs til NOK må være større enn null" }
        return bidrag.copy(valutakursTilNOK = kurs, valutakursGrunnlagsreferanse = referanse)
    }
}
