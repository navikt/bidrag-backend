package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class AinntektDto(
    @Schema(description = "Id til personen inntekten er rapportert for")
    val personId: String,
    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate,
    @Schema(description = "Angir om en inntektsopplysning er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt inntekten taes i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt inntekten ikke lenger er aktiv som grunnlag. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime?,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
    @Schema(description = "Liste over poster for innhentede inntektsposter")
    val ainntektspostListe: List<AinntektspostDto>,
)

data class AinntektGrunnlagDto(
    @Schema(description = "Id til personen inntekten er rapportert for")
    val personId: String,
    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate,
    @Schema(description = "Liste over poster for innhentede inntektsposter")
    val ainntektspostListe: List<AinntektspostDto>,
)

data class AinntektspostDto(
    @Schema(description = "Perioden innteksposten er utbetalt YYYYMM")
    val utbetalingsperiode: String?,
    @Schema(description = "Fra-dato for opptjening")
    val opptjeningsperiodeFra: LocalDate?,
    @Schema(description = "Til-dato for opptjening")
    val opptjeningsperiodeTil: LocalDate?,
    @Schema(description = "Id til de som rapporterer inn inntekten")
    val opplysningspliktigId: String?,
    @Schema(description = "Id til virksomheten som rapporterer inn inntekten")
    val virksomhetId: String?,
    @Schema(description = "Type fordel: Kontantytelse, Naturalytelse, Utgiftsgodtgjorelse")
    val fordelType: String?,
    @Schema(description = "Beskrivelse av inntekt")
    val beskrivelse: String?,
    @Schema(description = "Fra-dato etterbetaling")
    val etterbetalingsperiodeFra: LocalDate?,
    @Schema(description = "Til-dato etterbetaling")
    val etterbetalingsperiodeTil: LocalDate?,
    @Schema(description = "Type inntekt: Lonnsinntekt, Naeringsinntekt, Pensjon eller trygd, Ytelse fra offentlig")
    @Deprecated("", replaceWith = ReplaceWith("kategori"))
    val inntektType: String,
    @Schema(
        description = "Type inntekt: Lonnsinntekt, Naeringsinntekt, Pensjon eller trygd, Ytelse fra offentlig",
        allowableValues = ["LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"],
    )
    val kategori: String = inntektType,
    @Schema(description = "Beløp")
    @Deprecated("", replaceWith = ReplaceWith("beløp"))
    val belop: BigDecimal,
    @Schema(description = "Beløp")
    val beløp: BigDecimal = belop,
)
