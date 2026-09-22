package no.nav.bidrag.henvendelse.consumer

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.type.TypeReference
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.henvendelse.aop.TjenesteFeilException
import no.nav.bidrag.henvendelse.config.RestConfig
import no.nav.bidrag.henvendelse.dto.consumer.HenvendelseConsumerOutput
import no.nav.bidrag.henvendelse.dto.consumer.HenvendelseslisteKonvolutt
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Kaller sf-henvendelse-api-proxy (namespace `teamnks`), som proxyer videre til
 * henvendelsesløsningen i Salesforce.
 *
 * Proxyens swagger (`src/main/resources/static/swagger.json` i
 * [navikt/sf-henvendelse-api-proxy](https://github.com/navikt/sf-henvendelse-api-proxy))
 * beskriver endepunktet, men er utdatert på svarformen: den dokumenterer en ren liste, mens
 * kilden svarer med konvolutt. Det som faktisk skjer står i
 * `CRM_HenvendelseInfoListRestService` i navikt/crm-henvendelse.
 *
 * Proxyen krever on-behalf-of-token: maskintoken gir 403 "Machine token authorization not
 * sufficient" utenfor `/kodeverk/`. Proxyen henter selv saksbehandlerens NAVident ut av tokenet
 * og videresender den til Salesforce i `X-ACTING-NAV-IDENT` - vi setter ingen slik header her.
 *
 * `X-Correlation-ID` er påkrevd, og settes av [KorrelasjonsIdInterceptor] på RestTemplaten.
 *
 * ### Forespørsel
 * ```
 * GET /api/henvendelseinfo/henvendelseliste?aktorid=2000012345678&pageSize=100
 * Authorization: Bearer <on-behalf-of-token>
 * X-Correlation-ID: 4f8b1c2e-1f7a-4a3e-9c1b-8d2f6a5b0c31
 * ```
 *
 * ### Svar (200) - forkortet
 * ```json
 * {
 *   "data": [
 *     {
 *       "henvendelseType": "SAMTALEREFERAT",
 *       "fnr": "17490123474",
 *       "aktorId": "2000012345678",
 *       "kjedeId": "a0J3N000004dUBJUA2",
 *       "gjeldendeTemagruppe": "FMLI",
 *       "gjeldendeTema": "BID",
 *       "opprettetDato": "2026-06-27T12:00:00.000Z",
 *       "meldinger": [
 *         { "sendtDato": "2026-06-27T12:00:00.000Z", "kanal": "DIGITAL", "fritekst": "..." }
 *       ],
 *       "journalposter": [],
 *       "markeringer": []
 *     }
 *   ],
 *   "currentPage": 1,
 *   "pageSize": 100,
 *   "totalPages": 1,
 *   "hasNextPage": false
 * }
 * ```
 * Vi leser bare feltene i [HenvendelseConsumerOutput]; resten ignoreres.
 */
@Service
class HenvendelseConsumer(
    @param:Value($$"${HENVENDELSE_URL}") private val henvendelseUrl: URI,
    @param:Qualifier(RestConfig.BEAN_HENVENDELSE_REST_TEMPLATE) restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "sf-henvendelse-api") {
    fun hentHenvendelser(aktørid: String): Henvendelsesliste {
        val uri = UriComponentsBuilder
            .fromUri(henvendelseUrl)
            // `api` er proxyens basesti: `Application.kt` i navikt/sf-henvendelse-api-proxy binder
            // `"$API_BASE_PATH/{rest:.*}"` med `API_BASE_PATH = "/api"`, og ruter bare `/internal`
            // og `/static` utenom. Uten prefikset svarer den 404 med tom kropp, ikke 401 eller 403.
            .pathSegment("api", "henvendelseinfo", "henvendelseliste")
            .queryParam("aktorid", aktørid)
            // Kilden paginerer med default pageSize 50 (se CRM_HenvendelseInfoListRestService i
            // navikt/crm-henvendelse). Vi setter den eksplisitt framfor å arve en default vi ikke
            // har valgt. page utelates; default er 1, og vi henter bare første side.
            .queryParam("pageSize", PAGE_SIZE)
            .build()
            .toUri()

        // Responsen hentes som tekst og parses her framfor å la meldingskonverteren gjøre det.
        // Classpathen har både Jackson 2 (bidrag-transport/commons) og Jackson 3 (Spring Boot 4),
        // og hvilken som er i bruk avhenger av RestTemplate. Ved å parse selv blir oppførselen
        // den samme uansett - og vi kan velge form ut fra hva vi faktisk fikk.
        val respons = try {
            getForEntity<String>(uri)
        } catch (exception: Exception) {
            // Fanges på statuskode og ikke på HttpClientErrorException.NotFound: AbstractRestClient
            // lager en HttpServerErrorException - med 404 i statusfeltet - av en ikke-2xx respons
            // RestTemplate ikke selv har kastet på.
            if (exception !is RestClientResponseException || exception.statusCode != HttpStatus.NOT_FOUND) {
                throw TjenesteFeilException(TJENESTE, exception)
            }
            // Swaggeren dokumenterer 404 som "Could not find actor". Personer uten henvendelser
            // gir ikke 404 - Apex-koden svarer 200 med tom data-liste - så den responsen betyr en
            // aktørid Salesforce ikke kjenner i det hele tatt. Vi viser tom liste framfor 502: det
            // er ikke saksbehandleren som har gjort noe feil, og det finnes ingenting å vise.
            //
            // Kroppen må sjekkes, ikke bare statuskoden: proxyen svarer også 404, med tom kropp, på
            // en rute den ikke kjenner. Det skjedde da `/api` manglet i basestien. Uten denne
            // sjekken ville en feil HENVENDELSE_URL sett ut som en person uten henvendelser, og
            // feilen ville blitt stående usett.
            //
            // AbstractRestClient har allerede logget en WARN med hele URL-en og stacktracen før
            // vi kommer hit, så dette normaltilfellet ser ut som en feil i loggen. URL-en er også
            // grunnen til at maskeringen i logback-spring.xml må virke - den inneholder aktøriden.
            if (!exception.responseBodyAsString.contains(UKJENT_AKTØR, ignoreCase = true)) {
                throw TjenesteFeilException(TJENESTE, exception)
            }
            log.info("Henvendelsesløsningen kjenner ikke aktøren. Returnerer tom liste.")
            return Henvendelsesliste(emptyList(), avkortet = false)
        }
        // Tom kropp med 200 er ikke et gyldig svar fra kilden - den svarer alltid med konvolutten.
        if (respons.isNullOrBlank()) {
            throw TjenesteFeilException(TJENESTE, IllegalStateException("Tom kropp fra henvendelseslista"))
        }
        // En respons vi ikke klarer å tolke er en feil hos tjenesten vi kaller, ikke hos oss.
        // Uten denne ville Jackson-feilen gått til catch-allen i DefaultRestControllerAdvice og
        // blitt 500 "Ukjent feil", som sender saksbehandleren til feil sted med spørsmålet.
        return try {
            tolkRespons(respons)
        } catch (exception: JacksonException) {
            throw TjenesteFeilException(TJENESTE, exception)
        }
    }

    /**
     * Kilden svarer med en paginert konvolutt: `CRM_HenvendelseInfoListRestService` i
     * navikt/crm-henvendelse serialiserer `data`, `currentPage`, `pageSize`, `totalPages` og
     * `hasNextPage`.
     *
     * Swaggeren til proxyen dokumenterer en ren liste. Den er altså utdatert, men vi tåler
     * begge former - det koster to linjer, og BiSys pakker ut `data` på samme vis.
     */
    private fun tolkRespons(respons: String): Henvendelsesliste = if (respons.trimStart().startsWith("[")) {
        Henvendelsesliste(commonObjectmapper.readValue(respons, henvendelseListeType), avkortet = false)
    } else {
        val konvolutt = commonObjectmapper.readValue(respons, HenvendelseslisteKonvolutt::class.java)
        if (konvolutt.hasNextPage == true) {
            // Vi sender ikke page/pageSize, så flere sider betyr at lista er avkortet.
            // log er den arvede SLF4J-loggeren fra AbstractRestClient, ikke kotlin-logging.
            log.warn(
                "Henvendelseslista har flere sider (currentPage=${konvolutt.currentPage}), men vi henter bare den første.",
            )
        }
        // `data` mangler helt: da er det ikke konvolutten vi fikk, uansett hvor gyldig JSON-en er.
        val data = konvolutt.data ?: throw TjenesteFeilException(
            TJENESTE,
            IllegalStateException("Responsen mangler feltet data"),
        )
        Henvendelsesliste(data, avkortet = konvolutt.hasNextPage == true)
    }

    companion object {
        /**
         * Den dokumenterte 404-meldinga fra kilden. Hele setningen matches, ikke bare ordet
         * "actor": en rutingfeil eller en annen tjeneste i kjeden kan nevne det samme ordet, og da
         * skal svaret bli 502 framfor en tom liste som skjuler feilen.
         */
        private const val UKJENT_AKTØR = "Could not find actor"

        /**
         * Kildens default er 50. Brukeroversikten viser en håndfull rader, så 100 holder med god
         * margin - og sier kilden at det finnes flere sider, logges det.
         * `modiapersonoversikt-api` bruker samme størrelse.
         */
        private const val PAGE_SIZE = 100

        private val henvendelseListeType = object : TypeReference<List<HenvendelseConsumerOutput>>() {}
    }
}

private const val TJENESTE = "sf-henvendelse-api-proxy"

/**
 * Henvendelsene fra kilden, og om lista er avkortet.
 *
 * [avkortet] er sant når kilden sier at det finnes flere sider enn den vi henter. Da må
 * brukeroversikten kunne si fra om at den viser et utvalg - en stille avkorting er ikke til å
 * skille fra en person som faktisk har få henvendelser.
 */
data class Henvendelsesliste(
    val henvendelser: List<HenvendelseConsumerOutput>,
    val avkortet: Boolean,
)
