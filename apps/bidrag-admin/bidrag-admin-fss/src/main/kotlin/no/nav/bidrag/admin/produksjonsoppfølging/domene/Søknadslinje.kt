package no.nav.bidrag.admin.produksjonsoppfølging.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "t_soknad_linje")
class Søknadslinje {
    @Id
    @Column(name = "sokn_linje_id")
    val id: Long = 0

    @Column(name = "saksnr")
    val saksnr: String? = null

    @Column(name = "rolle_id")
    val rolle: Long = 0
}
