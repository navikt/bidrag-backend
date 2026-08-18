package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.grunnlag.bo.KontantstotteBo
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class Kontantstotte(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kontantstotte_id")
    val kontantstotteId: Int = 0,

    @Column(nullable = false, name = "grunnlagspakke_id")
    val grunnlagspakkeId: Int = 0,

    @Column(nullable = false, name = "part_person_id")
    val partPersonId: String = "",

    @Column(nullable = false, name = "barn_person_id")
    val barnPersonId: String = "",

    @Column(nullable = false, name = "periode_fra")
    val periodeFra: LocalDate = LocalDate.now(),

    @Column(nullable = true, name = "periode_til")
    val periodeTil: LocalDate? = LocalDate.now(),

    @Column(nullable = false, name = "aktiv")
    val aktiv: Boolean = true,

    @Column(nullable = false, name = "bruk_fra")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "bruk_til")
    val brukTil: LocalDateTime? = null,

    @Column(nullable = false, name = "belop")
    val belop: Int = 0,

    @Column(nullable = false, name = "hentet_tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

fun Kontantstotte.toKontantstotteBo() = with(::KontantstotteBo) {
    val propertiesByName = Kontantstotte::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameters ->
            when (parameters.name) {
                else -> propertiesByName[parameters.name]?.get(this@toKontantstotteBo)
            }
        },
    )
}
