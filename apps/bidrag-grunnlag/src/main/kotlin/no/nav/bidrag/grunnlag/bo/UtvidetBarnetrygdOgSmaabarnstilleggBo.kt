package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.UtvidetBarnetrygdOgSmaabarnstillegg
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class UtvidetBarnetrygdOgSmaabarnstilleggBo(

    @Schema(description = "Grunnlagspakke-id")
    val grunnlagspakkeId: Int = 0,

    @Schema(description = "Id til personen inntekten er rapport for")
    val personId: String,

    @Schema(description = "Type stønad")
    val type: String,

    @Schema(description = "Periode fra- og med måned")
    val periodeFra: LocalDate,

    @Schema(description = "Periode til- og med måned")
    val periodeTil: LocalDate?,

    @Schema(description = "Angir om en inntektsopplysning er aktiv")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt inntekten taes i bruk")
    val brukFra: LocalDateTime,

    @Schema(description = "Tidspunkt inntekten ikke lenger aktiv. Null betyr at inntekten er aktiv")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Belop")
    val belop: BigDecimal,

    @Schema(description = "Angir om stønaden er manuelt beregnet")
    val manueltBeregnet: Boolean,

    @Schema(description = "Angir om barnet har delt bosted")
    val deltBosted: Boolean,

    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
)

fun UtvidetBarnetrygdOgSmaabarnstilleggBo.toUtvidetBarnetrygdOgSmaabarnstilleggEntity() = with(::UtvidetBarnetrygdOgSmaabarnstillegg) {
    val propertiesByName = UtvidetBarnetrygdOgSmaabarnstilleggBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                UtvidetBarnetrygdOgSmaabarnstillegg::ubstId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toUtvidetBarnetrygdOgSmaabarnstilleggEntity)
            }
        },
    )
}
