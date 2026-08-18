package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.Grunnlagspakke
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class GrunnlagspakkeBo(

    @Schema(description = "opprettet av")
    val opprettetAv: String,

    @Schema(description = "opprettet timestamp")
    val opprettetTimestamp: LocalDateTime,

    @Schema(description = "Endret timestamp")
    val endretTimestamp: LocalDateTime?,

    @Schema(description = "Gyldig til-dato")
    val gyldigTil: LocalDate?,

    @Schema(description = "Til hvilket formål skal grunnlagspakken benyttes. Bidrag, Forskudd, Særtilskudd")
    val formaal: String,
)

fun GrunnlagspakkeBo.toGrunnlagspakkeEntity() = with(::Grunnlagspakke) {
    val propertiesByName = GrunnlagspakkeBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Grunnlagspakke::grunnlagspakkeId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toGrunnlagspakkeEntity)
            }
        },
    )
}
