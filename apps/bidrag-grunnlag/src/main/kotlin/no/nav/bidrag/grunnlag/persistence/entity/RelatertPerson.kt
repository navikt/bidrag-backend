package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.grunnlag.bo.RelatertPersonBo
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

@Entity
data class RelatertPerson(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relatert_person_id")
    val relatertPersonId: Int = 0,

    @Column(nullable = false, name = "grunnlagspakke_id")
    val grunnlagspakkeId: Int = 0,

    @Column(nullable = false, name = "part_person_id")
    val partPersonId: String = "",

    @Column(nullable = true, name = "relatert_person_person_id")
    val relatertPersonPersonId: String? = null,

    @Column(nullable = true, name = "navn")
    val navn: String? = null,

    @Column(nullable = true, name = "fodselsdato")
    val fodselsdato: LocalDate? = null,

    @Column(nullable = false, name = "er_barn_av_bm_bp")
    val erBarnAvBmBp: Boolean = false,

    @Column(nullable = true, name = "husstandsmedlem_periode_fra")
    val husstandsmedlemPeriodeFra: LocalDate? = null,

    @Column(nullable = true, name = "husstandsmedlem_periode_til")
    val husstandsmedlemPeriodeTil: LocalDate? = null,

    @Column(nullable = false, name = "aktiv")
    val aktiv: Boolean = true,

    @Column(nullable = false, name = "bruk_fra")
    val brukFra: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true, name = "bruk_til")
    val brukTil: LocalDateTime? = null,

    @Column(nullable = false, name = "hentet_tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

fun RelatertPerson.toRelatertPersonBo() = with(::RelatertPersonBo) {
    val propertiesByName = RelatertPerson::class.memberProperties.associateBy { it.name }
    callBy(
        parameters.associateWith { parameter ->
            when (parameter.name) {
                else -> propertiesByName[parameter.name]?.get(this@toRelatertPersonBo)
            }
        },
    )
}
