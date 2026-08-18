package no.nav.bidrag.aktoerregister.persistence.repository

import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.sql.Timestamp

interface AktørRepository : JpaRepository<Aktør, String> {

    // Brukes av batchReader
    @Suppress("unused")
    fun findAllByAktørType(aktørType: String, pageable: Pageable): Page<Aktør>

    // Brukes av batchReader
    @Suppress("unused")
    fun findAllByAktørTypeAndSistEndretIsLessThan(aktørType: String, sistEndret: Timestamp, pageable: Pageable): Page<Aktør>

    fun findByAktørIdent(aktørIdent: String): Aktør?

    fun deleteAktørByAktørIdent(aktørIdent: String)

    @Query(
        value = """
            WITH alle_identer AS (
                SELECT id AS aktoer_id, aktoer_ident AS ident
                FROM aktoer
                UNION
                SELECT aktoer_id, tidligere_aktoer_ident AS ident
                FROM tidligere_identer
            )
            SELECT ident
            FROM alle_identer
            GROUP BY ident
            HAVING COUNT(DISTINCT aktoer_id) > 1
        """,
        nativeQuery = true,
    )
    fun finnDuplikateIdenter(): List<String>
}
