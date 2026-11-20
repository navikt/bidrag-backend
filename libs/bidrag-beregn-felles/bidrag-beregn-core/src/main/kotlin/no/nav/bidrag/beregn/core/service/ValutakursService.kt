package no.nav.bidrag.beregn.core.service

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.core.consumer.ValutakursConsumer
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtalePeriodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutaPar
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutakursGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.MathContext
import java.time.YearMonth

@Service
@Import(ValutakursConsumer::class)
class ValutakursService(private val consumer: ValutakursConsumer) {
    fun hentValutakursgrunnlag(beregningsgrunnlag: List<BeregnGrunnlag>): List<GrunnlagDto> {
        val oppslag = mutableSetOf<Pair<Valutakode, YearMonth>>()
        for (beregnGrunnlag in beregningsgrunnlag) {
            val beregningsperiode = beregnGrunnlag.periode
            val valutaperioder = finnValutaperioder(beregnGrunnlag.grunnlagListe, beregningsperiode)
            for ((valuta, periode) in valutaperioder) {
                if (valuta == Valutakode.NOK) continue
                val beregnTil = requireNotNull(beregningsperiode.til) { "Beregningsperioden må ha en sluttdato ved henting av valutakurs" }
                val fra = maxOf(periode.fom, beregningsperiode.fom)
                val til = minOf(periode.til ?: beregnTil, beregnTil)
                if (fra >= til) continue
                var halvårsstart = YearMonth.of(fra.year, if (fra.monthValue < 7) 1 else 7)
                while (halvårsstart < til) {
                    oppslag.add(valuta to halvårsstart)
                    halvårsstart = halvårsstart.plusMonths(6)
                }
            }
        }

        val resultat = mutableListOf<GrunnlagDto>()
        // API-et tilbyr enkeltoppslag. Samme valuta og halvår hentes bare én gang per beregning.
        for ((valuta, halvårsstart) in oppslag.sortedWith(compareBy({ it.first.name }, { it.second }))) {
            val kursgrunnlag = consumer.hentValutakurs(valuta, halvårsstart.atDay(1))
            val kurs = requireNotNull(kursgrunnlag.kurs) { "Valutakurs er ikke tilgjengelig" }
            require(!kursgrunnlag.feiletHenting && kurs.signum() > 0 && kursgrunnlag.multiplikator == 0) {
                "Valutakursgrunnlaget må inneholde en gyldig, normalisert kurs"
            }
            require(kursgrunnlag.basisvaluta == valuta && kursgrunnlag.kvoteringsvaluta == Valutakode.NOK) {
                "Valutakursgrunnlaget har feil valutapar"
            }
            require(
                kursgrunnlag.brukFra == halvårsstart.atDay(1).atStartOfDay() &&
                    kursgrunnlag.brukTil == halvårsstart.plusMonths(6).atDay(1).atStartOfDay(),
            ) { "Valutakursgrunnlaget må dekke det forespurte halvåret" }
            resultat.add(
                GrunnlagDto(
                    referanse = "innhentet_valutakurs_${valuta.name}_$halvårsstart",
                    type = Grunnlagstype.VALUTAKURS_GRUNNLAG,
                    innhold = POJONode(
                        ValutakursGrunnlag(
                            periode = ÅrMånedsperiode(halvårsstart, halvårsstart.plusMonths(6)),
                            valutakursListe = listOf(
                                ValutaPar(valuta, Valutakode.NOK, kurs),
                                ValutaPar(Valutakode.NOK, valuta, BigDecimal.ONE.divide(kurs, MathContext.DECIMAL128)),
                            ),
                        ),
                    ),
                ),
            )
        }
        return resultat
    }

    private fun finnValutaperioder(
        grunnlagListe: List<GrunnlagDto>,
        beregningsperiode: ÅrMånedsperiode,
    ): List<Pair<Valutakode, ÅrMånedsperiode>> = grunnlagListe.flatMap { grunnlag ->
        when (grunnlag.type) {
            Grunnlagstype.LØPENDE_BIDRAG_PERIODE -> grunnlag.innholdTilObjekt<LøpendeBidragPeriode>().let {
                listOf(it.valutakode to it.periode)
            }

            Grunnlagstype.LØPENDE_BIDRAG -> grunnlag.innholdTilObjekt<LøpendeBidragGrunnlag>().løpendeBidragListe.map {
                it.valutakode to beregningsperiode
            }

            Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG -> grunnlag.innholdTilObjekt<PrivatAvtalePeriodeGrunnlag>().let {
                listOf((it.valutakode ?: Valutakode.NOK) to it.periode)
            }

            else -> emptyList()
        }
    }
}
