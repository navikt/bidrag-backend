package no.nav.bidrag.grunnlag.bo

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

data class ValutakursgrunnlagBo(
    @Schema(description = "Valutakursgrunnlag-id")
    val valutakursgrunnlagId: Int = 0,

    @Schema(description = "Angir om nåværende tidspunkt er på eller etter brukFra og før brukTil. Beregnes ved lesing, uavhengig av kursstatus.")
    val aktiv: Boolean = true,

    @Schema(description = "Tidspunkt valutakursgrunnlaget tas i bruk")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Tidspunkt valutakursgrunnlaget ikke lenger er aktiv. Null betyr at valutagrunnlaget er aktivt")
    val brukTil: LocalDateTime? = null,

    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Observert kurs. Null dersom henting av valuta feilet")
    val kurs: BigDecimal?,

    @Schema(description = "Eksponent i tiende potens slik at en multiplikasjon av observasjonsverdien med 10^multiplikator gir verdien av en enhet. Null dersom henting av valuta feilet")
    val multiplikator: Int?,

    @Schema(description = "Første valuta i et valutakvoteringspar. Også kalt transaksjonsvaluta")
    val basisvaluta: Valutakode,

    @Schema(description = "Andre valuta i et valutakvoteringspar. Også kalt motvaluta")
    val kvoteringsvaluta: Valutakode = Valutakode.NOK,

    @Schema(description = "Dersom true har henting av valutakurs feilet. Det kan være på grunn av at valutakurs ikke er støttet eller en annen teknisk feil")
    val feiletHenting: Boolean = false,

    @Schema(description = "Status for kursgrunnlaget")
    val status: ValutakursgrunnlagStatus = if (feiletHenting) ValutakursgrunnlagStatus.FEILET else ValutakursgrunnlagStatus.HENTET,

    @Schema(description = "Tidspunktet kursgrunnlaget sist ble oppdatert")
    val oppdatertTidspunkt: LocalDateTime? = null,

    @Schema(description = "Kilden til kursen, eller null hvis ingen kurs ble funnet")
    val kilde: ValutakursgrunnlagKilde? = null,

    @Schema(description = "Første dag i måneden kursen er hentet fra, eller null hvis ingen kurs ble funnet")
    val observasjonsdato: LocalDate? = null,
)

fun ValutakursgrunnlagBo.toValutakursgrunnlagEntity() = with(::Valutakursgrunnlag) {
    val propertiesByName = ValutakursgrunnlagBo::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.mapNotNull { parameter ->
            val name = parameter.name ?: return@mapNotNull null
            propertiesByName[name]?.let { parameter to it.get(this@toValutakursgrunnlagEntity) }
        }.toMap(),
    ).also { it.status = status }
}
