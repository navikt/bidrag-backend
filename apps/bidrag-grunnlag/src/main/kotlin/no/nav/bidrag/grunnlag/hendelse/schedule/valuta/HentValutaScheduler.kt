package no.nav.bidrag.grunnlag.hendelse.schedule.valuta

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.Datoperiode
import no.nav.bidrag.domene.tid.Periode
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LOGGER = KotlinLogging.logger { }
internal val VALUTAKURS_TIDSSONE: ZoneId = ZoneId.of("Europe/Oslo")
private val SLACK_DATOFORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

internal enum class ValutakursKjøring(
    val beskrivelse: String,
) {
    PLANLAGT("Planlagt"),
    MANUELL("Manuell"),
}

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT6H")
class HentValutaScheduler(
    private val hentValutakursService: HentValutakursService,
    private val valutakursgrunnlagService: ValutakursgrunnlagService,
    private val slackService: SlackService,
    @param:Value($$"${NAIS_CLIENT_ID:bidrag-grunnlag-local}") private val clientId: String,
    @param:Value($$"${NAIS_CLUSTER_NAME:dev-gcp}") private val clusterName: String = "dev-gcp",
) {
    @Scheduled(cron = $$"${VALUTAKURS_HENT_CRON:0 0 5 1 1,7 *}", zone = "Europe/Oslo")
    @SchedulerLock(name = "hentValutakursgrunnlag", lockAtLeastFor = "PT15M")
    fun hentValutakurs() {
        LockAssert.assertLocked()
        hentValutakurs(LocalDate.now(VALUTAKURS_TIDSSONE))
    }

    @Scheduled(cron = $$"${VALUTAKURS_SJEKK_CRON:0 0 6 * * *}", zone = "Europe/Oslo")
    @SchedulerLock(name = "sjekkValutakursgrunnlag", lockAtLeastFor = "PT15M")
    fun sjekkValutakursgrunnlag() {
        LockAssert.assertLocked()
        sjekkValutakursgrunnlag(LocalDate.now(VALUTAKURS_TIDSSONE))
    }

    internal fun sjekkValutakursgrunnlag(dato: LocalDate) {
        val forventetBrukFra = LocalDate.of(dato.year, if (dato.monthValue >= 7) 7 else 1, 1)
        val sisteBrukFra = valutakursgrunnlagService.hentSisteBrukFra()
        if (sisteBrukFra != forventetBrukFra) {
            val nyesteGrunnlag = sisteBrukFra?.format(SLACK_DATOFORMAT) ?: "ingen"
            slackService.sendMelding(
                "*Valutakursgrunnlag mangler*\n" +
                    "• *Forventet startdato:* ${forventetBrukFra.format(SLACK_DATOFORMAT)}\n" +
                    "• *Nyeste startdato:* $nyesteGrunnlag\n" +
                    applikasjonslinje(),
            )
        }
    }

    internal fun hentValutakurs(
        dato: LocalDate,
        kjøring: ValutakursKjøring = ValutakursKjøring.PLANLAGT,
    ) {
        val melding = try {
            require(dato.dayOfMonth == 1 && dato.monthValue in listOf(1, 7)) { "Valutakursgrunnlag må starte 1. januar eller 1. juli" }
            LOGGER.info { "Henter valutakursgrunnlag for $dato" }
            val valutakoder = Valutakode.entries.filter { it != Valutakode.NOK && it.aktiv(dato) }
            val hentValutakursRequest = HentValutakursRequest(
                hentValutakursListe = valutakoder.map { valutakode ->
                    HentValutakurs(
                        dato = dato,
                        valutakode = valutakode,
                    )
                },
            )

            val gyldighetsperiode = lagGyldighetsperiode(dato)
            val hentvalutakursResponse = hentValutakursService.hentValutakurs(hentValutakursRequest)
            val valutakursgrunnlag = valutakursgrunnlagService.opprettValutakursgrunnlag(hentvalutakursResponse.hentetValutakursListe, gyldighetsperiode)
            LOGGER.info { "${valutakursgrunnlag.size} valutakursgrunnlag opprettet" }
            val innhentedeValutaer = hentvalutakursResponse.hentetValutakursListe
                .filterIsInstance<HentetValutakursResultat.HentetValutakurs>()
                .map { it.basisvaluta }
                .toSet()
            val manglendeKurser = valutakoder.filter { it !in innhentedeValutaer }.joinToString(", ") { it.name }.ifEmpty { "Ingen" }
            "*Innhenting av valutakurser fullført*\n" +
                "• *Startdato:* ${dato.format(SLACK_DATOFORMAT)}\n" +
                "• *Kjøring:* ${kjøring.beskrivelse}\n" +
                applikasjonslinje() +
                "• *Opprettet valutakursgrunnlag:* ${valutakursgrunnlag.size}\n" +
                "• *Manglende kurser:* $manglendeKurser"
        } catch (e: Exception) {
            slackService.sendMelding(
                "*Innhenting av valutakurser feilet*\n" +
                    "• *Startdato:* ${dato.format(SLACK_DATOFORMAT)}\n" +
                    "• *Kjøring:* ${kjøring.beskrivelse}\n" +
                    applikasjonslinje() +
                    "• *Feiltype:* ${e.javaClass.simpleName}\n" +
                    "Se applikasjonsloggene for detaljer.",
            )
            throw e
        }
        slackService.sendMelding(melding)
    }

    internal fun lagGyldighetsperiode(dato: LocalDate): Periode<LocalDate> = Datoperiode(dato, dato.plusMonths(6))

    private fun applikasjonslinje(): String =
        if (clusterName.startsWith("prod-")) {
            ""
        } else {
            "• *Applikasjon:* $clientId\n"
        }
}
