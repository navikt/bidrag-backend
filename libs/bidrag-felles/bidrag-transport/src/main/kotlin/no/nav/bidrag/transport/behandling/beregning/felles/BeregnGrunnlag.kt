package no.nav.bidrag.transport.behandling.beregning.felles

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import java.time.YearMonth

@Schema(description = "Grunnlaget for en beregning av barnebidrag, forskudd og særbidrag")
data class BeregnGrunnlag(
    @Schema(description = "Beregningsperiode") val periode: ÅrMånedsperiode,
    @Schema(description = "Dato hvis siste periode skal opphøres") val opphørsdato: YearMonth? = null,
    @Schema(description = "Stønadstype som skal beregnes") val stønadstype: Stønadstype = Stønadstype.BIDRAG,
    @Schema(description = "Referanse til Person-objekt som tilhører søknadsbarnet") val søknadsbarnReferanse: String,
    @Schema(description = "Periodisert liste over grunnlagselementer") val grunnlagListe: List<GrunnlagDto> = emptyList(),
) {
    val opphørSistePeriode get() = opphørsdato != null
}

fun BeregnGrunnlag.valider() {
    require(søknadsbarnReferanse.isNotEmpty()) { "søknadsbarnReferanse kan ikke være tom streng" }
    require(grunnlagListe.isNotEmpty()) { "grunnlagListe kan ikke være tom" }
    requireNotNull(periode.til) { "beregningsperiode til kan ikke være null" }
    grunnlagListe.forEach(GrunnlagDto::valider)
}

@Schema(description = "Grunnlaget for en beregning av aldersjustering for barnebidrag og forskudd")
data class BeregnGrunnlagAldersjustering(
    @Schema(description = "Beregningsperiode") val periode: ÅrMånedsperiode,
    @Schema(description = "Liste over personer som er involvert i beregningen") val personObjektListe: List<GrunnlagDto>,
    @Schema(description = "Liste over beløpshistorikk") val beløpshistorikkListe: List<GrunnlagDto>,
    @Schema(description = "Liste over vedtak") val vedtakListe: List<BeregnGrunnlagVedtak>,
)

@Schema(description = "Vedtak som skal brukes ved beregning av aldersjustering")
data class BeregnGrunnlagVedtak(
    @Schema(description = "Gjelder barn - referanse til personobjekt for søknadsbarn") val gjelderBarnReferanse: Grunnlagsreferanse,
    @Schema(description = "Vedtak id") val vedtakId: Int,
    @Schema(description = "Vedtak innhold") val vedtakInnhold: VedtakDto,
)

fun BeregnGrunnlagAldersjustering.valider() {
    requireNotNull(periode.til) { "beregningsperiode til kan ikke være null" }
    require(personObjektListe.isNotEmpty()) { "personObjektListe kan ikke være tom" }
    require(beløpshistorikkListe.isNotEmpty()) { "beløpshistorikkListe kan ikke være tom" }
    require(vedtakListe.isNotEmpty()) { "vedtakListe kan ikke være tom" }
    personObjektListe.forEach(GrunnlagDto::valider)
    beløpshistorikkListe.forEach(GrunnlagDto::valider)
}
