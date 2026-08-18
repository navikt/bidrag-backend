package no.nav.bidrag.sak.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import java.time.LocalDateTime
import java.util.Objects

@Entity(name = "T_ROLLE_HISTORIKK")
open class Rollehistorikk(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Int = 0,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "ROLLE_FNR", columnDefinition = "CHAR(11)")
    open var rolleFødselsnummer: String? = null,
    @Column(name = "ROLLE_TYPE")
    @Convert(converter = RolletypeConverter::class)
    open var type: Rolletype? = null,
    @Column(name = "RM_ROLLE_ID")
    open var rmRolleId: Int? = null,
    @Column(name = "RM_ROLLE_FNR", columnDefinition = "CHAR(11)")
    open var rmRolleFødselsnummer: String? = null,
    @Column(name = "TYPE")
    @Convert(converter = TypeEndringConverter::class)
    open var typeEndring: TypeEndring? = null,
    @Column(name = "OPPRETTET_AV")
    open var opprettetAv: String,
    @Column(name = "OPPRETTET_DATO")
    open var opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    @ManyToOne(targetEntity = Rolle::class, fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "ROLLE_ID", nullable = false)
    open var rolle: Rolle? = null,
) {
    override fun hashCode(): Int = Objects.hash(
        id,
        rolleFødselsnummer,
        type,
        rmRolleId,
        rmRolleFødselsnummer,
        typeEndring,
        opprettetAv,
        opprettetTidspunkt,
        rolle?.rolleId,
    )

    override fun toString(): String = "Rollehistorikk(id=$id, rolleFødselsnummer=$rolleFødselsnummer, rolleType=$type, " +
        "rmRolleId=$rmRolleId, rmRolleFødselsnummer=$rmRolleFødselsnummer, " +
        "typeEndring=$typeEndring, opprettetAv=$opprettetAv, opprettetTidspunkt=$opprettetTidspunkt, rolleid=${rolle?.rolleId})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rollehistorikk

        if (id != other.id) return false
        if (rolleFødselsnummer != other.rolleFødselsnummer) return false
        if (type != other.type) return false
        if (rmRolleId != other.rmRolleId) return false
        if (rmRolleFødselsnummer != other.rmRolleFødselsnummer) return false
        if (typeEndring != other.typeEndring) return false
        if (opprettetAv != other.opprettetAv) return false
        if (opprettetTidspunkt != other.opprettetTidspunkt) return false
        if (rolle?.rolleId != other.rolle?.rolleId) return false

        return true
    }
}
