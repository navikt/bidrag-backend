package no.nav.bidrag.sak.repository

import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.VedtakOverføring
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VedtakOverføringRepository : JpaRepository<VedtakOverføring, Long> {
    @Query(
        """
            select count(vo) from T_VEDTAK_OVERFORING vo
            where vo.saksnr = :saksnummer
                and vo.soknadId = :soknadId
                and vo.vedtakIdBidragVedtak is not null
                and vo.vedtakOverfortMedGrunnlagTimestamp is not null
        """,
    )
    fun countVedtakGrunnlagOverførtForSak(
        saksnummer: Saksnummer,
        soknadId: Int,
    ): Int

    @Query(
        """
            select vo.vedtakIdBidragVedtak from T_VEDTAK_OVERFORING vo
            where vo.saksnr = :saksnummer
                and vo.soknadId = :soknadId
                and vo.vedtakIdBidragVedtak is not null
        """,
    )
    fun finnVedtakIdBidragVedtakForSak(
        saksnummer: Saksnummer,
        soknadId: Int,
    ): List<Int>
}
