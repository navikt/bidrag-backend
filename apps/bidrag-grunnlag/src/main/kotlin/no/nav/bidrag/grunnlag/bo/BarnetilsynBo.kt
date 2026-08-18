package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.grunnlag.persistence.entity.Barnetilsyn
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class BarnetilsynBo(

    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int = 0,

    @Schema(description = "Id til personen som mottar barnetilsynet")
    val partPersonId: String = "",

    @Schema(description = "Id til barnet barnetilsynet er for")
    val barnPersonId: String = "",

    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,

    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate?,

    @Schema(description = "Angir om en inntektsopplysning er aktiv")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt inntekten taes i bruk")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Tidspunkt inntekten ikke lenger aktiv. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Beløpet barnetilsynet er på")
    val belop: Int? = null,

    @Schema(description = "Angir om barnetilsynet er heltid eller deltid")
    val tilsynstype: Tilsynstype?,

    @Schema(description = "Angir om barnet er over eller under skolealder")
    val skolealder: Skolealder?,

    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

fun BarnetilsynBo.toBarnetilsynEntity() = with(::Barnetilsyn) {
    val propertiesByName = BarnetilsynBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameters ->
            when (parameters.name) {
                Barnetilsyn::barnetilsynId.name -> 0
                else -> propertiesByName[parameters.name]?.get(this@toBarnetilsynEntity)
            }
        },
    )
}
