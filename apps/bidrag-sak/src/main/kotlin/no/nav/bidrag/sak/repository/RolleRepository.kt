package no.nav.bidrag.sak.repository

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.domain.projections.RollePipInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RolleRepository : JpaRepository<Rolle, String> {
    @Query(
        """SELECT rolle.bidragssak.saksnummer
        FROM T_ROLLE rolle
        WHERE rolle.samhandlerIdent = :samhandlerId
        and EXISTS (SELECT r2 FROM T_ROLLE r2 WHERE r2.rmRolleId = rolle.rolleId and r2.bidragssak.saksnummer = rolle.bidragssak.saksnummer)
        AND rolle.rolleType <> 'FR' group by rolle.bidragssak""",
    )
    fun samhandlereForSak(samhandlerId: String): List<String>

    @Query(
        """
            SELECT DISTINCT new no.nav.bidrag.sak.domain.projections.RollePipInfo(r.rolleType, r.samhandlerIdent, r.fødselsnummer)
            FROM T_ROLLE r 
            WHERE r.bidragssak.saksnummer = :saksnummer
        """,
    )
    fun findPipInfoBySaksnummer(saksnummer: String): List<RollePipInfo>

    @Query("SELECT r FROM T_ROLLE r WHERE r.bidragssak.saksnummer = :saksnummer AND r.rolleType = :rolleType")
    fun findByBySaksnummerAndRolleType(
        @Param("saksnummer") saksnummer: String,
        @Param("rolleType") rolleType: Rolletype,
    ): List<Rolle>
}
