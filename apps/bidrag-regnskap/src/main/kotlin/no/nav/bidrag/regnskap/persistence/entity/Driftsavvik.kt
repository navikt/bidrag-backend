package no.nav.bidrag.regnskap.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity(name = "driftsavvik")
data class Driftsavvik(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driftsavvik_id")
    val driftsavvikId: Int? = null,

    @Column(name = "palop_id")
    val påløpId: Int? = null,

    @Column(name = "tidspunkt_fra")
    val tidspunktFra: LocalDateTime,

    @Column(name = "tidspunkt_til")
    val tidspunktTil: LocalDateTime? = null,

    @Column(name = "opprettet_av")
    val opprettetAv: String? = null,

    @Column("skal_stoppe_innlesning")
    val skalStoppeInnlesning: Boolean = true,

    @Column(name = "arsak")
    val årsak: String? = null,
) {

    override fun toString(): String = this::class.simpleName +
        "(driftsavvikId = $driftsavvikId , " +
        "påløpId = $påløpId , " +
        "tidspunktFra = $tidspunktFra , " +
        "tidspunktTil = $tidspunktTil , " +
        "opprettetAv = $opprettetAv , " +
        "skalStoppeInnlesning = $skalStoppeInnlesning , " +
        "årsak = $årsak )"
}
