package no.nav.bidrag.dokument.bestilling.tjenester

import no.nav.bidrag.dokument.bestilling.SIKKER_LOGG
import no.nav.bidrag.dokument.bestilling.consumer.BidragPersonConsumer
import no.nav.bidrag.dokument.bestilling.model.FantIkkePersonException
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val bidragPersonConsumer: BidragPersonConsumer,
) {
    fun hentPerson(
        personId: String,
        rolle: String? = "UKJENT",
    ): PersonDto =
        bidragPersonConsumer.hentPerson(personId) ?: run {
            SIKKER_LOGG.warn("Fant ikke person med fnr $personId og rolle $rolle")
            throw FantIkkePersonException("Fant ikke person med rolle $rolle")
        }

    fun hentPersonAdresse(
        personId: String,
        rolle: String? = "UKJENT",
    ): PersonAdresseDto? =
        bidragPersonConsumer.hentAdresse(personId) ?: run {
            SIKKER_LOGG.warn("Fant ikke adresse for person $personId med rolle $rolle")
            null
        }

    fun hentSpråk(personId: String): String = bidragPersonConsumer.hentSpraak(personId)?.uppercase() ?: "NB"
}
