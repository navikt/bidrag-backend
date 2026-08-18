package no.nav.bidrag.admin.persistence.entity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import java.time.LocalDateTime

@Entity(name = "lest_av_bruker")
class LestAvBruker(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endringslogg_endring_id", nullable = false)
    val endringsloggEndring: EndringsloggEndring? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endringslogg_id", nullable = false)
    val endringslogg: Endringslogg? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driftsmelding_historikk_id", nullable = false)
    val driftsmeldingHistorikk: DriftsmeldingHistorikk? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    val person: Person,
    @Column(name = "lest_tidspunkt")
    var lestTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "lestetid_varighet_ms")
    var lestetidVarighetMs: Long? = null,
) {
    // equals/hashCode based on id only — avoids LazyInitializationException when used in HashSet
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LestAvBruker) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
