package no.nav.bidrag.aktoerregister.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity(name = "tidligere_identer")
class TidligereIdenter(

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "tidligere_aktoer_ident")
    var tidligereAktoerIdent: String,

    @Column(name = "identtype")
    var identtype: String,

    @ManyToOne
    @JoinColumn(name = "aktoer_id")
    var aktør: Aktør? = null,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TidligereIdenter

        if (tidligereAktoerIdent != other.tidligereAktoerIdent) return false
        if (identtype != other.identtype) return false
        if (aktør != other.aktør) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tidligereAktoerIdent.hashCode()
        result = 31 * result + identtype.hashCode()
        result = 31 * result + (aktør?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "TidligereIdenter(" +
        "id=$id, " +
        "tidligereAktoerIdent='$tidligereAktoerIdent', " +
        "identtype='$identtype', " +
        "aktør=${aktør?.id})"
}
