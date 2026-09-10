package no.nav.bidrag.henvendelse.service

import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.aop.IngenTilgangException
import org.springframework.stereotype.Component

/**
 * Tilgangen avgjøres av bidrag-tilgangskontroll, som spør tilgangsmaskinen. Kallet går
 * on-behalf-of saksbehandleren, så svaret gjelder henne og ikke appen.
 *
 * Sjekken må ligge her og ikke lenger ned i kjeden: `/henvendelseinfo/henvendelseliste` i
 * navikt/crm-henvendelse er deklarert `without sharing`, og sf-henvendelse-api-proxy
 * kontrollerer bare at tokenet er gyldig. Ingen ledd etter oss ser på om saksbehandleren har
 * tilgang til personen, så et oppslag uten denne sjekken utleverer henvendelser om hvem som
 * helst - også skjermede personer.
 */
@Component
class Tilgangskontroll(
    private val tilgangClient: TilgangClient,
) {
    fun sjekkTilgangTilPerson(personident: Personident) {
        if (!tilgangClient.harTilgangPerson(personident)) throw IngenTilgangException()
    }
}
