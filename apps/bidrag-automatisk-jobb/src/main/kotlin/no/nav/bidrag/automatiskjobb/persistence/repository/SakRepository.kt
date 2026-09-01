package no.nav.bidrag.automatiskjobb.persistence.repository

import no.nav.bidrag.automatiskjobb.persistence.entity.Sak
import org.springframework.data.jpa.repository.JpaRepository

interface SakRepository : JpaRepository<Sak, Int> {
    fun findBySaksnummer(saksnummer: String): Sak?
}
