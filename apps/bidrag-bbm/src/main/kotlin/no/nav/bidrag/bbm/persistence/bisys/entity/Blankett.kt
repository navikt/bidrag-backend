package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "T_BLANKETT")
@Suppress("unused")
open class Blankett(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BLANKETT_ID")
    open var blankettid: Long? = null,
    @Column(name = "SAKSNR")
    open var saksnummer: String,
    @Column(name = "SOKN_FRA_KODE")
    open var søknadFraKode: String,
    @Column(name = "SOKN_TYPE")
    open var søknadstype: String,
)
