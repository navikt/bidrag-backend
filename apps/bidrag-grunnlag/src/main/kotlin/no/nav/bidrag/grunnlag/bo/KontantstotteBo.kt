package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.Kontantstotte
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class KontantstotteBo(

    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int = 0,

    @Schema(description = "Id til personen som mottar kontantstøtten")
    val partPersonId: String = "",

    @Schema(description = "Id til barnet kontantstøtten er for")
    val barnPersonId: String = "",

    @Schema(description = "Periode fra-dato")
    val periodeFra: LocalDate,

    @Schema(description = "Periode til-dato")
    val periodeTil: LocalDate?,

    @Schema(description = "Angir om en inntektsopplysning er aktiv")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt inntekten taes i bruk")
    val brukFra: LocalDateTime? = LocalDateTime.now(),

    @Schema(description = "Tidspunkt inntekten ikke lenger aktiv. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Beløpet kontantstøtten er på")
    val belop: Int,

    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),

)

fun KontantstotteBo.toKontantstotteEntity() = with(::Kontantstotte) {
    val propertiesByName = KontantstotteBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameters ->
            when (parameters.name) {
                Kontantstotte::kontantstotteId.name -> 0
                else -> propertiesByName[parameters.name]?.get(this@toKontantstotteEntity)
            }
        },
    )
}
