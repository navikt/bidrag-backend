package no.nav.bidrag.admin.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDate

@Entity(name = "endringslogg_endring")
class EndringsloggEndring(
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endringslogg_id", nullable = false)
    open val endringslogg: Endringslogg,
    var rekkefølgeIndeks: Int,
    var innhold: String,
    var tittel: String,
    @Enumerated(EnumType.STRING)
    var endringstype: Endringstype = Endringstype.ENDRING,
    open var opprettetTidspunkt: LocalDate = LocalDate.now(),
//    @OneToMany(
//        mappedBy = "endringsloggEndring",
//        cascade = [CascadeType.ALL],
//        orphanRemoval = true,
//        fetch = FetchType.EAGER,
//    )
    @Transient
    var brukerLesinger: MutableSet<LestAvBruker> = mutableSetOf(),
) {
    override fun toString(): String = "EndringsloggEndring(id=$id, innhold='$innhold', tittel='$tittel', rekkefølgeIndeks=$rekkefølgeIndeks)"
}
