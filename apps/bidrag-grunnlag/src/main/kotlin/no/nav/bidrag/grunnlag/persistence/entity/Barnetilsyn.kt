package no.nav.bidrag.grunnlag.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
data class Barnetilsyn(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "barnetilsyn_id")
    val barnetilsynId: Int = 0,

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

    @Column(nullable = true, name = "belop")
    val belop: Int? = null,

    @Column(nullable = true, name = "tilsyntype")
    val tilsynstype: Tilsynstype?,

    @Column(nullable = true, name = "skolealder")
    val skolealder: Skolealder?,

    @Column(nullable = false, name = "hentet_tidspunkt")
    val hentetTidspunkt: LocalDateTime = LocalDateTime.now(),
)
