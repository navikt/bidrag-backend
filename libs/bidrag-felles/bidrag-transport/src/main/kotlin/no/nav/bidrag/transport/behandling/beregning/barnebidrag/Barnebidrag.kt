package no.nav.bidrag.transport.behandling.beregning.barnebidrag

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.beregning.Beregningstype
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Collections.emptyList

@Schema(description = "Resultatet av en barnebidragsberegning")
data class BeregnetBarnebidragResultat(
    @Schema(description = "Periodisert liste over resultat av barnebidragsberegning")
    var beregnetBarnebidragPeriodeListe: List<ResultatPeriode> = emptyList(),
    @Schema(description = "Liste over grunnlag brukt i beregning")
    var grunnlagListe: List<GrunnlagDto> = emptyList(),
)

@Schema(description = "Resultatet av en barnebidragsberegning versjon 2")
data class BeregnetBarnebidragResultatV2(
    val søknadsbarnreferanse: Grunnlagsreferanse,
    val beregnetBarnebidragResultat: BeregnetBarnebidragResultat,
    val fatteVedtakAnbefalt: Boolean = false,
    val avvistRevurderingsbarn: Boolean = false,
)

@Schema(description = "Resultatet av en beregning for en gitt periode - barnebidrag")
data class ResultatPeriode(
    @Schema(description = "Beregnet resultat periode") var periode: ÅrMånedsperiode,
    @Schema(description = "Beregnet resultat") var resultat: ResultatBeregning,
    @Schema(description = "Beregnet grunnlag innhold") var grunnlagsreferanseListe: List<String>,
)

@Schema(description = "Resultatet av en beregning")
data class ResultatBeregning(
    @Schema(description = "Resultat beløp") var beløp: BigDecimal?,
)

@Schema(description = "Request til BidragsberegningOrkestrator")
data class BidragsberegningOrkestratorRequest(
    @Schema(description = "Grunnlag for beregning av barnebidrag")
    val beregnGrunnlag: BeregnGrunnlag,
    @Schema(description = "Grunnlag for orkestrering av klage")
    @JsonAlias("klageOrkestratorGrunnlag")
    val omgjøringOrkestratorGrunnlag: OmgjøringOrkestratorGrunnlag? = null,
    val erDirekteAvslag: Boolean = false,
    @Schema(description = "Type beregning")
    val beregningstype: Beregningstype = Beregningstype.BIDRAG,
)

@Schema(description = "Grunnlag for orkestrering av aldersjustering")
data class OmgjøringOrkestratorGrunnlag(
    @get:Schema(description = "Id til stønad")
    val stønad: Stønadsid,
    @get:Schema(description = "Vedtaksid til originale vedtak som ble fattet")
    val omgjørVedtakId: Int,
    @get:Schema(description = "Vedtaksid til påklaget vedtak")
    val påklagetVedtakId: Int,
    val manuellAldersjustering: List<OmgjøringorkestratorManuellAldersjustering> = emptyList(),
    @get:Schema(description = "Om behandlingen gjelder paragraf35c")
    val gjelderParagraf35c: Boolean = false,
    // Om det er klage eller en omgjøring. Bestemmer hvilken vedtakstype som skal brukes i delvedtakene
    val gjelderKlage: Boolean = false,
    // Om det skal innkreves. Utfører ikke aldersjustering/indeksregulering hvis ingen innkreving
    val skalInnkreves: Boolean,
    val erBeregningsperiodeLøpende: Boolean,
    // Hvis null så er det beregningen som bestemmer om det skal fattes vedtak
    val skalFatteVedtakForRevurderingsbarn: Boolean? = null,
)

data class OmgjøringorkestratorManuellAldersjustering(
    val aldersjusteringForÅr: Int,
    val grunnlagFraVedtak: Int?,
    val grunnlagFraOmgjøringsvedtak: Boolean = false,
)

@Schema(description = "Response fra BidragsberegningOrkestrator")
data class BidragsberegningOrkestratorResponse(
    @Schema(description = "Liste over resultat av bidragsberegning")
    val resultatVedtakListe: List<ResultatVedtak> = emptyList(),
)

@Schema(description = "Resultat av bidragsberegning")
data class ResultatVedtak(
    @Schema(description = "Resultat av beregning")
    val resultat: BeregnetBarnebidragResultat,
    @Schema(description = "Er delvedtak?")
    val delvedtak: Boolean = false,
    @Schema(description = "Er klagevedtak eller omgjøringsvedtak")
    @JsonAlias("klagevedtak")
    val omgjøringsvedtak: Boolean = false,
    val beregnet: Boolean = false,
    val vedtakstype: Vedtakstype,
    val beregnetFraDato: LocalDate = resultat.beregnetBarnebidragPeriodeListe.minOf { it.periode.fom }.atDay(1),
) {
    val endeligVedtak get() = !delvedtak && !omgjøringsvedtak
}

data class BidragsberegningOrkestratorRequestV2(
    @Deprecated("Bruk beregningsperiode per barn i stedet")
    val beregningsperiode: ÅrMånedsperiode,
    val beregningBarn: List<BeregningGrunnlagV2>,
    val grunnlagsliste: List<GrunnlagDto>,
    val erDirekteAvslag: Boolean = false,
    val beregningstype: Beregningstype = Beregningstype.BIDRAG,
    val skalHensyntaLøpendeBidrag: Boolean = false,
)

data class BeregningGrunnlagV2(
    val søknadsbarnreferanse: Grunnlagsreferanse,
    @Deprecated("Bruk beregningsperiode for beregning og virkningstidspunkt for vedtaksperiode")
    val periode: ÅrMånedsperiode,
    val beregningsperiode: ÅrMånedsperiode,
    val virkningstidspunkt: YearMonth,
    val opphørsdato: YearMonth? = null,
    val stønadstype: Stønadstype,
    val erDirekteAvslag: Boolean = false,
    val omgjøringOrkestratorGrunnlag: OmgjøringOrkestratorGrunnlag? = null,
)

data class BidragsberegningOrkestratorResponseV2(
    var grunnlagListe: List<GrunnlagDto> = emptyList(),
    val resultat: List<BidragsberegningResultatBarnV2> = emptyList(),
)

data class BidragsberegningResultatBarnV2(
    val søknadsbarnreferanse: Grunnlagsreferanse,
    val resultatVedtakListe: List<ResultatVedtakV2> = emptyList(),
    @Deprecated("Skal ikke brukes. Erstatt med avvisning")
    val beregningsfeil: Exception? = null,
    val fatteVedtakAnbefalt: Boolean = false,
    val avvistRevurderingsbarn: Boolean = false,
)

data class ResultatVedtakV2(
    var periodeListe: List<ResultatPeriode> = emptyList(),
    // Grunnlagliste for delvedtak ved orkestrering. Skal bare inneholde grunnlag fra delvedtak
    // Grunnlagsliste for hovedberegningen skal ligge i BidragsberegningOrkestratorResponseV2
    val grunnlagslisteDelvedtak: List<GrunnlagDto> = emptyList(),
    val delvedtak: Boolean = false,
    val avvisning: Resultatkode? = null,
    val omgjøringsvedtak: Boolean = false,
    val beregnet: Boolean = false,
    val vedtakstype: Vedtakstype,
    val beregnetFraDato: LocalDate = periodeListe.minOfOrNull { it.periode.fom }?.atDay(1) ?: LocalDate.now().withDayOfMonth(1),
)
