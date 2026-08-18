package no.nav.bidrag.admin.persistence.repository

import no.nav.bidrag.admin.persistence.entity.Person
import org.springframework.data.repository.CrudRepository

interface Personrepository : CrudRepository<Person, Long> {
    fun findByNavIdent(navIdent: String): Person?

    fun enhet(enhet: String): MutableList<Person>
}
