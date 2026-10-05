package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.Sak
import org.springframework.data.repository.CrudRepository

interface SakRepository : CrudRepository<Sak, Long> {
    fun findBySaksnummer(saksnummer: String): Sak?
}
