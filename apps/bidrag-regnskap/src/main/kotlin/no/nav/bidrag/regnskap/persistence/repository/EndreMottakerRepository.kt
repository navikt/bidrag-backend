package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.EndreMottaker
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EndreMottakerRepository : JpaRepository<EndreMottaker, Long> {
    @Query(
        value = """
            SELECT * FROM (
                SELECT DISTINCT ON (saksnummer, barn_ident) *
                FROM endre_mottaker
                ORDER BY saksnummer, barn_ident, opprettet_tidspunkt DESC, id DESC
            ) siste
            WHERE siste.godkjent_av_skatt_tidspunkt IS NULL
        """,
        nativeQuery = true,
    )
    fun hentNyesteIkkeGodkjentePerSakOgBarn(): List<EndreMottaker>
}
