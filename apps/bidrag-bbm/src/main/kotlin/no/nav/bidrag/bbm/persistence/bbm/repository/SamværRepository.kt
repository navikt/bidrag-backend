package no.nav.bidrag.bbm.persistence.bbm.repository

import no.nav.bidrag.bbm.persistence.bbm.entity.Samvær
import no.nav.bidrag.bbm.persistence.bbm.entity.SamværId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface SamværRepository : CrudRepository<Samvær, SamværId> {
    @Query(
        "select sm from Samvær sm where sm.saksnummer = :saksnummer and sm.personidentBarn = :personidentBarn " +
            "and sm.datoSøknad = :datoSøknad and sm.soknadstype in :søknadstyper order by sm.datoFom desc fetch first 1 row only",
    )
    fun finnSisteSamvær(
        saksnummer: String,
        personidentBarn: String,
        datoSøknad: LocalDate,
        søknadstyper: List<String>,
    ): Samvær?

    @Query(
        "select sm from Samvær sm where sm.saksnummer = :saksnummer " +
            "and sm.soknadstype in ('BB', 'OB', '18') order by sm.datoFom asc",
    )
    fun finnAlleSamværForSaksnummer(saksnummer: String): List<Samvær>

    @Query(
        "select sm from Samvær sm where sm.saksnummer = :saksnummer and sm.personidentBarn = :personidentBarn " +
            "and sm.datoSøknad = :datoSøknad and sm.soknadstype in :søknadstyper order by sm.datoFom",
    )
    fun finnAlleSamvær(
        saksnummer: String,
        personidentBarn: String,
        datoSøknad: LocalDate,
        søknadstyper: List<String>,
    ): List<Samvær>
}
