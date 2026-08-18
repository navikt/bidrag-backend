package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.Søknadsknytning
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SøknadsknytningRepository : CrudRepository<Søknadsknytning, String> {
    @Query(
        "select s from Søknadsknytning s where s.hovedsøknadsid = :hovedsøknadsid  and s.status = :status ",
    )
    fun finnSøknadsknytningerHovedsøknad(
        hovedsøknadsid: Long,
        status: String,
    ): List<Søknadsknytning>

    @Query(
        "select s from Søknadsknytning s where s.referertSøknadsid = :referertSøknadsid and s.status = :status  ",
    )
    fun finnSøknadsknytningReferertSøknad(
        referertSøknadsid: Long,
        status: String,
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
