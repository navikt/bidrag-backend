package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import java.time.LocalDate
import java.time.LocalDateTime

data class BarnetilsynDto(
    @Schema(description = "Id til personen som mottar barnetilsynet")
    val partPersonId: String,
    @Schema(description = "Id til barnet barnetilsynet er for")
    val barnPersonId: String,
    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate?,
    @Schema(description = "Angir om en stønadsopplysning er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt stønadsopplysningen tas i bruk som grunnlag")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt stønadsopplysning ikke lenger aktiv som grunnlag. Null betyr at stønadsopplysningen er aktiv")
    val brukTil: LocalDateTime?,
    @Schema(description = "Beløpet barnetilsynet er på")
    val belop: Int?,
    @Schema(description = "Angir om barnetilsynet er heltid eller deltid")
    val tilsynstype: Tilsynstype?,
    @Schema(description = "Angir om barnet er over eller under skolealder")
    val skolealder: Skolealder?,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

data class BarnetilsynGrunnlagDto(
    @Schema(description = "Id til personen som mottar barnetilsynet")
    val partPersonId: String,
    @Schema(description = "Id til barnet barnetilsynet er for")
    val barnPersonId: String,
    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,
    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate?,
    @Schema(description = "Beløpet barnetilsynet er på")
    val beløp: Int?,
    @Schema(description = "Angir om barnetilsynet er heltid eller deltid")
    val tilsynstype: Tilsynstype?,
    @Schema(description = "Angir om barnet er over eller under skolealder")
    val skolealder: Skolealder?,
)
