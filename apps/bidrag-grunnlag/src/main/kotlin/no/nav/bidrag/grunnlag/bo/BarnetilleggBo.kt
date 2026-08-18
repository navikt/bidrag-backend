package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.Barnetillegg
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class BarnetilleggBo(

    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int = 0,

    @Schema(description = "Id til personen barnetillegget er rapport for")
    val partPersonId: String,

    @Schema(description = "Id til barnet barnetillegget gjelder for")
    val barnPersonId: String,

    @Schema(description = "Type barnetillegg")
    val barnetilleggType: String,

    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,

    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,

    @Schema(description = "Angir om en barnetilleggopplysning er aktiv")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt barnetillegget taes i bruk")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Tidspunkt barnetillegget ikke lenger er aktivt. Null betyr at barnetillegget er aktivt")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Bruttobeløp")
    val belopBrutto: BigDecimal,

    @Schema(description = "Angir om barnet er felles- eller særkullsbarn")
    val barnType: String = "",

    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

fun BarnetilleggBo.toBarnetilleggEntity() = with(::Barnetillegg) {
    val propertiesByName = BarnetilleggBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Barnetillegg::barnetilleggId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toBarnetilleggEntity)
            }
        },
    )
}
