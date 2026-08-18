package no.nav.bidrag.sak.repository

import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.projections.BidragssakPipInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BidragssakRepository : JpaRepository<Bidragssak, String> {
    fun findBySaksnummer(saksnummer: String): Bidragssak?

    @Query(
        """SELECT sak
        FROM T_BIDRAG_SAK sak
        JOIN sak.roller rolle
        WHERE rolle.fødselsnummer in :foedselsnummer
        AND rolle.rolleType <> 'FR'""",
    )
    fun findByRoller(foedselsnummer: List<String>): List<Bidragssak>

    @Query("SELECT MAX(CAST(sak.saksnummer AS INTEGER)) FROM T_BIDRAG_SAK sak WHERE CAST(sak.saksnummer AS INTEGER) < :maxGrense")
    fun hentMaxLoepenummerSomIkkeOverskrider(maxGrense: Int): Int?

    @Query(
        """
        SELECT new no.nav.bidrag.sak.domain.projections.BidragssakPipInfo(s.avsluttetTidspunkt) 
        FROM T_BIDRAG_SAK s 
        WHERE s.saksnummer = :saksnummer
        """,
    )
    fun findBidragssakPipInfoBySaksnummer(saksnummer: String): BidragssakPipInfo?
}
