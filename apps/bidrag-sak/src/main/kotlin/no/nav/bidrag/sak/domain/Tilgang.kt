package no.nav.bidrag.sak.domain

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
import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import java.time.LocalDate
import java.util.Objects

@Entity(name = "T_TILGANG_TK")
open class Tilgang(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var tilgangId: Int = 0,
    @Column(name = "TKNR", columnDefinition = "CHAR(4)")
    open var enhetsnummer: String,
    open var tilgangFomDato: LocalDate = LocalDate.now(),
    open var tilgangTomDato: LocalDate? = null,
    open var opprettetAv: String? = null,
    @Column(name = "FOGD_AARSAK_KODE") @Enumerated(EnumType.STRING)
    open var årsak: Fogdårsak = Fogdårsak.EIER,
    @Column(name = "TILG_TYPE") @Enumerated(EnumType.STRING)
    open var type: Tilgangstype = Tilgangstype.EIER,
    @ManyToOne(
        targetEntity = Bidragssak::class,
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
    ) @JoinColumn(name = "SAKSNR", nullable = false)
    open var bidragssak: Bidragssak? = null,
) {
    override fun hashCode(): Int = Objects.hash(
        tilgangId,
        enhetsnummer,
        tilgangFomDato,
        tilgangTomDato,
        årsak,
        type,
        bidragssak?.saksnummer,
    )

    override fun toString(): String = "Tilgang(tilgangId=$tilgangId, enhetsnummer='$enhetsnummer', " +
        "tilgangFomDato=$tilgangFomDato, tilgangTomDato=$tilgangTomDato, " +
        "årsak=$årsak, type=$type, bidragssak=${bidragssak?.saksnummer}, opprettetAv=$opprettetAv)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tilgang

        if (tilgangId != other.tilgangId) return false
        if (enhetsnummer != other.enhetsnummer) return false
        if (tilgangFomDato != other.tilgangFomDato) return false
        if (tilgangTomDato != other.tilgangTomDato) return false
        if (årsak != other.årsak) return false
        if (type != other.type) return false
        if (bidragssak?.saksnummer != other.bidragssak?.saksnummer) return false

        return true
    }
}
