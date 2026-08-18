package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadslinje
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SøknadslinjeRepository : CrudRepository<Søknadslinje, String> {
    @Query(
        "select s from Søknadslinje s where s.søknadsid = :søknadsid ",
    )
    fun finnSøknadslinjerForSøknad(søknadsid: Long): List<Søknadslinje>
}
