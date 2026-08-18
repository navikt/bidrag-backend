package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.vedtakoverføring

import no.nav.bidrag.admin.produksjonsoppfølging.domene.VedtakOverføring
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakoverføringRepository : JpaRepository<VedtakOverføring, Int> {
    @Query("FROM VedtakOverføring WHERE status = 'FEILET'")
    fun finnOverføringerMedStatusFeilet(): List<VedtakOverføring?>?
}
