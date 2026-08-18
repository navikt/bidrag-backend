package no.nav.bidrag.sak.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity(name = "T_BLANKETT")
class Blankett(
    @Id
    @Column(name = "blankett_id", nullable = false)
    val blankettId: Int = 0,
    @Column(name = "sokn_fra_kode", nullable = true)
    val soknFraKode: String? = null,
    @Column(name = "sokn_type", nullable = true)
    val soknType: String? = null,
)
