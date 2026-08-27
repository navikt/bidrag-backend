package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class BarnetilleggDto(
    @Schema(description = "Id til personen barnetillegg er rapportert for")
    val partPersonId: String,
    @Schema(description = "Id til barnet barnetillegget er rapportert for")
    val barnPersonId: String,
    @Schema(description = "Type barnetillegg")
    val barnetilleggType: String,
    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,
    @Schema(description = "Angir om en stønad er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt stønaden taes i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt stønaden ikke lenger er aktiv som grunnlag. Null betyr at stønaden er aktiv")
    val brukTil: LocalDateTime?,
    @Schema(description = "Bruttobeløp")
    val belopBrutto: BigDecimal,
    @Schema(description = "Angir om barnet er felles- eller særkullsbarn")
    val barnType: String,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

data class BarnetilleggGrunnlagDto(
    @Schema(description = "Id til personen barnetillegg er rapportert for")
    val partPersonId: String,
    @Schema(description = "Id til barnet barnetillegget er rapportert for")
    val barnPersonId: String,
    @Schema(description = "Type barnetillegg")
    val barnetilleggType: String,
    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,
    @Schema(description = "Bruttobeløp")
    val beløpBrutto: BigDecimal,
    @Schema(description = "Angir om barnet er felles- eller særkullsbarn")
    val barnType: String,
)
