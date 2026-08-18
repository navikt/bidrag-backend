package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.Blankett
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface BlankettRepository : CrudRepository<Blankett, String> {
    @Query(
        "select distinct b.søknadFraKode from Blankett b where b.blankettid = :blankettid  ",
    )
    fun finnSøknadFraKode(blankettid: Long): String

    @Query(
        "select distinct b.søknadstype from Blankett b where b.blankettid = :blankettid  ",
    )
    fun finnSøknadstype(blankettid: Long): String
}
