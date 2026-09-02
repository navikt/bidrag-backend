package no.nav.bidrag.automatiskjobb.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime

@Entity(name = "sak")
data class Sak(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Int? = null,
    @Column(nullable = false, unique = true)
    var saksnummer: String = "",
    var bidragspliktig: String? = null,
    var bidragsmottaker: String? = null,
    @OneToMany(mappedBy = "sak", cascade = [CascadeType.ALL], orphanRemoval = true)
    var barn: MutableList<SakBarn> = mutableListOf(),
    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "endret_tidspunkt", nullable = false)
    var endretTidspunkt: LocalDateTime = LocalDateTime.now(),
) : EntityObject {
    fun finnBarn(kravhaver: String): SakBarn? = barn.find { it.kravhaver == kravhaver }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        if (this.javaClass != oEffectiveClass) return false
        other as Sak

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Sak(id=$id, saksnummer='$saksnummer', antallBarn=${barn.size}, endretTidspunkt=$endretTidspunkt)"
}

@Entity(name = "sak_barn")
data class SakBarn(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "sak_id", nullable = false)
    var sak: Sak? = null,
    @Column(nullable = false)
    var kravhaver: String = "",
    @Column(name = "reell_mottaker")
    var reellMottaker: String? = null,
    @Column(name = "endret_tidspunkt", nullable = false)
    var endretTidspunkt: LocalDateTime = LocalDateTime.now(),
) : EntityObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        if (this.javaClass != oEffectiveClass) return false
        other as SakBarn

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "SakBarn(id=$id, endretTidspunkt=$endretTidspunkt)"
}
