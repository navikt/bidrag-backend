package no.nav.bidrag.grunnlag.hendelse.schedule.valuta

import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.commons.service.slack.SlackMelding
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.BidragGrunnlagConfig
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentetValutakursResultat
import no.nav.bidrag.grunnlag.persistence.entity.Valutakursgrunnlag
import no.nav.bidrag.grunnlag.persistence.entity.ValutakursgrunnlagKilde
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.scheduling.annotation.Scheduled
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class HentValutaSchedulerTest {
    private val hentValutakursService = Mockito.mock(HentValutakursService::class.java)
    private val valutakursgrunnlagService = Mockito.mock(ValutakursgrunnlagService::class.java)
    private val slackService = Mockito.mock(SlackService::class.java)
    private val clientId = "dev-gcp:bidrag:bidrag-grunnlag"
    private val scheduler = HentValutaScheduler(hentValutakursService, valutakursgrunnlagService, slackService, clientId)

    @Test
    fun `jobben kjøres 1 januar og 1 juli i Oslo-tid`() {
        val plan = HentValutaScheduler::class.java.getMethod("hentValutakurs").getAnnotation(Scheduled::class.java)

        assertEquals("0 0 5 1 1,7 *", plan.cron)
        assertEquals("Europe/Oslo", plan.zone)
        val lås = HentValutaScheduler::class.java.getMethod("hentValutakurs").getAnnotation(SchedulerLock::class.java)
        assertEquals("hentValutakursgrunnlag", lås.name)
        assertEquals("PT15M", lås.lockAtLeastFor)
    }

    @Test
    fun `kontrolljobben kjører daglig etter valutakursjobben i Oslo-tid`() {
        val plan = HentValutaScheduler::class.java.getMethod("sjekkValutakursgrunnlag").getAnnotation(Scheduled::class.java)

        assertEquals("0 0 6 * * *", plan.cron)
        assertEquals("Europe/Oslo", plan.zone)
        val lås = HentValutaScheduler::class.java.getMethod("sjekkValutakursgrunnlag").getAnnotation(SchedulerLock::class.java)
        assertEquals("sjekkValutakursgrunnlag", lås.name)
        assertEquals("PT15M", lås.lockAtLeastFor)
    }

    @Test
    fun `varsler ikke når nyeste valutakursgrunnlag starter i gjeldende halvår`() {
        Mockito.`when`(valutakursgrunnlagService.hentSisteBrukFra()).thenReturn(LocalDate.of(2026, 7, 1))

        scheduler.sjekkValutakursgrunnlag(LocalDate.of(2026, 10, 7))

        Mockito.verifyNoInteractions(slackService)
    }

    @Test
    fun `varsler når valutakursgrunnlag for gjeldende halvår mangler`() {
        Mockito.`when`(valutakursgrunnlagService.hentSisteBrukFra()).thenReturn(LocalDate.of(2026, 1, 1))

        scheduler.sjekkValutakursgrunnlag(LocalDate.of(2026, 7, 2))

        verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for 2026-07-01 mangler i $clientId. Nyeste valutakursgrunnlag starter 2026-01-01.",
        )
    }

    @Test
    fun `varsler når det ikke finnes valutakursgrunnlag`() {
        scheduler.sjekkValutakursgrunnlag(LocalDate.of(2026, 1, 2))

        verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for 2026-01-01 mangler i $clientId. Nyeste valutakursgrunnlag starter ingen.",
        )
    }

    @Test
    fun `to samtidige pod-er kan ikke ta samme databaselås`() {
        val datakilde = DriverManagerDataSource("jdbc:h2:mem:valuta-shedlock;DB_CLOSE_DELAY=-1", "sa", "")
        datakilde.connection.use {
            ScriptUtils.executeSqlScript(it, ClassPathResource("db/migration/V1_0_39__create-table-shedlock.sql"))
        }
        val låseleverandør = BidragGrunnlagConfig().lockProvider(JdbcTemplate(datakilde))
        val konfigurasjon = LockConfiguration(Instant.now(), "hentValutakursgrunnlag", Duration.ofHours(6), Duration.ofMinutes(15))

        val første = låseleverandør.lock(konfigurasjon)
        try {
            assertTrue(første.isPresent)
            assertFalse(låseleverandør.lock(konfigurasjon).isPresent)
        } finally {
            første.ifPresent { it.unlock() }
        }
    }

    @Test
    fun `januar henter bare gjeldende valutaer med startdato 1 januar`() {
        val dato = LocalDate.of(2026, 1, 1)
        Mockito.`when`(hentValutakursService.hentValutakurs(any()))
            .thenReturn(HentValutakursResponse(emptyList()))

        scheduler.hentValutakurs(dato)

        val forespørsel = argumentCaptor<HentValutakursRequest>()
        verify(hentValutakursService).hentValutakurs(forespørsel.capture())
        assertTrue(forespørsel.firstValue.hentValutakursListe.all { it.dato == dato })
        assertTrue(forespørsel.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.USD })
        assertFalse(forespørsel.firstValue.hentValutakursListe.any { it.valutakode in setOf(Valutakode.NOK, Valutakode.ANG, Valutakode.BGN) })
        Mockito.verify(valutakursgrunnlagService).opprettValutakursgrunnlag(emptyList(), scheduler.lagGyldighetsperiode(dato))
        assertEquals(LocalDate.of(2026, 7, 1), scheduler.lagGyldighetsperiode(dato).til)
        val manglendeKurser = forespørsel.firstValue.hentValutakursListe.joinToString(", ") { it.valutakode.name }
        verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for $dato fullført i $clientId.\n" +
                "0 valutakursgrunnlag opprettet.\nKurser som ikke ble innhentet: $manglendeKurser.",
        )
    }

    @Test
    fun `juli bruker julidato og utelater valutaer som utgikk før juli`() {
        val dato = LocalDate.of(2025, 7, 1)
        Mockito.`when`(hentValutakursService.hentValutakurs(any()))
            .thenReturn(HentValutakursResponse(emptyList()))

        scheduler.hentValutakurs(dato)

        val forespørsel = argumentCaptor<HentValutakursRequest>()
        verify(hentValutakursService).hentValutakurs(forespørsel.capture())
        assertTrue(forespørsel.firstValue.hentValutakursListe.all { it.dato == dato })
        assertTrue(forespørsel.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.BGN })
        assertFalse(forespørsel.firstValue.hentValutakursListe.any { it.valutakode == Valutakode.ANG })
        Mockito.verify(valutakursgrunnlagService).opprettValutakursgrunnlag(emptyList(), scheduler.lagGyldighetsperiode(dato))
        assertEquals(LocalDate.of(2026, 1, 1), scheduler.lagGyldighetsperiode(dato).til)
    }

    @Test
    fun `feil ved innhenting propageres slik at jobben markeres feilet`() {
        val feil = IllegalStateException("Kilde utilgjengelig")
        Mockito.`when`(hentValutakursService.hentValutakurs(any()))
            .thenThrow(feil)
        Mockito.`when`(slackService.sendMelding(any(), anyOrNull(), anyOrNull()))
            .thenReturn(SlackMelding(slackService, null, channel = null, feil = "ratelimited"))

        assertSame(feil, assertThrows<IllegalStateException> { scheduler.hentValutakurs(LocalDate.of(2026, 1, 1)) })
        Mockito.verifyNoInteractions(valutakursgrunnlagService)
        verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for 2026-01-01 feilet i $clientId (IllegalStateException). Se applikasjonsloggene for detaljer.",
        )
        Mockito.verifyNoMoreInteractions(slackService)
    }

    @Test
    fun `fullført kjøring varsler etter lagring og lister feilede og manglende kurser`() {
        val dato = LocalDate.of(2026, 1, 1)
        val periode = ÅrMånedsperiode(YearMonth.of(2025, 12), YearMonth.of(2026, 1))
        val resultater = vellykkedeKurser(dato).filter { it.basisvaluta !in setOf(Valutakode.USD, Valutakode.EUR) } +
            HentetValutakursResultat.FeiledValutakurs(periode, Valutakode.EUR, Valutakode.NOK)
        Mockito.`when`(hentValutakursService.hentValutakurs(any())).thenReturn(HentValutakursResponse(resultater))
        Mockito.`when`(valutakursgrunnlagService.opprettValutakursgrunnlag(resultater, scheduler.lagGyldighetsperiode(dato)))
            .thenReturn(resultater.map { Mockito.mock(Valutakursgrunnlag::class.java) })

        scheduler.hentValutakurs(dato)

        val rekkefølge = inOrder(valutakursgrunnlagService, slackService)
        rekkefølge.verify(valutakursgrunnlagService).opprettValutakursgrunnlag(resultater, scheduler.lagGyldighetsperiode(dato))
        rekkefølge.verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for $dato fullført i $clientId.\n" +
                "${resultater.size} valutakursgrunnlag opprettet.\nKurser som ikke ble innhentet: EUR, USD.",
        )
        Mockito.verifyNoMoreInteractions(slackService)
    }

    @Test
    fun `fullført kjøring uten manglende kurser varsler at ingen mangler`() {
        val dato = LocalDate.of(2025, 7, 1)
        val resultater = vellykkedeKurser(dato)
        Mockito.`when`(hentValutakursService.hentValutakurs(any())).thenReturn(HentValutakursResponse(resultater))
        Mockito.`when`(valutakursgrunnlagService.opprettValutakursgrunnlag(resultater, scheduler.lagGyldighetsperiode(dato)))
            .thenReturn(resultater.map { Mockito.mock(Valutakursgrunnlag::class.java) })

        scheduler.hentValutakurs(dato)

        verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for $dato fullført i $clientId.\n" +
                "${resultater.size} valutakursgrunnlag opprettet.\nKurser som ikke ble innhentet: Ingen.",
        )
        Mockito.verifyNoMoreInteractions(slackService)
    }

    @Test
    fun `feil ved lagring varsles uten successmelding og opprinnelig feil propageres`() {
        val dato = LocalDate.of(2026, 1, 1)
        val resultater = vellykkedeKurser(dato)
        val feil = IllegalStateException("Database utilgjengelig")
        Mockito.`when`(hentValutakursService.hentValutakurs(any())).thenReturn(HentValutakursResponse(resultater))
        Mockito.`when`(valutakursgrunnlagService.opprettValutakursgrunnlag(resultater, scheduler.lagGyldighetsperiode(dato)))
            .thenThrow(feil)

        assertSame(feil, assertThrows<IllegalStateException> { scheduler.hentValutakurs(dato) })

        verify(slackService).sendMelding(
            "Planlagt innhenting av valutakursgrunnlag for $dato feilet i $clientId (IllegalStateException). Se applikasjonsloggene for detaljer.",
        )
        Mockito.verifyNoMoreInteractions(slackService)
    }

    @Test
    fun `andre datoer avvises før innhenting`() {
        assertThrows<IllegalArgumentException> { scheduler.hentValutakurs(LocalDate.of(2026, 1, 2)) }
        Mockito.verifyNoInteractions(hentValutakursService, valutakursgrunnlagService)
    }

    private fun vellykkedeKurser(dato: LocalDate): List<HentetValutakursResultat.HentetValutakurs> = Valutakode.entries.filter { it != Valutakode.NOK && it.aktiv(dato) }.map {
        HentetValutakursResultat.HentetValutakurs(
            periode = ÅrMånedsperiode(YearMonth.from(dato.minusMonths(1)), YearMonth.from(dato)),
            valutakursSnitt = BigDecimal.TEN,
            multiplikator = 0,
            basisvaluta = it,
            kvoteringsvaluta = Valutakode.NOK,
            hentetTidspunkt = LocalDateTime.of(2026, 1, 1, 5, 0),
            kilde = ValutakursgrunnlagKilde.ECB,
        )
    }
}
