package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.Skattegrunnlagspost
import java.math.BigDecimal
import kotlin.reflect.full.memberProperties

data class SkattegrunnlagspostBo(

    @Schema(description = "Skattegrunnlag-id")
    val skattegrunnlagId: Int = 0,

    @Schema(description = "Ordinær eller Svalbard")
    val skattegrunnlagType: String,

    @Schema(description = "Type inntekt, Lonnsinntekt, Naeringsinntekt, Pensjon eller trygd, Ytelse fra offentlig")
    val inntektType: String,

    @Schema(description = "Belop")
    val belop: BigDecimal,
)

fun SkattegrunnlagspostBo.toSkattegrunnlagspostEntity() = with(::Skattegrunnlagspost) {
    val propertiesByName = SkattegrunnlagspostBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Skattegrunnlagspost::skattegrunnlagspostId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toSkattegrunnlagspostEntity)
            }
        },
    )
}
