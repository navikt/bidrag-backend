package no.nav.bidrag.beregn.særbidrag.core.bpsberegnedetotalbidrag.dto

import no.nav.bidrag.beregn.core.bo.SjablonPeriode
import no.nav.bidrag.beregn.core.dto.PeriodeCore
import no.nav.bidrag.beregn.core.dto.SjablonResultatGrunnlagCore
import no.nav.bidrag.beregn.særbidrag.core.felles.dto.IResultatPeriode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeregnetBidragPerBarn
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagInnhold
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutaPar
import java.math.BigDecimal
import java.time.LocalDate

data class LøpendeBidragGrunnlagCore(
    val beregnDatoFra: LocalDate,
    val beregnDatoTil: LocalDate,
    val referanse: String,
    val løpendeBidragCoreListe: List<LøpendeBidragCore>,
    val grunnlagsreferanseListe: List<String>,
    val sjablonPeriodeListe: List<SjablonPeriode>,
    val valutakursPeriodeListe: List<ValutakursPeriodeCore> = emptyList(),
) : GrunnlagInnhold

data class ValutakursPeriodeCore(
    val referanse: String,
    val periode: PeriodeCore,
    val valutakursListe: List<ValutaPar>,
)

data class LøpendeBidragCore(
    val saksnummer: Saksnummer,
    val fødselsdatoBarn: LocalDate,
    val personidentBarn: Personident,
    val referanseBarn: String,
    val løpendeBeløp: BigDecimal,
    val valutakode: Valutakode,
    val samværsklasse: Samværsklasse,
    val beregnetBeløp: BigDecimal,
    val faktiskBeløp: BigDecimal,
    val vedtaksid: Int? = null,
    val bruttoBidragEtterBarnetilleggBM: BigDecimal? = null,
    val bruttoBidragEtterBarnetilleggBP: BigDecimal? = null,
    val erVedtakKildeBBM: Boolean = true,
    val valutakursTilNOK: BigDecimal? = null,
    val valutakursGrunnlagsreferanse: String? = null,
)

// Resultatperiode
data class BeregnBPsBeregnedeTotalbidragResultatCore(val resultatPeriodeListe: List<ResultatPeriodeCore>, val sjablonListe: List<SjablonResultatGrunnlagCore>)

data class ResultatPeriodeCore(
    override val periode: PeriodeCore,
    val resultat: ResultatBeregningCore,
    override val grunnlagsreferanseListe: MutableList<String>,
) : IResultatPeriode

data class ResultatBeregningCore(val bPsBeregnedeTotalbidrag: BigDecimal, val beregnetBidragPerBarn: List<BeregnetBidragPerBarn>)
