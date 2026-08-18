package no.nav.bidrag.admin.produksjonsoppfølging.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "t_vedtak_overforing")
class VedtakOverføring {
    @Id
    @Column
    var id = 0

    @Column
    var saksnr: String? = null

    @Column
    var status: String? = null

    @Column
    var notat: String? = null
}
