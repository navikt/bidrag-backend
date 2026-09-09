package no.nav.bidrag.henvendelse.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/**
 * Henvendelsene til en person, slik de vises i brukeroversikten.
 *
 * Lista er pakket i et objekt framfor å svares ut som en ren JSON-liste. Det er mønsteret for
 * skjermbildedata i monorepoet - se `TransaksjonerDto` i bidrag-reskontro, som ligger rett ved
 * siden av oss i brukermenyen - og det gjør at vi senere kan legge til felter på toppnivå uten
 * å brekke den genererte frontend-klienten. Rene lister brukes til enkle oppslag, som
 * `/personidenter` i bidrag-person.
 *
 * ### Eksempel på respons fra `POST /henvendelser`
 * ```json
 * {
 *   "henvendelser": [
 *     {
 *       "kjedeId": "a0J3N000004dUBJUA2",
 *       "henvendelsestype": "MELDINGSKJEDE",
 *       "tema": "BID",
 *       "temagruppe": "FMLI",
 *       "sisteMeldingSendt": "2026-06-28T09:30:00Z"
 *     },
 *     {
 *       "kjedeId": "a0J3N000004dUBKUA2",
 *       "henvendelsestype": "SAMTALEREFERAT",
 *       "tema": "BID",
 *       "temagruppe": "FMLI",
 *       "sisteMeldingSendt": null
 *     }
 *   ]
 * }
 * ```
 * Tom liste er `{ "henvendelser": [] }`, aldri `null`.
 */
@Schema(name = "Henvendelser")
data class HenvendelserDto(
    @field:Schema(description = "Henvendelsene til personen, nyeste rekkefølge som fra kilden")
    val henvendelser: List<HenvendelseDto>,
)

/**
 * Én henvendelse.
 *
 * Kodeverdier (`tema`, `temagruppe`) dekodes ikke her - frontend har sin egen kodeverk-klient.
 * Lenken videre til Modia bygges også av frontend, som har `MODIA_URL` per miljø; herfra
 * trengs bare [kjedeId].
 */
data class HenvendelseDto(
    /** Id-en til meldingskjeden. Brukes av frontend for å lenke til henvendelsen i Modia. */
    val kjedeId: String,
    val henvendelsestype: Henvendelsestype,
    /** Tema-kode fra Navs felles kodeverk, f.eks. BID. */
    val tema: String? = null,
    /** Temagruppe-kode, f.eks. FMLI. */
    val temagruppe: String? = null,
    /**
     * Tidspunktet for den siste meldingen i kjeden, altså maks av `meldinger[].sendtDato`.
     *
     * `null` når kjeden er tom, eller når ingen av meldingene har sendtDato. De to tilfellene
     * kan ikke skilles fra hverandre her.
     */
    val sisteMeldingSendt: OffsetDateTime? = null,
)

/**
 * Henvendelsestypene BiSys viser. Kilden leverer fritekst, så ukjente verdier havner
 * i [UKJENT] framfor å velte kallet; den opprinnelige verdien logges.
 */
enum class Henvendelsestype {
    CHAT,
    MELDINGSKJEDE,
    SAMTALEREFERAT,
    UKJENT,
}
