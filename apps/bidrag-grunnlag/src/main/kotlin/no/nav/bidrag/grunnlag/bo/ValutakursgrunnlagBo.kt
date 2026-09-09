package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class ValutakursgrunnlagBo(
    @Schema(description = "Valutakursgrunnlag-id")
    val valutakursgrunnlagId: Int = 0,

    @Schema(description = "Angir om et valutakursgrunnlag er aktivt")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt valutakursgrunnlaget taes i bruk")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Tidspunkt valutakursgrunnlaget ikke lenger er aktiv. Null betyr at valutagrunnlaget er aktivt")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Observert kurs")
    val kurs: BigDecimal = BigDecimal.ONE,

    @Schema(description = """
Eksponent i tiende potens slik at en multiplikasjon av observasjonsverdien med 10^multiplikator gir verdien av en enhet.

For eksempel:
basisvaluta = SEK
kvoteringsvaluta = NOK
multiplikator = 2
kurs = 100,8

=> 1 SEK * 10^2 = 100,80 NOK
=> 38 NOK = (38 NOK / 100,80) * 100^2 = 37,70 SEK
"""
    )
    val multiplikator: Int = 0,

    @Schema(description = "Første valuta i et valutakvoteringspar. Også kalt transaksjonsvaluta")
    val basisvaluta: Valutakode,

    @Schema(description = "Andre valuta i et valutakvoteringspar. Også kalt motvaluta")
    val kvoteringsvaluta: Valutakode = Valutakode.NOK,
)

fun ValutakursgrunnlagBo.toValutakursgrunnlagEntity() = with(::Valutakursgrunnlag) {
    val propertiesByName = ValutakursgrunnlagBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toValutakursgrunnlagEntity)
            }
        },
    )
}