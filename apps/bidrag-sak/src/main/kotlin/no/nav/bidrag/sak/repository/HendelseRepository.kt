package no.nav.bidrag.sak.repository

import no.nav.bidrag.sak.domain.Hendelse
import org.springframework.data.jpa.repository.JpaRepository

interface HendelseRepository : JpaRepository<Hendelse, Int> {
    fun findBySaksnummer(saksnummer: String): List<Hendelse>
}
