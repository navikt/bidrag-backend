package no.nav.bidrag.grunnlag.hendelse.schedule.valuta

import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.grunnlag.BidragGrunnlagConfig
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.grunnlag.service.ValutakursgrunnlagService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.scheduling.annotation.Scheduled
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class HentValutaSchedulerTest {
    private val hentValutakursService = Mockito.mock(HentValutakursService::class.java)
    private val valutakursgrunnlagService = Mockito.mock(ValutakursgrunnlagService::class.java)
    private val scheduler = HentValutaScheduler(hentValutakursService, valutakursgrunnlagService)

    @Test
    fun `jobben kjøres 1 januar og 1 juli i Oslo-tid`() {
        val plan = HentValutaScheduler::class.java.getMethod("hentValutakurs").getAnnotation(Scheduled::class.java)

        assertEquals("0 0 9 1 1,7 *", plan.cron)
        assertEquals("Europe/Oslo", plan.zone)
        val lås = HentValutaScheduler::class.java.getMethod("hentValutakurs").getAnnotation(SchedulerLock::class.java)
        assertEquals("hentValutakursgrunnlag", lås.name)
        assertEquals("PT15M", lås.lockAtLeastFor)
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
        Mockito.`when`(hentValutakursService.hentValutakurs(any()))
            .thenThrow(IllegalStateException("Kilde utilgjengelig"))

        assertThrows<IllegalStateException> { scheduler.hentValutakurs(LocalDate.of(2026, 1, 1)) }
        Mockito.verifyNoInteractions(valutakursgrunnlagService)
    }

    @Test
    fun `andre datoer avvises før innhenting`() {
        assertThrows<IllegalArgumentException> { scheduler.hentValutakurs(LocalDate.of(2026, 1, 2)) }
        Mockito.verifyNoInteractions(hentValutakursService, valutakursgrunnlagService)
    }
}
