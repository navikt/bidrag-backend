package no.nav.bidrag.bbm.persistence.bisys.repository

import no.nav.bidrag.bbm.persistence.bisys.entity.Rolle
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface RolleRepository : CrudRepository<Rolle, String> {
    @Query(
        "select distinct r.saksnummer from Rolle r where r.fnr = :personident and r.rolletype = 'BP'  ",
    )
    fun finnBPsSaker(personident: String): List<String>

    @Query(
        "select r from Rolle r where r.saksnummer = :saksnummer and r.rolletype in ('BP', 'BM', 'RM')  ",
    )
    fun finnBmBpRmISak(saksnummer: String): List<Rolle>

    @Query(
        "select r from Rolle r where r.saksnummer = :saksnummer and r.fnr = :personident and r.rolletype in ('BA', 'RM')  ",
    )
    fun finnBaRmISak(
        saksnummer: String,
        personident: String,
    ): List<Rolle>

    @Query(
        "select r from Rolle r where r.saksnummer = :saksnummer ",
    )
    fun finnRollerISak(saksnummer: String): List<Rolle>
}
