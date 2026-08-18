package no.nav.bidrag.regnskap.persistence.repository

import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface DriftsavvikRepository : JpaRepository<Driftsavvik, Int> {

    fun findByPåløpId(påløpId: Int): Driftsavvik?

    @Query(
        value = "SELECT * FROM driftsavvik d WHERE tidspunkt_fra < :currentDateTime AND (tidspunkt_til > :currentDateTime OR tidspunkt_til IS NULL)",
        nativeQuery = true,
    )
    fun hentAktiveDriftsavvik(currentDateTime: LocalDateTime = LocalDateTime.now()): List<Driftsavvik>
}
