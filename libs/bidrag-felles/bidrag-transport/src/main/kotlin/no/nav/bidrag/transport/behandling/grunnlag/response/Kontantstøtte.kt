package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class KontantstotteDto(
    @Schema(description = "Id til personen som mottar kontantstøtten")
    val partPersonId: String,
    @Schema(description = "Id til barnet kontantstøtten mottas for")
    val barnPersonId: String,
    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate?,
    @Schema(description = "Angir om en inntektsopplysning er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt inntekten tas i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt inntekten ikke lenger er aktiv som grunnlag. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime?,
    @Schema(description = "Beløpet kontantstøtten er på")
    val belop: Int,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

data class KontantstøtteGrunnlagDto(
    @Schema(description = "Id til personen som mottar kontantstøtten")
    val partPersonId: String,
    @Schema(description = "Id til barnet kontantstøtten mottas for")
    val barnPersonId: String,
    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate?,
    @Schema(description = "Beløpet kontantstøtten er på")
    val beløp: Int,
)
