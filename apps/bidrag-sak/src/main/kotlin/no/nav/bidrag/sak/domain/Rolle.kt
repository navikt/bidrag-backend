package no.nav.bidrag.sak.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
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
import jakarta.persistence.OneToOne
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.UkjentPart
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate
import java.util.Objects

@Entity(name = "T_ROLLE")
open class Rolle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLLE_ID", columnDefinition = "INTEGER")
    open var rolleId: Int = 0,
    @Column(name = "FNR", columnDefinition = "CHAR(11)")
    open var fødselsnummer: String? = null,
    @Column(name = "ROLLE_TYPE")
    @Convert(converter = RolletypeConverter::class)
    open var rolleType: Rolletype,
    @Column(name = "OBJEKTNR", columnDefinition = "CHAR(2)")
    open var objektnummer: String? = null,
    @Column(name = "RM_ROLLE_ID")
    open var rmRolleId: Int? = null,
    @Column(name = "UKJ_PART_KODE")
    @Enumerated(EnumType.STRING)
    open var ukjentPart: UkjentPart? = null,
    @Column(name = "MOTTAGER_ER_VERGE")
    @Convert(converter = BooleanConverter::class)
    open var mottagerErVerge: Boolean = false,
    @Column(name = "SH_IDENT")
    open var samhandlerIdent: String? = null,
    @Column(name = "OPPRETTET_DATO")
    open var opprettetDato: LocalDate = LocalDate.now(),
    @Column(name = "FODT_DATO")
    open var fødselsdato: LocalDate? = null,
    @OneToMany(mappedBy = "rolle", cascade = [CascadeType.ALL])
    open var rollehistorikk: MutableSet<Rollehistorikk> = mutableSetOf(),
    @ManyToOne(targetEntity = Bidragssak::class, fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "SAKSNR", nullable = false)
    open var bidragssak: Bidragssak? = null,
) {
    fun erPerson(): Boolean = rolleType != Rolletype.REELMOTTAKER &&
        harIkkeSamhandlerIdent() &&
        erIkkeFeilregistrert() &&
        harFødselsnummerPaElleveSiffer()

    private fun harIkkeSamhandlerIdent(): Boolean = samhandlerIdent.isNullOrBlank()

    private fun erIkkeFeilregistrert(): Boolean = Rolletype.FEILREGISTRERT != rolleType

    private fun harFødselsnummerPaElleveSiffer(): Boolean = fødselsnummer?.let { Personident(it).gyldig() } ?: false

    override fun hashCode(): Int = Objects.hash(
        rolleId,
        fødselsnummer,
        rolleType,
        objektnummer,
        rmRolleId,
        ukjentPart,
        mottagerErVerge,
        samhandlerIdent,
        opprettetDato,
        fødselsdato,
        bidragssak?.saksnummer,
    )

    override fun toString(): String = "Rolle(rolleId=$rolleId, fødselsnummer=$fødselsnummer, rolleType=$rolleType, " +
        "objektnummer=$objektnummer, rmRolleId=$rmRolleId, ukjentPart=$ukjentPart, " +
        "mottagerErVerge=$mottagerErVerge," +
        " samhandlerIdent=$samhandlerIdent, opprettetDato=$opprettetDato, fødselsdato=$fødselsdato, " +
        "rollehistorikk=$rollehistorikk, bidragssak=${bidragssak?.saksnummer})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rolle

        if (rolleId != other.rolleId) return false
        if (fødselsnummer != other.fødselsnummer) return false
        if (rolleType != other.rolleType) return false
        if (objektnummer != other.objektnummer) return false
        if (rmRolleId != other.rmRolleId) return false
        if (ukjentPart != other.ukjentPart) return false
        if (mottagerErVerge != other.mottagerErVerge) return false
        if (samhandlerIdent != other.samhandlerIdent) return false
        if (opprettetDato != other.opprettetDato) return false
        if (fødselsdato != other.fødselsdato) return false
        if (rollehistorikk != other.rollehistorikk) return false
        if (bidragssak?.saksnummer != other.bidragssak?.saksnummer) return false

        return true
    }
}
