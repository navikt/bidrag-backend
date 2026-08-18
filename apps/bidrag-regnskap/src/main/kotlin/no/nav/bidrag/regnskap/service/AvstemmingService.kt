package no.nav.bidrag.regnskap.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.regnskap.fil.avstemning.AvstemmingsfilGenerator
import no.nav.bidrag.regnskap.fil.overføring.FiloverføringTilElinKlient
import no.nav.bidrag.transport.regnskap.avstemning.SumPrSakResponse
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val LOGGER = KotlinLogging.logger { }

@Service
class AvstemmingService(
    private val avstemmingsfilGenerator: AvstemmingsfilGenerator,
    private val persistenceService: PersistenceService,
    private val filoverføringTilElinKlient: FiloverføringTilElinKlient,
) {

    companion object {
        private const val AVSTEMNINGSMAPPE = "avstemning/"
        private const val DATO_FORMAT = "yyMMdd"
        private const val KONTERINGS_FILNAVN_PREFIX = "avstdet_D"
        private const val SUMMERINGS_FILNAVN_PREFIX = "avstsum_D"
    }

    fun startAvstemming(dato: LocalDate, fomTidspunkt: LocalDateTime? = null, tomTidspunkt: LocalDateTime? = null) {
        if (fomTidspunkt != null && tomTidspunkt != null) {
            startAvstemmingForDatoMedTidspunkt(dato, fomTidspunkt, tomTidspunkt)
        } else {
            startAvstemmingForDato(dato)
        }
    }

    private fun startAvstemmingForDato(dato: LocalDate) {
        LOGGER.info { "Starter avstemning for dato: $dato" }
        avstemmingsfilGenerator.skrivAvstemmingsfil(
            persistenceService.hentAlleKonteringerForDato(dato).filter { it.behandlingsstatusOkTidspunkt != null },
            dato,
        )
    }

    private fun startAvstemmingForDatoMedTidspunkt(dato: LocalDate, fomTidspunkt: LocalDateTime, tomTidspunkt: LocalDateTime) {
        LOGGER.info { "Starter avstemning for dato: $dato for konteringer mellom $fomTidspunkt og $tomTidspunkt" }
        avstemmingsfilGenerator.skrivAvstemmingsfil(
            persistenceService.hentAlleKonteringerForDato(
                dato,
                fomTidspunkt,
                tomTidspunkt,
            ).filter { it.behandlingsstatusOkTidspunkt != null },
            dato,
        )
    }

    fun startManuellOverføringAvstemingTilSftpFraGcpBucket(dato: LocalDate) {
        val nowFormattert = dato.format(DateTimeFormatter.ofPattern(DATO_FORMAT)).toString()
        val avstemmingKonteringFilnavn = "$KONTERINGS_FILNAVN_PREFIX$nowFormattert.xml"
        val avstemmingSummeringFilnavn = "$SUMMERINGS_FILNAVN_PREFIX$nowFormattert.xml"

        filoverføringTilElinKlient.lastOppFilTilFilsluse(AVSTEMNINGSMAPPE, avstemmingKonteringFilnavn)
        filoverføringTilElinKlient.lastOppFilTilFilsluse(AVSTEMNINGSMAPPE, avstemmingSummeringFilnavn)
    }

    fun hentSumForSaker(stønadstype: Stønadstype, periode: YearMonth): SumPrSakResponse {
        LOGGER.info { "Henter summering for saker av type $stønadstype for periode $periode" }
        return SumPrSakResponse(persistenceService.hentSakSumForStønadOgMåned(stønadstype, periode))
    }
}
