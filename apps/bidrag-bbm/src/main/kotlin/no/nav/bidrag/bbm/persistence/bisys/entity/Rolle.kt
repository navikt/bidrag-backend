package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.bidrag.domene.enums.rolle.Rolletype

@Entity
@Table(name = "T_ROLLE")
@Suppress("unused")
open class Rolle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLLE_ID")
    open var rolleid: Long? = null,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "FNR")
    open var fnr: String? = null,
    @Column(name = "ROLLE_TYPE")
    open var rolletype: String,
    @Column(name = "MOTTAGER_ER_VERGE")
    open var mottagerErVerge: Char = '0',
) {
    fun tilRolletype() = when (rolletype) {
        "BM" -> Rolletype.BIDRAGSMOTTAKER
        "BP" -> Rolletype.BIDRAGSPLIKTIG
        "BA" -> Rolletype.BARN
        "RM" -> Rolletype.REELMOTTAKER
        else -> Rolletype.FEILREGISTRERT
    }
}
