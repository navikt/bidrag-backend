package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.grunnlag.bo.BarnetilleggBo
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class Barnetillegg(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barnetillegg_id")
    val barnetilleggId: Int = 0,

    @Column(nullable = false, name = "grunnlagspakke_id")
    val grunnlagspakkeId: Int = 0,

    @Column(nullable = false, name = "part_person_id")
    val partPersonId: String = "",

    @Column(nullable = false, name = "barn_person_id")
    val barnPersonId: String = "",

    @Column(nullable = false, name = "barnetillegg_type")
    val barnetilleggType: String = "",

    @Column(nullable = false, name = "periode_fra")
    val periodeFra: LocalDate = LocalDate.now(),

    @Column(nullable = true, name = "periode_til")
    val periodeTil: LocalDate? = null,

    @Column(nullable = false, name = "aktiv")
    val aktiv: Boolean = true,

    @Column(nullable = false, name = "bruk_fra")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "bruk_til")
    val brukTil: LocalDateTime? = null,

    @Column(nullable = false, name = "belop_brutto")
    val belopBrutto: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, name = "barn_type")
    val barnType: String = "",

    @Column(nullable = false, name = "hentet_tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

fun Barnetillegg.toBarnetilleggBo() = with(::BarnetilleggBo) {
    val propertiesByName = Barnetillegg::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toBarnetilleggBo)
            }
        },
    )
}
