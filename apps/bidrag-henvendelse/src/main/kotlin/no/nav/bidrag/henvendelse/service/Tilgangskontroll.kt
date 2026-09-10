package no.nav.bidrag.henvendelse.service

import no.nav.bidrag.commons.logging.audit.AuditLogger
import no.nav.bidrag.commons.logging.audit.AuditLoggerEvent
import no.nav.bidrag.commons.tilgang.TilgangClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.aop.IngenTilgangException
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

/**
 * Tilgangen avgjøres av bidrag-tilgangskontroll, som spør tilgangsmaskinen. Kallet går
 * on-behalf-of saksbehandleren, så svaret gjelder henne og ikke appen.
 *
 * Sjekken må ligge her og ikke lenger ned i kjeden: `/henvendelseinfo/henvendelseliste` i
 * navikt/crm-henvendelse er deklarert `without sharing`, og sf-henvendelse-api-proxy
 * kontrollerer bare at tokenet er gyldig. Ingen ledd etter oss ser på om saksbehandleren har
 * tilgang til personen, så et oppslag uten denne sjekken utleverer henvendelser om hvem som
 * helst - også skjermede personer.
 *
 * `hentSporingsdataPerson` framfor `harTilgangPerson`: svaret inneholder både avgjørelsen og
 * feltene [AuditLogger] trenger, så tilgangsvurderingen og auditsporet blir ett kall.
 */
@Component
class Tilgangskontroll(
    private val tilgangClient: TilgangClient,
    private val auditLogger: AuditLogger,
) {
    fun sjekkTilgangTilPerson(personident: Personident) {
        val sporingsdata = tilgangClient.hentSporingsdataPerson(personident)

        // AuditLogger skriver linja og kaster selv 403 ved avslag. Den kommer som en
        // HttpClientErrorException, som DefaultRestControllerAdvice ellers oversetter til 502
        // "feil hos tjenesten vi kaller" - se KDoc-en der. Derfor byttes den her til vår egen.
        try {
            auditLogger.log(AuditLoggerEvent.ACCESS, sporingsdata)
        } catch (_: HttpClientErrorException) {
            throw IngenTilgangException()
        }

        // AuditLogger hopper over både logging og avslag for maskintoken. Kravet gjelder
        // uansett hvem som kaller, så avgjørelsen håndheves her i tillegg.
        if (!sporingsdata.tilgang) throw IngenTilgangException()
    }
}
