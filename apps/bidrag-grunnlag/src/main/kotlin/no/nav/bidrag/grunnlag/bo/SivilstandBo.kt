package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.Sivilstand
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class SivilstandBo(

    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int = 0,

    @Schema(description = "Person-id til personen sivilstanden gjelder for")
    val personId: String,

    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate?,

    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,

    @Schema(description = "Kode for sivilstand i perioden")
    val sivilstand: String,

    @Schema(description = "Angir om en sivilstand er aktiv")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt sivilstanden taes i bruk")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Tidspunkt sivilstanden ikke lenger er aktiv. Null betyr at sivilstanden er aktiv")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Opprettet tidspunkt")
    val hentetTidspunkt: LocalDateTime,

)

fun SivilstandBo.toSivilstandEntity() = with(::Sivilstand) {
    val propertiesByName = SivilstandBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Sivilstand::sivilstandId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toSivilstandEntity)
            }
        },
    )
}
