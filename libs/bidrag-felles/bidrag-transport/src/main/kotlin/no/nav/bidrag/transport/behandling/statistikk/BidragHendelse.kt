package no.nav.bidrag.transport.behandling.statistikk

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class BidragHendelse(
    val vedtaksid: Int,
    val vedtakstidspunkt: LocalDateTime,
    val stønadstype: Stønadstype,
    val type: String,
    val saksnr: String,
    val skyldner: String,
    val kravhaver: String,
    val mottaker: String,
    val historiskVedtak: Boolean,
    val innkreving: Boolean,
    val bidragPeriodeListe: List<BidragPeriode>,
)

data class BidragPeriode(
    val periodeFra: LocalDate,
    val periodeTil: LocalDate?,
    val beløp: BigDecimal?,
    val valutakode: String?,
    val resultat: String,
    val bidragsevne: BigDecimal?,
    val underholdskostnad: BigDecimal?,
    val skyldnersAndelUnderholdskostnad: BigDecimal?,
    val nettoTilsynsutgift: BigDecimal?,
    val faktiskUtgift: BigDecimal?,
    val samværsfradrag: BigDecimal?,
    val nettoBarnetilleggSkyldner: BigDecimal?,
    val nettoBarnetilleggMottaker: BigDecimal?,
    val skyldnerBorMedAndreVoksne: Boolean?,
    val samværsklasse: Samværsklasse?,
    val skyldnerInntektListe: List<Inntekt>?,
    val mottakerInntektListe: List<Inntekt>?,
    val kravhaverInntektListe: List<Inntekt>?,
)
