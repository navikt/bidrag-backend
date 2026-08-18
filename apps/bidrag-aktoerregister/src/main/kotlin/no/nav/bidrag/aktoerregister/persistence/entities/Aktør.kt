package no.nav.bidrag.aktoerregister.persistence.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Version
import java.sql.Timestamp
import java.time.LocalDate

@Entity(name = "aktoer")
class Aktør(

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @Column(name = "aktoer_ident")
    var aktørIdent: String,

    @Column(name = "aktoertype")
    var aktørType: String,

    @Column(name = "offentlig_id")
    var offentligId: String? = null,

    @Column(name = "offentlig_id_type")
    var offentligIdType: String? = null,

    @Column(name = "norskkontonr")
    var norskKontonr: String? = null,

    @Column(name = "iban")
    var iban: String? = null,

    @Column(name = "swift")
    var swift: String? = null,

    @Column(name = "banknavn")
    var bankNavn: String? = null,

    @Column(name = "banklandkode")
    var bankLandkode: String? = null,

    @Column(name = "bankcode")
    var bankCode: String? = null,

    @Column(name = "valutakode")
    var valutaKode: String? = null,

    @Column(name = "fornavn")
    var fornavn: String? = null,

    @Column(name = "etternavn")
    var etternavn: String? = null,

    @Column(name = "fodt_dato")
    var fødtDato: LocalDate? = null,

    @Column(name = "dod_dato")
    var dødDato: LocalDate? = null,

    @Column(name = "gradering")
    var gradering: String? = null,

    @Column(name = "sprakkode")
    var språkkode: String? = null,

    @Column(name = "adresselinje1")
    var adresselinje1: String? = null,

    @Column(name = "adresselinje2")
    var adresselinje2: String? = null,

    @Column(name = "adresselinje3")
    var adresselinje3: String? = null,

    @Column(name = "leilighetsnummer")
    var leilighetsnummer: String? = null,

    @Column(name = "postnr")
    var postnr: String? = null,

    @Column(name = "poststed")
    var poststed: String? = null,

    @Column(name = "land")
    var land: String? = null,

    @OneToMany(mappedBy = "aktør", cascade = [CascadeType.ALL], orphanRemoval = true)
    var tidligereIdenter: MutableSet<TidligereIdenter> = mutableSetOf(),

    @OneToOne(mappedBy = "aktør", cascade = [CascadeType.ALL], orphanRemoval = true)
    var dødsbo: Dødsbo? = null,

    @OneToMany(mappedBy = "aktør", cascade = [CascadeType.ALL], orphanRemoval = true)
    var hendelser: MutableList<Hendelse> = ArrayList(),

    @Version
    @Column(name = "sist_endret")
    var sistEndret: Timestamp? = null,

) {

    fun addHendelse(hendelse: Hendelse) {
        hendelser.add(hendelse)
    }

    fun oppdaterAlleFelter(aktør: Aktør) {
        this.aktørIdent = aktør.aktørIdent
        this.aktørType = aktør.aktørType
        this.offentligId = aktør.offentligId
        this.offentligIdType = aktør.offentligIdType
        this.norskKontonr = aktør.norskKontonr
        this.iban = aktør.iban
        this.swift = aktør.swift
        this.bankNavn = aktør.bankNavn
        this.bankLandkode = aktør.bankLandkode
        this.bankCode = aktør.bankCode
        this.valutaKode = aktør.valutaKode
        this.fornavn = aktør.fornavn
        this.etternavn = aktør.etternavn
        this.fødtDato = aktør.fødtDato
        this.dødDato = aktør.dødDato
        this.gradering = aktør.gradering
        this.språkkode = aktør.språkkode
        this.adresselinje1 = aktør.adresselinje1
        this.adresselinje2 = aktør.adresselinje2
        this.adresselinje3 = aktør.adresselinje3
        this.leilighetsnummer = aktør.leilighetsnummer
        this.poststed = aktør.poststed
        this.postnr = aktør.postnr
        this.land = aktør.land
        this.dødsbo = aktør.dødsbo
        this.tidligereIdenter.clear()
        this.tidligereIdenter.addAll(aktør.tidligereIdenter)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Aktør

        return when {
            aktørIdent != other.aktørIdent -> false
            aktørType != other.aktørType -> false
            offentligId != other.offentligId -> false
            offentligIdType != other.offentligIdType -> false
            norskKontonr != other.norskKontonr -> false
            iban != other.iban -> false
            swift != other.swift -> false
            bankNavn != other.bankNavn -> false
            bankLandkode != other.bankLandkode -> false
            bankCode != other.bankCode -> false
            valutaKode != other.valutaKode -> false
            fornavn?.lowercase()?.trim() != other.fornavn?.lowercase()?.trim() -> false
            etternavn?.lowercase()?.trim() != other.etternavn?.lowercase()?.trim() -> false
            fødtDato != other.fødtDato -> false
            dødDato != other.dødDato -> false
            gradering != other.gradering -> false
            språkkode != other.språkkode -> false
            adresselinje1 != other.adresselinje1 -> false
            adresselinje2 != other.adresselinje2 -> false
            adresselinje3 != other.adresselinje3 -> false
            leilighetsnummer != other.leilighetsnummer -> false
            postnr != other.postnr -> false
            poststed != other.poststed -> false
            land != other.land -> false
            erDødsboUlikt(other) -> false
            erTidligereIdenterUlike(other) -> false
            else -> true
        }
    }

    private fun erDødsboUlikt(other: Aktør): Boolean = when {
        dødsbo?.kontaktperson != other.dødsbo?.kontaktperson -> true
        dødsbo?.adresselinje1 != other.dødsbo?.adresselinje1 -> true
        dødsbo?.adresselinje2 != other.dødsbo?.adresselinje2 -> true
        dødsbo?.adresselinje3 != other.dødsbo?.adresselinje3 -> true
        dødsbo?.leilighetsnummer != other.dødsbo?.leilighetsnummer -> true
        dødsbo?.postnr != other.dødsbo?.postnr -> true
        dødsbo?.poststed != other.dødsbo?.poststed -> true
        dødsbo?.land != other.dødsbo?.land -> true
        else -> false
    }

    fun erTidligereIdenterUlike(other: Aktør): Boolean = !(
        tidligereIdenter.size == other.tidligereIdenter.size &&
            tidligereIdenter.all { a1 ->
                other.tidligereIdenter.any { a2 -> a1.tidligereAktoerIdent == a2.tidligereAktoerIdent } &&
                    other.tidligereIdenter.all { a2 ->
                        tidligereIdenter.any { a1 -> a2.tidligereAktoerIdent == a1.tidligereAktoerIdent }
                    }
            }
        )

    override fun hashCode(): Int {
        var result = aktørIdent.hashCode()
        result = 31 * result + aktørType.hashCode()
        result = 31 * result + (offentligId?.hashCode() ?: 0)
        result = 31 * result + (offentligIdType?.hashCode() ?: 0)
        result = 31 * result + (norskKontonr?.hashCode() ?: 0)
        result = 31 * result + (iban?.hashCode() ?: 0)
        result = 31 * result + (swift?.hashCode() ?: 0)
        result = 31 * result + (bankNavn?.hashCode() ?: 0)
        result = 31 * result + (bankLandkode?.hashCode() ?: 0)
        result = 31 * result + (bankCode?.hashCode() ?: 0)
        result = 31 * result + (valutaKode?.hashCode() ?: 0)
        result = 31 * result + (fornavn?.hashCode() ?: 0)
        result = 31 * result + (etternavn?.hashCode() ?: 0)
        result = 31 * result + (fødtDato?.hashCode() ?: 0)
        result = 31 * result + (dødDato?.hashCode() ?: 0)
        result = 31 * result + (gradering?.hashCode() ?: 0)
        result = 31 * result + (språkkode?.hashCode() ?: 0)
        result = 31 * result + (adresselinje1?.hashCode() ?: 0)
        result = 31 * result + (adresselinje2?.hashCode() ?: 0)
        result = 31 * result + (adresselinje3?.hashCode() ?: 0)
        result = 31 * result + (leilighetsnummer?.hashCode() ?: 0)
        result = 31 * result + (postnr?.hashCode() ?: 0)
        result = 31 * result + (poststed?.hashCode() ?: 0)
        result = 31 * result + (land?.hashCode() ?: 0)
        result = 31 * result + dødsbo?.id.hashCode()
        return result
    }

    override fun toString(): String = "Aktoer(id=$id, " +
        "aktoerIdent='$aktørIdent', " +
        "aktoerType='$aktørType', " +
        "offentligId=$offentligId, " +
        "offentligIdType=$offentligIdType, " +
        "norskKontonr=$norskKontonr, " +
        "iban=$iban, " +
        "swift=$swift, " +
        "bankNavn=$bankNavn, " +
        "bankLandkode=$bankLandkode, " +
        "bankCode=$bankCode, " +
        "valutaKode=$valutaKode, " +
        "fornavn=$fornavn, " +
        "etternavn=$etternavn, " +
        "fodtDato=$fødtDato, " +
        "dodDato=$dødDato, " +
        "gradering=$gradering, " +
        "sprakkode=$språkkode, " +
        "adresselinje1=$adresselinje1, " +
        "adresselinje2=$adresselinje2, " +
        "adresselinje3=$adresselinje3, " +
        "leilighetsnummer=$leilighetsnummer, " +
        "postnr=$postnr, " +
        "poststed=$poststed, land=$land, " +
        "tidligereIdenter=$tidligereIdenter, " +
        "dodsbo=${dødsbo?.id}, " +
        "sistEndret=$sistEndret)"
}
