package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.VedtakOverføring
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface VedtakOverføringRepository : CrudRepository<VedtakOverføring, String> {
    @Query(
        "select b from VedtakOverføring b where b.soknadId = :søknadsid  ",
    )
    fun finnForSøknadsid(søknadsid: Long): VedtakOverføring?
}
