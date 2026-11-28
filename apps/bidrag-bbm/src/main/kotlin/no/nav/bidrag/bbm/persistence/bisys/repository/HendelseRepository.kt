package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.Hendelse
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface HendelseRepository : CrudRepository<Hendelse, String> {
    @Query(
        "select h from Hendelse h where h.søknadsid = :søknadsid ",
    )
    fun finnHendelserForSøknad(søknadsid: Long): List<Hendelse>

    @Query(
        "select h from Hendelse h where h.søknadsid in :søknadsider and h.hendelsestype in :hendelsestyper",
    )
    fun finnHendelserForSøknader(
        søknadsider: Collection<Long>,
        hendelsestyper: Collection<String>,
    ): List<Hendelse>
}
