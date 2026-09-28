package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EndreMottakerRepository : JpaRepository<EndreMottaker, Long> {
    @Query(
        value = """
            SELECT kandidat.*
            FROM endre_mottaker kandidat
            WHERE kandidat.godkjent_av_skatt_tidspunkt IS NULL
              AND NOT EXISTS (
                SELECT 1
                FROM endre_mottaker eldre
                WHERE eldre.saksnummer = kandidat.saksnummer
                  AND eldre.godkjent_av_skatt_tidspunkt IS NULL
                  AND (
                    eldre.opprettet_tidspunkt < kandidat.opprettet_tidspunkt
                    OR (
                        eldre.opprettet_tidspunkt = kandidat.opprettet_tidspunkt
                        AND eldre.id < kandidat.id
                    )
                  )
              )
            ORDER BY kandidat.saksnummer, kandidat.opprettet_tidspunkt, kandidat.id
        """,
        nativeQuery = true,
    )
    fun hentEldsteIkkeGodkjentePerSak(): List<EndreMottaker>

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM endre_mottaker eldre
                WHERE eldre.saksnummer = :saksnummer
                  AND eldre.godkjent_av_skatt_tidspunkt IS NULL
                  AND (
                    eldre.opprettet_tidspunkt < :opprettetTidspunkt
                    OR (
                        eldre.opprettet_tidspunkt = :opprettetTidspunkt
                        AND eldre.id < :id
                    )
                  )
            )
        """,
        nativeQuery = true,
    )
    fun finnesEldreIkkeGodkjentForSak(
        @Param("saksnummer") saksnummer: String,
        @Param("opprettetTidspunkt") opprettetTidspunkt: LocalDateTime,
        @Param("id") id: Long,
    ): Boolean
}
