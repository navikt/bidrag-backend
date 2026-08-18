package no.nav.bidrag.admin.produksjonsoppfølging.repository

import no.nav.bidrag.admin.produksjonsoppfølging.domene.Søknadslinje
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SøknadslinjeRepository : CrudRepository<Søknadslinje, Long> {
    fun findBySaksnr(saksnr: String?): MutableList<Søknadslinje>
}
