package no.nav.bidrag.bbm.persistence.bisys.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "T_KODE_SOKN_STAT")
@Suppress("unused")
open class KodeSøknadStatus(
    @Id
    @Column(name = "KODE")
    open var kode: String,
    @Column(name = "LUKKET_STATUS")
    open var lukketStatus: String,
)
