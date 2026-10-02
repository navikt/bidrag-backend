package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EndreMottakerRepository : JpaRepository<EndreMottaker, Long> {
    @Query(
        """
            SELECT kandidat
            FROM endre_mottaker kandidat
            WHERE kandidat.godkjentAvSkattTidspunkt IS NULL
              AND NOT EXISTS (
                SELECT 1
                FROM endre_mottaker eldre
                WHERE eldre.saksnummer = kandidat.saksnummer
                  AND eldre.godkjentAvSkattTidspunkt IS NULL
                  AND (
                    eldre.opprettetTidspunkt < kandidat.opprettetTidspunkt
                    OR (
                        eldre.opprettetTidspunkt = kandidat.opprettetTidspunkt
                        AND eldre.id < kandidat.id
                    )
                  )
              )
            ORDER BY kandidat.saksnummer, kandidat.opprettetTidspunkt, kandidat.id
        """,
    )
    fun hentEldsteIkkeGodkjentePerSak(): List<EndreMottaker>

    @Query(
        """
            SELECT CASE WHEN COUNT(eldre) > 0 THEN true ELSE false END
            FROM endre_mottaker eldre
            WHERE eldre.saksnummer = :saksnummer
              AND eldre.godkjentAvSkattTidspunkt IS NULL
              AND (
                eldre.opprettetTidspunkt < :opprettetTidspunkt
                OR (
                    eldre.opprettetTidspunkt = :opprettetTidspunkt
                    AND eldre.id < :id
                )
            )
        """,
    )
    fun finnesEldreIkkeGodkjentForSak(
        @Param("saksnummer") saksnummer: String,
        @Param("opprettetTidspunkt") opprettetTidspunkt: LocalDateTime,
        @Param("id") id: Long,
    ): Boolean
}
