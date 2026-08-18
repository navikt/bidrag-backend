package no.nav.bidrag.admin.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity(name = "driftsmelding")
class Driftsmelding(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    open var opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    open var aktivFraTidspunkt: LocalDateTime? = null,
    open var aktivTilTidspunkt: LocalDateTime? = null,
    var tittel: String,
    val opprettetAv: String,
    val opprettetAvNavn: String,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "driftsmelding",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    open var historikk: MutableSet<DriftsmeldingHistorikk> = mutableSetOf(),
)
