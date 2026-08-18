package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.grunnlag.persistence.entity.Ainntektspost
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

data class AinntektspostBo(

    @Schema(description = "Inntekt-id")
    val inntektId: Int = 0,

    @Schema(description = "Perioden inntektsposten er utbetalt YYYY-MM")
    val utbetalingsperiode: String?,

    @Schema(description = "Fra-dato for opptjening")
    val opptjeningsperiodeFra: LocalDate?,

    @Schema(description = "Til-dato for opptjening")
    val opptjeningsperiodeTil: LocalDate?,

    @Schema(description = "Id til de som rapporterer inn inntekten")
    val opplysningspliktigId: String?,

    @Schema(description = "Id til virksomheten som rapporterer inn inntekten")
    val virksomhetId: String?,

    @Schema(description = "Type inntekt, Lonnsinntekt, Naeringsinntekt, Pensjon eller trygd, Ytelse fra offentlig")
    val inntektType: String,

    @Schema(description = "Type fordel, Kontantytelse, Naturalytelse, Utgiftsgodtgjorelse")
    val fordelType: String?,

    @Schema(description = "Beskrivelse av inntekt")
    val beskrivelse: String?,

    @Schema(description = "Belop")
    val belop: BigDecimal,

    @Schema(description = "Fra-dato etterbetaling")
    val etterbetalingsperiodeFra: LocalDate?,

    @Schema(description = "Til-dato etterbetaling")
    val etterbetalingsperiodeTil: LocalDate?,
)

fun AinntektspostBo.toAinntektspostEntity() = with(::Ainntektspost) {
    val propertiesByName = AinntektspostBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                Ainntektspost::inntektspostId.name -> 0
                else -> propertiesByName[parameter.name]?.get(this@toAinntektspostEntity)
            }
        },
    )
}
