package no.nav.bidrag.aktoerregister.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Version
import java.sql.Timestamp

@Entity
class Hendelse(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sekvensnummer")
    var sekvensnummer: Int? = null,

    @ManyToOne
    @JoinColumn(name = "aktoer_id", referencedColumnName = "id")
    var aktør: Aktør? = null,

    @Column(name = "aktoer_ident")
    var aktørIdent: String,

    @Version
    @Column(name = "sist_endret")
    var sistEndret: Timestamp? = null,

    @Column(name = "kontonummer_oppdatering")
    var kontonummerOppdatering: Boolean? = null,

    @Column(name = "ident_oppdatering")
    var identOppdatering: Boolean? = null,

    @Column(name = "navn_oppdatering")
    var navnOppdatering: Boolean? = null,

    @Column(name = "adresse_oppdatering")
    var adresseOppdatering: Boolean? = null,

    @Column(name = "fodt_dato_oppdatering")
    var fødtDatoOppdatering: Boolean? = null,

    @Column(name = "dod_dato_oppdatering")
    var dødDatoOppdatering: Boolean? = null,

    @Column(name = "gradering_oppdatering")
    var graderingOppdatering: Boolean? = null,

    @Column(name = "dodsbo_oppdatering")
    var dødsboOppdatering: Boolean? = null,

    @Column(name = "sprak_oppdatering")
    var språkOppdatering: Boolean? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hendelse

        if (sekvensnummer != other.sekvensnummer) return false
        if (aktørIdent != other.aktørIdent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sekvensnummer
        result = 31 * (result ?: 0) + aktørIdent.hashCode()
        return result
    }

    override fun toString(): String = "Hendelse(" +
        "sekvensnummer=$sekvensnummer," +
        "aktør=${aktør?.id}, " +
        "aktoerIdent='$aktørIdent', " +
        "sistEndret=$sistEndret)"

    constructor(sekvensnummer: Int, aktør: Aktør) : this(sekvensnummer = sekvensnummer, aktør = aktør, aktørIdent = aktør.aktørIdent)
}
