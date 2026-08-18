package no.nav.bidrag.bbm.persistence.bbm.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "T_PERIODE_BIDRAG")
@IdClass(PeriodeBidragId::class)
@Suppress("unused")
open class PeriodeBidrag(
    @Id
    @Column(name = "SAKSNR")
    open var saksnummer: String? = null,
    @Id
    @Column(name = "DATO_SOKNAD")
    open var datoSøknad: LocalDate? = null,
    @Id
    @Column(name = "DATO_PERIODE_FOM")
    open var datoFom: LocalDate? = null,
    @Id
    @Column(name = "KODE_SOKNADSTYPE")
    open var soknadstype: String? = null,
    @Id
    @Column(name = "PERSON_ID_BARN")
    open var personidentBarn: String? = null,
    @Column(name = "BELOP_BIDRAG_BEREG")
    open var beregnetBeløp: BigDecimal? = null,
    @Column(name = "BELOP_BIDRAG_FAKT")
    open var faktiskBeløp: BigDecimal? = null,
    @Column(name = "BELOP_FOR_SAMVAR")
    open var beløpSamvær: BigDecimal? = null,
    @Column(name = "BELOP_U")
    open var beløpUnderholdskostnad: BigDecimal? = null,
    @Column(name = "BELOP_BARNETILSYN")
    open var beløpBarnetilsyn: BigDecimal? = null,
    @Column(name = "BELOP_BIDRAGSEVNE")
    open var beløpBidragsevne: BigDecimal? = null,
    @Column(name = "BELOP_BP_ANDEL_U")
    open var bpAndelUnderholdskostnad: BigDecimal? = null,
    @Column(name = "FULL_BIDRAGSEVNE")
    open var fullBidragsevne: String? = null,
)

@Suppress("unused")
open class PeriodeBidragId(
    @Column(name = "SAKSNR")
    var saksnummer: String? = null,
    @Column(name = "DATO_SOKNAD")
    var datoSøknad: LocalDate? = null,
    @Column(name = "DATO_PERIODE_FOM")
    var datoFom: LocalDate? = null,
    @Column(name = "KODE_SOKNADSTYPE")
    var soknadstype: String? = null,
    @Column(name = "PERSON_ID_BARN")
    var personidentBarn: String? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as PeriodeBidragId

        if (saksnummer != other.saksnummer) return false
        if (datoSøknad != other.datoSøknad) return false
        if (datoFom != other.datoFom) return false
        if (soknadstype != other.soknadstype) return false
        if (personidentBarn != other.personidentBarn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = saksnummer?.hashCode() ?: 0
        result = 31 * result + (datoSøknad?.hashCode() ?: 0)
        result = 31 * result + (datoFom?.hashCode() ?: 0)
        result = 31 * result + (soknadstype?.hashCode() ?: 0)
        result = 31 * result + (personidentBarn?.hashCode() ?: 0)
        return result
    }
}
