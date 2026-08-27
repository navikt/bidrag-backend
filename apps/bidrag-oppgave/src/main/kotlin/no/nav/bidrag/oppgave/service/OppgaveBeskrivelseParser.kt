package no.nav.bidrag.oppgave.service

import no.nav.bidrag.oppgave.dto.Beskrivelseinnslag
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Tolker oppgave-beskrivelsen til strukturerte innslag.
 *
 * Formatet er det samme som Bisys skriver og leser
 * (`OppgaveBeskrivelseFormatterImpl` i bidrag-bisys):
 *
 * ```
 * --- 31.12.2016 23:59 Gjøresak, Iver (G161234, 4802) ---
 * En kommentar som kan gå over flere linjer
 * · Frist endret fra 14.01.2017 til 31.12.2016
 * --- 28.12.2016 20:17 Automatisk jobb ---
 * Mottatt melding om utflytting for BM i sak.
 * ```
 *
 * Rekkefølgen fra kilden beholdes (nyeste først). Klarer vi ikke å tolke innholdet – typisk et
 * ugyldig tidsstempel i en header – logges det som warning og [parse] returnerer `null`.
 */
object OppgaveBeskrivelseParser {

    private val logger = LoggerFactory.getLogger(javaClass)

    private const val HEADER_PREFIX = "---"
    private const val ENDRING_PREFIX = "\u00B7"
    private const val AUTOMATISK_JOBB = "Automatisk jobb"
    private const val BISYS_SERVICE_BRUKER = "srvbisys"

    private val tidsstempelFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)

    private val headerMedSaksbehandler =
        Regex("""---\s*(\d\d\.\d\d\.\d\d\d\d \d\d:\d\d) (.*?) \((\S+), (\S+).*?\)\s*---""")
    private val headerUtenSaksbehandler =
        Regex("""---\s*(\d\d\.\d\d\.\d\d\d\d \d\d:\d\d) (.*?)\s*---""")
    private val kunSiffer = Regex("""\d+""")
    private val linjeskift = Regex("""\r?\n""")

    fun parse(
        beskrivelse: String?,
        sistEndretTidspunkt: OffsetDateTime? = null,
        sistEndretAv: String? = null,
        sistEndretEnhetsnr: String? = null,
        oppgaveId: Long? = null,
    ): List<Beskrivelseinnslag>? {
        if (beskrivelse.isNullOrBlank()) return emptyList()

        return try {
            tolkLinjer(beskrivelse, sistEndretTidspunkt, sistEndretAv, sistEndretEnhetsnr)
        } catch (e: DateTimeParseException) {
            // Beskrivelsen kan inneholde personopplysninger og skal ikke logges. Meldingen fra
            // DateTimeParseException inneholder kun tidsstempelet fra headeren, som er trygt å logge.
            logger.warn(
                "Kunne ikke tolke beskrivelseshistorikk for oppgave {}, setter historikken til null. Årsak: {}",
                oppgaveId,
                e.message,
            )
            null
        }
    }

    private fun tolkLinjer(
        beskrivelse: String,
        sistEndretTidspunkt: OffsetDateTime?,
        sistEndretAv: String?,
        sistEndretEnhetsnr: String?,
    ): List<Beskrivelseinnslag> {
        val innslag = mutableListOf<Innslag>()

        fun gjeldendeInnslag(): Innslag = innslag.lastOrNull()
            ?: defaultInnslag(sistEndretTidspunkt, sistEndretAv, sistEndretEnhetsnr).also { innslag.add(it) }

        beskrivelse.split(linjeskift).forEach { linje ->
            when {
                linje.isBlank() -> Unit

                linje.startsWith(HEADER_PREFIX) -> {
                    val nyttInnslag = tolkHeader(linje)
                    if (nyttInnslag != null) {
                        innslag.add(nyttInnslag)
                    } else {
                        gjeldendeInnslag().leggTilKommentarlinje(linje)
                    }
                }

                linje.startsWith(ENDRING_PREFIX) -> gjeldendeInnslag().leggTilEndring(linje.substring(1).trim())

                else -> gjeldendeInnslag().leggTilKommentarlinje(linje)
            }
        }

        return innslag.map { it.tilDto() }
    }

    private fun tolkHeader(linje: String): Innslag? {
        headerMedSaksbehandler.matchEntire(linje)?.let {
            return Innslag(
                tidspunkt = parseTidspunkt(it.groupValues[1]),
                saksbehandlerNavn = it.groupValues[2],
                saksbehandlerId = it.groupValues[3],
                enhetsnr = it.groupValues[4],
            )
        }
        headerUtenSaksbehandler.matchEntire(linje)?.let {
            return Innslag(
                tidspunkt = parseTidspunkt(it.groupValues[1]),
                saksbehandlerNavn = it.groupValues[2],
            )
        }
        return null
    }

    private fun parseTidspunkt(verdi: String): LocalDateTime = LocalDateTime.parse(verdi, tidsstempelFormat)

    private fun defaultInnslag(
        tidspunkt: OffsetDateTime?,
        ident: String?,
        enhetsnr: String?,
    ): Innslag = if (ident.equals(BISYS_SERVICE_BRUKER, ignoreCase = true)) {
        Innslag(tidspunkt = tidspunkt?.toLocalDateTime(), saksbehandlerNavn = AUTOMATISK_JOBB)
    } else {
        Innslag(
            tidspunkt = tidspunkt?.toLocalDateTime(),
            saksbehandlerId = ident,
            enhetsnr = enhetsnr?.takeIf { kunSiffer.matches(it) },
        )
    }

    private class Innslag(
        val tidspunkt: LocalDateTime? = null,
        val saksbehandlerNavn: String? = null,
        val saksbehandlerId: String? = null,
        val enhetsnr: String? = null,
    ) {
        private val kommentarlinjer = mutableListOf<String>()
        private val endringer = mutableListOf<String>()

        fun leggTilKommentarlinje(linje: String) {
            kommentarlinjer.add(linje)
        }

        fun leggTilEndring(endring: String) {
            endringer.add(endring)
        }

        fun tilDto(): Beskrivelseinnslag = Beskrivelseinnslag(
            tidspunkt = tidspunkt,
            saksbehandlerNavn = saksbehandlerNavn,
            saksbehandlerId = saksbehandlerId,
            enhetsnr = enhetsnr,
            kommentar = kommentarlinjer.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            endringer = endringer.toList(),
        )
    }
}
