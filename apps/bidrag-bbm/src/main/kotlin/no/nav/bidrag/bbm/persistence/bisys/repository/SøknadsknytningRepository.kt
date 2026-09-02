package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadsknytning
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SøknadsknytningRepository : CrudRepository<Søknadsknytning, String> {
    @Query(
        "select s from Søknadsknytning s where s.hovedsøknadsid = :hovedsøknadsid  and s.status in :statuser ",
    )
    fun finnSøknadsknytningerHovedsøknad(
        hovedsøknadsid: Long,
        statuser: List<String>,
    ): List<Søknadsknytning>

    @Query(
        "select s from Søknadsknytning s where s.referertSøknadsid = :referertSøknadsid and s.status in :statuser  ",
    )
    fun finnSøknadsknytningReferertSøknad(
        referertSøknadsid: Long,
        statuser: List<String>,
    ): List<Søknadsknytning>

    @Query(
        "select s from Søknadsknytning s where s.hovedsøknadsid = :hovedsøknadsid and s.referertSøknadsid = :referertSøknadsid  and s.status = :status",
    )
    fun finnSøknadsknytning(
        hovedsøknadsid: Long,
        referertSøknadsid: Long,
        status: String,
    ): Søknadsknytning?
}
