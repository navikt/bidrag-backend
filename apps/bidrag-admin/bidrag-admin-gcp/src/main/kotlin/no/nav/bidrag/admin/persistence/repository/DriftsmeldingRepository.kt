package no.nav.bidrag.admin.persistence.repository

import no.nav.bidrag.admin.persistence.entity.Driftsmelding
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface DriftsmeldingRepository : CrudRepository<Driftsmelding, Long> {
    @Query(
        "select e from driftsmelding e where " +
            "e.aktivFraTidspunkt is not null and e.aktivFraTidspunkt <= current_date " +
            "and e.aktivTilTidspunkt is null or e.aktivTilTidspunkt > current_date",
    )
    fun hentAlleAktiv(): List<Driftsmelding>
}
