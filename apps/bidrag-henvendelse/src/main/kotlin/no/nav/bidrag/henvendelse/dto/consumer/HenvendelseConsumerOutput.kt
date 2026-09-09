package no.nav.bidrag.henvendelse.dto.consumer

import java.time.OffsetDateTime

/**
 * Delvis speiling av `Henvendelse`-skjemaet i sf-henvendelse-api-proxy, slik det er
 * dokumentert i tjenestens swagger (`src/main/resources/static/swagger.json` i
 * [navikt/sf-henvendelse-api-proxy](https://github.com/navikt/sf-henvendelse-api-proxy)).
 *
 * Fullt skjema har feltene `henvendelseType`, `fnr`, `aktorId`, `opprettetDato`,
 * `avsluttetDato`, `kasseringsDato`, `avsluttetAv`, `sattTilSladdingAv`, `sladding`,
 * `feilsendt`, `kjedeId`, `gjeldendeTemagruppe`, `gjeldendeTema`, `journalposter`,
 * `meldinger` og `markeringer`.
 *
 * Vi tar med de fem feltene brukeroversikten trenger. Resten utelates bevisst - blant annet
 * meldingstekst (`fritekst`), journalposter og markeringer - fordi henvendelsene ikke
 * behandles her; saksbehandleren går videre til Modia. ObjectMapperen er konfigurert med
 * `FAIL_ON_UNKNOWN_PROPERTIES = false`, så resten ignoreres uten videre.
 *
 * ### Eksempel på ett element i responsen
 * ```json
 * {
 *   "henvendelseType": "MELDINGSKJEDE",
 *   "fnr": "17490123474",
 *   "aktorId": "2000012345678",
 *   "kjedeId": "a0J3N000004dUBJUA2",
 *   "gjeldendeTemagruppe": "FMLI",
 *   "gjeldendeTema": "BID",
 *   "meldinger": [
 *     { "sendtDato": "2026-06-27T12:00:00.000Z" },
 *     { "sendtDato": "2026-06-28T09:30:00.000Z" }
 *   ]
 * }
 * ```
 */
data class HenvendelseConsumerOutput(
    /** Id-en til meldingskjeden. Identifiserer den enkelte henvendelsen. */
    val kjedeId: String? = null,
    /** CHAT, MELDINGSKJEDE eller SAMTALEREFERAT. Fritekst i kilden. */
    val henvendelseType: String? = null,
    /** Tema-kode fra Navs felles kodeverk, f.eks. BID. */
    val gjeldendeTema: String? = null,
    /** Temagruppe-kode, f.eks. FMLI. */
    val gjeldendeTemagruppe: String? = null,
    val meldinger: List<MeldingConsumerOutput> = emptyList(),
)

/**
 * Vi leser kun `sendtDato`, som brukes til å finne tidspunktet for den siste meldingen i
 * kjeden. Meldingsteksten hentes bevisst ikke.
 */
data class MeldingConsumerOutput(
    val sendtDato: OffsetDateTime? = null,
)

/**
 * Den paginerte konvolutten kilden svarer med, slik `CRM_HenvendelseInfoListRestService` i
 * navikt/crm-henvendelse serialiserer den. Proxyens swagger dokumenterer en ren liste og er
 * utdatert på dette punktet; consumeren tåler begge former.
 *
 * Personer uten henvendelser gir 200 med tom `data` og `totalPages: 0`, ikke 404.
 *
 * ### Eksempel
 * ```json
 * {
 *   "data": [ { "kjedeId": "a0J3N000004dUBJUA2" } ],
 *   "currentPage": 1,
 *   "pageSize": 100,
 *   "totalPages": 1,
 *   "hasNextPage": false
 * }
 * ```
 */
data class HenvendelseslisteKonvolutt(
    val data: List<HenvendelseConsumerOutput> = emptyList(),
    val currentPage: Int? = null,
    val pageSize: Int? = null,
    val totalPages: Int? = null,
    val hasNextPage: Boolean? = null,
)
