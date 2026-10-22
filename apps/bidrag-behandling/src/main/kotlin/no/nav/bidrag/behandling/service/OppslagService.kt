package no.nav.bidrag.behandling.service

import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.behandling.consumer.BidragPersonConsumer
import no.nav.bidrag.behandling.consumer.BidragTilgangskontrollConsumer
import no.nav.bidrag.behandling.consumer.BidragVedtakConsumer
import no.nav.bidrag.behandling.transformers.vedtak.takeIfNotNullOrEmpty
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragssakerRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragssakerResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.SkyldnerStønaderResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.søknad.FinnSammenknytningerHovedsøknadResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException

fun hentLøpendeBidrag(bp: Personident): LøpendeBidragssakerResponse? = try {
    AppContext.getBean(BidragBeløpshistorikkConsumer::class.java).hentLøpendeBidrag(LøpendeBidragssakerRequest(bp))
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av beløpshistorikk $bp" }
    null
}

fun hentSammenknytningerHovedsøknad(søknadsid: Long): FinnSammenknytningerHovedsøknadResponse? = try {
    AppContext.getBean(BidragBBMConsumer::class.java).finnAktiveSammenknytningerHovedsøknadCached(søknadsid)
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av sammenknytninger for hovedsøknad $søknadsid" }
    null
}

fun hentStønad(req: HentStønadHistoriskRequest): StønadDto? = try {
    AppContext.getBean(BidragBeløpshistorikkConsumer::class.java).hentHistoriskeStønader(req)
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av beløpshistorikk $req" }
    null
}

fun hentAlleStønaderForBidragspliktig(bp: Personident): SkyldnerStønaderResponse? = try {
    AppContext.getBean(BidragBeløpshistorikkConsumer::class.java).hentAlleStønaderForBidragspliktig(bp)
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av beløpshistorikk $bp" }
    null
}

fun hentVedtak(vedtaksid: Int?): VedtakDto? = try {
    vedtaksid.takeIfNotNullOrEmpty {
        AppContext.getBean(BidragVedtakConsumer::class.java).hentVedtak(it)
    }
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av vedtak $vedtaksid" }
    if (e is HttpStatusCodeException) {
        throw e
    }
    null
}
fun sjekkTilgangSak(saksnummer: String): Boolean = try {
    AppContext.getBean(BidragTilgangskontrollConsumer::class.java).sjekkTilgangSak(saksnummer)
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av person for ident $saksnummer" }
    false
}

private val statuserSomGirIngenPerson = setOf(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN)

/**
 * Henter person fra bidrag-person.
 * Gir null når personen ikke finnes (404) eller bidrag-person svarer 403, slik at kallstedet faller tilbake på lagrede opplysninger.
 * 403 betyr at saksbehandler mangler tilgang, eller ved apptoken at tilgangsoppsettet mellom appene er feil.
 * Andre feil kastes videre. Null betyr ikke at saksbehandler har tilgang, så funksjonen må ikke brukes som tilgangskontroll.
 */
fun hentPerson(ident: String?): PersonDto? = try {
    ident.takeIfNotNullOrEmpty {
        AppContext.getBean(BidragPersonConsumer::class.java).hentPerson(it)
    }
} catch (e: HttpStatusCodeException) {
    if (e.statusCode in statuserSomGirIngenPerson) {
        null
    } else {
        throw e
    }
}

fun harTilgangSak(saksnummer: String) = sjekkTilgangSak(saksnummer)

/** Fødselsdato fra bidrag-person, eller null etter samme regler som [hentPerson]. */
fun hentPersonFødselsdato(ident: String?) = hentPerson(ident)?.fødselsdato

/** Visningsnavn fra bidrag-person, eller null etter samme regler som [hentPerson]. */
fun hentPersonVisningsnavn(ident: String?) = hentPerson(ident)?.visningsnavn

/**
 * Henter gjeldende ident fra bidrag-person.
 * Gir identen det ble søkt på når [hentPerson] gir null, altså ved 404 eller 403.
 * Andre feil kastes videre. Brukes bare til intern matching, ikke som tilgangskontroll.
 */
fun hentNyesteIdentIgnorerForbidden(ident: String?) = ident?.let { hentPerson(it)?.ident ?: Personident(it) }

@Service
data class PersonService(
    val personConsumer: BidragPersonConsumer,
) {
    /** Samme feilhåndtering som den frittstående [no.nav.bidrag.behandling.service.hentPerson]. */
    fun hentPerson(ident: String?): PersonDto? = try {
        ident.takeIfNotNullOrEmpty {
            personConsumer.hentPerson(it)
        }
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode in statuserSomGirIngenPerson) {
            null
        } else {
            throw e
        }
    }

    /** Fødselsdato fra bidrag-person, eller null etter samme regler som [hentPerson]. */
    fun hentPersonFødselsdato(ident: String?) = hentPerson(ident)?.fødselsdato

    /** Visningsnavn fra bidrag-person, eller null etter samme regler som [hentPerson]. */
    fun hentPersonVisningsnavn(ident: String?) = hentPerson(ident)?.visningsnavn

    /**
     * Gjeldende ident fra bidrag-person, eller null ved 404 og 403.
     * Kallstedet må selv falle tilbake på søkt ident. Sammenlign aldri to null-svar som lik person.
     */
    fun hentNyesteIdent(ident: String?) = hentPerson(ident)?.ident
}
