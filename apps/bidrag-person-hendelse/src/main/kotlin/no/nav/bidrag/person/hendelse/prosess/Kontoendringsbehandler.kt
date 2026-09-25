package no.nav.bidrag.person.hendelse.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.person.BidragPersonklient
import no.nav.bidrag.person.hendelse.integrasjon.bidrag.topic.BidragKafkaMeldingsprodusent
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.hendelse.Endringsmelding
import org.springframework.stereotype.Service

@Service
class Kontoendringsbehandler(
    val bidragPersonklient: BidragPersonklient,
    val bidragtopic: BidragKafkaMeldingsprodusent,
) {
    fun publisere(personidentKontoeier: String) {
        val alleIdenterKontoeier = bidragPersonklient.henteAlleIdenterForPerson(personidentKontoeier)
        val aktørid = alleIdenterKontoeier?.find { it.gruppe.equals(Identgruppe.AKTORID) }?.ident
        if (aktørid != null) {
            bidragtopic.publisereEndringsmelding(
                aktørid,
                alleIdenterKontoeier
                    .map {
                        it.ident
                    }.toSet(),
                opplysningstype = Endringsmelding.Opplysningstype.KONTOENDRING,
            )
        } else {
            secureLogger.warn { "Aktørid null for kontoeier $personidentKontoeier - kontoendring ble ikke publisert" }
        }
    }

    companion object {
        val log = KotlinLogging.logger {}
    }
}
