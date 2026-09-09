package no.nav.bidrag.henvendelse.consumer

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.henvendelse.config.RestConfig
import no.nav.bidrag.henvendelse.dto.consumer.HenvendelseConsumerOutput
import no.nav.bidrag.henvendelse.dto.consumer.HenvendelseslisteKonvolutt
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Kaller sf-henvendelse-api-proxy (namespace `teamnks`), som proxyer videre til
 * henvendelsesløsningen i Salesforce.
 *
 * Kontrakten er dokumentert i tjenestens egen swagger, `src/main/resources/static/swagger.json`
 * i [navikt/sf-henvendelse-api-proxy](https://github.com/navikt/sf-henvendelse-api-proxy).
 *
 * Proxyen krever on-behalf-of-token: maskintoken gir 403 "Machine token authorization not
 * sufficient" utenfor `/kodeverk/`. Proxyen henter selv saksbehandlerens NAVident ut av tokenet
 * og videresender den til Salesforce i `X-ACTING-NAV-IDENT` - vi setter ingen slik header her.
 *
 * `X-Correlation-ID` er påkrevd, og settes av [KorrelasjonsIdInterceptor] på RestTemplaten.
 *
 * ### Forespørsel
 * ```
 * GET /henvendelseinfo/henvendelseliste?aktorid=2000012345678
 * Authorization: Bearer <on-behalf-of-token>
 * X-Correlation-ID: 4f8b1c2e-1f7a-4a3e-9c1b-8d2f6a5b0c31
 * ```
 *
 * ### Svar (200) - forkortet
 * ```json
 * [
 *   {
 *     "henvendelseType": "SAMTALEREFERAT",
 *     "fnr": "17490123474",
 *     "aktorId": "2000012345678",
 *     "kjedeId": "a0J3N000004dUBJUA2",
 *     "gjeldendeTemagruppe": "FMLI",
 *     "gjeldendeTema": "BID",
 *     "opprettetDato": "2026-06-27T12:00:00.000Z",
 *     "meldinger": [
 *       { "sendtDato": "2026-06-27T12:00:00.000Z", "kanal": "DIGITAL", "fritekst": "..." }
 *     ],
 *     "journalposter": [],
 *     "markeringer": []
 *   }
 * ]
 * ```
 * Vi leser bare feltene i [HenvendelseConsumerOutput]; resten ignoreres.
 */
@Service
class HenvendelseConsumer(
    @param:Value($$"${HENVENDELSE_URL}") private val henvendelseUrl: URI,
    @param:Qualifier(RestConfig.BEAN_HENVENDELSE_REST_TEMPLATE) restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "sf-henvendelse-api") {
    fun hentHenvendelser(aktørid: String): List<HenvendelseConsumerOutput> {
        val uri = UriComponentsBuilder
            .fromUri(henvendelseUrl)
            .pathSegment("henvendelseinfo", "henvendelseliste")
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
        } catch (_: HttpClientErrorException.NotFound) {
            // Swaggeren dokumenterer 404 som "Could not find actor". Personer uten henvendelser
            // gir ikke 404 - Apex-koden svarer 200 med tom data-liste - så 404 betyr en aktørid
            // Salesforce ikke kjenner i det hele tatt. Vi viser tom liste framfor 502: det er
            // ikke saksbehandleren som har gjort noe feil, og det finnes ingenting å vise.
            log.info("Henvendelsesløsningen kjenner ikke aktøren. Returnerer tom liste.")
            return emptyList()
        }
        if (respons.isNullOrBlank()) return emptyList()
        return tolkRespons(respons)
    }

    /**
     * Kilden svarer med en paginert konvolutt: `CRM_HenvendelseInfoListRestService` i
     * navikt/crm-henvendelse serialiserer `data`, `currentPage`, `pageSize`, `totalPages` og
     * `hasNextPage`.
     *
     * Swaggeren til proxyen dokumenterer en ren liste. Den er altså utdatert, men vi tåler
     * begge former - det koster to linjer, og BiSys pakker ut `data` på samme vis.
     */
    private fun tolkRespons(respons: String): List<HenvendelseConsumerOutput> = if (respons.trimStart().startsWith("[")) {
        commonObjectmapper.readValue(respons, henvendelseListeType)
    } else {
        val konvolutt = commonObjectmapper.readValue(respons, HenvendelseslisteKonvolutt::class.java)
        if (konvolutt.hasNextPage == true) {
            // Vi sender ikke page/pageSize, så flere sider betyr at lista er avkortet.
            // log er den arvede SLF4J-loggeren fra AbstractRestClient, ikke kotlin-logging.
            log.warn(
                "Henvendelseslista har flere sider (currentPage=${konvolutt.currentPage}), men vi henter bare den første.",
            )
        }
        konvolutt.data
    }

    companion object {
        /**
         * Kildens default er 50. Brukeroversikten viser en håndfull rader, så 100 holder med god
         * margin - og sier kilden at det finnes flere sider, logges det.
         * `modiapersonoversikt-api` bruker samme størrelse.
         */
        private const val PAGE_SIZE = 100

        private val henvendelseListeType = object : TypeReference<List<HenvendelseConsumerOutput>>() {}
    }
}
