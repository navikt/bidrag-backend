package no.nav.bidrag.admin.produksjonsoppfølging.repository

import no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolle
import org.springframework.data.repository.CrudRepository

interface RolleRepository : CrudRepository<Rolle, Long> {
    fun findBySaksnr(saksnr: String): List<Rolle>
}
