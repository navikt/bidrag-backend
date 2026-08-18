package no.nav.bidrag.admin.persistence.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity(name = "driftsmelding_historikk")
class DriftsmeldingHistorikk(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driftsmelding_id", nullable = false)
    open val driftsmelding: Driftsmelding,
    open var opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    open var aktivFraTidspunkt: LocalDateTime? = null,
    open var aktivTilTidspunkt: LocalDateTime? = null,
    var innhold: String,
    val opprettetAv: String,
    val opprettetAvNavn: String,
    @Schema(enumAsRef = true)
    var status: DriftsmeldingStatus,
    @OneToMany(
        mappedBy = "driftsmeldingHistorikk",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    val brukerLesinger: MutableSet<LestAvBruker> = mutableSetOf(),
)

enum class DriftsmeldingStatus {
    KRITISK,
    VARSEL,
    INFO,
    AVSLUTTET,
}
