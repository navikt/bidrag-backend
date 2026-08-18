package no.nav.bidrag.aktoerregister.persistence.repository

import no.nav.bidrag.aktoerregister.persistence.entities.TidligereIdenter
import org.springframework.data.jpa.repository.JpaRepository

interface TidligereIdenterRepository : JpaRepository<TidligereIdenter, Int> {

    fun findByTidligereAktoerIdent(tidligereAktoerIdent: String): List<TidligereIdenter>
}
