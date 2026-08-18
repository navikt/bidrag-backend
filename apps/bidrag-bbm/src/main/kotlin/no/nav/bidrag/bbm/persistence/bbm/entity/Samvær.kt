package no.nav.bidrag.bbm.persistence.bbm.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate

@Entity
@Table(name = "T_SAMVAR")
@IdClass(SamværId::class)
@Suppress("unused")
open class Samvær(
    @Id
    @Column(name = "SAKSNR")
    open var saksnummer: String? = null,
    @Id
    @Column(name = "DATO_SOKNAD")
    open var datoSøknad: LocalDate? = null,
    @Id
    @Column(name = "KODE_SOKNADSTYPE")
    open var soknadstype: String? = null,
    @Id
    @Column(name = "PERSON_ID_BARN")
    open var personidentBarn: String? = null,
    @Id
    @Column(name = "DATO_GJELDER_FOM")
    open var datoFom: LocalDate? = null,
    @Column(name = "DATO_GJELDER_TOM")
    open var datoTom: LocalDate? = null,
    @Column(name = "KODE_SAMVAER")
    open var samværskode: String? = null,
)

@Suppress("unused")
class SamværId(
    @Column(name = "SAKSNR")
    var saksnummer: String? = null,
    @Column(name = "DATO_SOKNAD")
    var datoSøknad: LocalDate? = null,
    @Column(name = "KODE_SOKNADSTYPE")
    var soknadstype: String? = null,
    @Column(name = "PERSON_ID_BARN")
    var personidentBarn: String? = null,
    @Column(name = "DATO_GJELDER_FOM")
    var datoFom: LocalDate? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as SamværId

        if (saksnummer != other.saksnummer) return false
        if (datoSøknad != other.datoSøknad) return false
        if (soknadstype != other.soknadstype) return false
        if (personidentBarn != other.personidentBarn) return false
        if (datoFom != other.datoFom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = saksnummer?.hashCode() ?: 0
        result = 31 * result + (datoSøknad?.hashCode() ?: 0)
        result = 31 * result + (soknadstype?.hashCode() ?: 0)
        result = 31 * result + (personidentBarn?.hashCode() ?: 0)
        result = 31 * result + (datoFom?.hashCode() ?: 0)
        return result
    }
}
