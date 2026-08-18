package no.nav.bidrag.admin.persistence.repository

import no.nav.bidrag.admin.persistence.entity.DriftsmeldingHistorikk
import no.nav.bidrag.admin.persistence.entity.Endringslogg
import no.nav.bidrag.admin.persistence.entity.EndringsloggEndring
import no.nav.bidrag.admin.persistence.entity.LestAvBruker
import no.nav.bidrag.admin.persistence.entity.Person
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface LestAvBrukerRepository : CrudRepository<LestAvBruker, Long> {
    fun findByPersonAndEndringsloggEndring(
        person: Person,
        endring: EndringsloggEndring,
    ): LestAvBruker?

    fun findByPersonAndEndringsloggAndEndringsloggEndringIsNull(
        person: Person,
        endring: Endringslogg,
    ): LestAvBruker?

    fun findByPersonAndDriftsmeldingHistorikk(
        person: Person,
        driftsmelding: DriftsmeldingHistorikk,
    ): LestAvBruker?

    @Query(
        "select sum(l.lestetidVarighetMs) " +
            "from lest_av_bruker l " +
            "where l.endringslogg = :endringslogg and l.person.id = :personId",
    )
    fun sumLesetidVarighetMsByEndringslogg(
        @Param("endringslogg") endringslogg: Endringslogg,
        @Param("personId") personId: Long,
    ): Long?
}
