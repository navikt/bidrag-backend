package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class UtvidetBarnetrygdOgSmaabarnstilleggDto(
    @Schema(description = "Id til personen ubst er rapportert for")
    val personId: String,
    @Schema(description = "Type stønad, utvidet barnetrygd eller småbarnstillegg")
    val type: String,
    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,
    @Schema(description = "Angir om en stønad er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt inntekten taes i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt inntekten ikke lenger er aktiv som grunnlag. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime?,
    @Schema(description = "Beløp")
    val belop: BigDecimal,
    @Schema(description = "Angir om stønaden er manuelt beregnet")
    val manueltBeregnet: Boolean,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

data class UtvidetBarnetrygdGrunnlagDto(
    @Schema(description = "Id til personen utvidet barnetrygd er rapportert for")
    val personId: String,
    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,
    @Schema(description = "Beløp")
    val beløp: BigDecimal,
    @Schema(description = "Angir om stønaden er manuelt beregnet")
    val manueltBeregnet: Boolean,
)

data class SmåbarnstilleggGrunnlagDto(
    @Schema(description = "Id til personen småbarnstillegg er rapportert for")
    val personId: String,
    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,
    @Schema(description = "Beløp")
    val beløp: BigDecimal,
    @Schema(description = "Angir om stønaden er manuelt beregnet")
    val manueltBeregnet: Boolean,
)
