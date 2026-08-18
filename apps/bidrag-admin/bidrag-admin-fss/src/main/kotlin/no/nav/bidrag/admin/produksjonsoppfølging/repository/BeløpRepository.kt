package no.nav.bidrag.admin.produksjonsoppfølging.repository

import no.nav.bidrag.admin.produksjonsoppfølging.domene.Beløp
import org.springframework.data.repository.CrudRepository

interface BeløpRepository : CrudRepository<Beløp, Long> {
    fun findBySaksnr(saksnr: String): List<Beløp>
}
