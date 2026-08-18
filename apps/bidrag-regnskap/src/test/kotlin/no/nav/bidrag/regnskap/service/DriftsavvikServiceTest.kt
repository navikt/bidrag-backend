package no.nav.bidrag.regnskap.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
class DriftsavvikServiceTest {

    private val persistenceService: PersistenceService = mockk()
    private val kravService: KravService = mockk()
    private val oppdragsperiodeService: OppdragsperiodeService = mockk()
    private val service = DriftsavvikService(persistenceService, kravService, oppdragsperiodeService)

    @Test
    fun `skal oppdatere driftsavvik med nye verdier`() {
        val driftsavvikId = 1
        val eksisterendeDriftsavvik = Driftsavvik(
            tidspunktFra = LocalDateTime.now().minusDays(1),
            tidspunktTil = null,
            opprettetAv = "test",
            årsak = "testÅrsak",
            skalStoppeInnlesning = true,
        )
        val oppdatertDriftsavvikSlot = slot<Driftsavvik>()
        val nyttTidspunktTil = LocalDateTime.now()
        val nyttSkalStoppeInnlesning = false

        every { persistenceService.hentDriftsavvik(driftsavvikId) } returns eksisterendeDriftsavvik
        every { persistenceService.lagreDriftsavvik(capture(oppdatertDriftsavvikSlot)) } returns driftsavvikId

        val resultatId = service.endreDriftsavvik(driftsavvikId, nyttTidspunktTil, nyttSkalStoppeInnlesning)

        assertEquals(driftsavvikId, resultatId)
        verify(exactly = 1) { persistenceService.hentDriftsavvik(driftsavvikId) }
        verify(exactly = 1) { persistenceService.lagreDriftsavvik(any()) }

        val oppdatertDriftsavvik = oppdatertDriftsavvikSlot.captured
        assertEquals(nyttTidspunktTil, oppdatertDriftsavvik.tidspunktTil)
        assertEquals(nyttSkalStoppeInnlesning, oppdatertDriftsavvik.skalStoppeInnlesning)
    }

    @Test
    fun `skal returnere null hvis driftsavvik ikke finnes`() {
        val driftsavvikId = 1
        every { persistenceService.hentDriftsavvik(driftsavvikId) } returns null

        val resultatId = service.endreDriftsavvik(driftsavvikId, LocalDateTime.now(), true)

        assertNull(resultatId)
        verify(exactly = 1) { persistenceService.hentDriftsavvik(driftsavvikId) }
        verify(exactly = 0) { persistenceService.lagreDriftsavvik(any()) }
    }

    @Test
    fun `skal hente flere driftsavvik`() {
        val driftsavvik = listOf(mockk<Driftsavvik>())
        every { persistenceService.hentFlereDriftsavvik(PageRequest.of(0, 5)) } returns driftsavvik

        val resultat = service.hentFlereDriftsavvik(5)

        assertEquals(driftsavvik, resultat)
        verify(exactly = 1) { persistenceService.hentFlereDriftsavvik(PageRequest.of(0, 5)) }
    }

    @Test
    fun `skal hente alle aktive driftsavvik`() {
        val aktiveDriftsavvik = listOf(mockk<Driftsavvik>())
        every { persistenceService.hentAlleAktiveDriftsavvik() } returns aktiveDriftsavvik

        val resultat = service.hentAlleAktiveDriftsavvik()

        assertEquals(aktiveDriftsavvik, resultat)
        verify(exactly = 1) { persistenceService.hentAlleAktiveDriftsavvik() }
    }

    @Test
    fun `skal lagre driftsavvik`() {
        val tidspunktFra = LocalDateTime.now()
        val tidspunktTil = LocalDateTime.now().plusDays(1)
        val opprettetAv = "test"
        val årsak = "testÅrsak"
        val skalStoppeInnlesning = true
        val driftsavvikId = 123

        every { persistenceService.lagreDriftsavvik(any()) } returns driftsavvikId

        val resultat = service.lagreDriftsavvik(
            tidspunktFra,
            tidspunktTil,
            opprettetAv,
            årsak,
            skalStoppeInnlesning,
        )

        assertEquals(driftsavvikId, resultat)
        verify {
            persistenceService.lagreDriftsavvik(
                match {
                    it.tidspunktFra == tidspunktFra &&
                        it.tidspunktTil == tidspunktTil &&
                        it.opprettetAv == opprettetAv &&
                        it.årsak == årsak &&
                        it.skalStoppeInnlesning == skalStoppeInnlesning
                },
            )
        }
    }

    @Test
    fun `skal sjekke aktivt driftsavvik`() {
        every { persistenceService.harAktivtDriftsavvik(false) } returns true

        val resultat = service.harAktivtDriftsavvik(false)

        assertEquals(true, resultat)
        verify(exactly = 1) { persistenceService.harAktivtDriftsavvik(false) }
    }

    @Test
    fun `skal slippe vedtak gjennom driftsavvik`() {
        val vedtakId = 123
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdragsperiodeId = 456, oppdrag = TestData.opprettOppdrag(oppdragId = 321))

        every { oppdragsperiodeService.hentAlleOppdragsperiodeMedVedtaksId(vedtakId) } returns listOf(oppdragsperiode)
        every { kravService.erVedlikeholdsmodusPåslått() } returns false
        every { kravService.sendKrav(any()) } returns Unit

        service.slippVedtakGjennomDriftsavvik(vedtakId)

        verify { oppdragsperiodeService.hentAlleOppdragsperiodeMedVedtaksId(vedtakId) }
        verify { kravService.sendKrav(listOf(321)) }
    }

    @Test
    fun `skal ikke sende krav når vedlikeholdsmodus er påslått`() {
        val vedtakId = 123
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdragsperiodeId = 456, oppdrag = TestData.opprettOppdrag(oppdragId = 321))

        every { oppdragsperiodeService.hentAlleOppdragsperiodeMedVedtaksId(vedtakId) } returns listOf(oppdragsperiode)
        every { kravService.erVedlikeholdsmodusPåslått() } returns true

        service.slippVedtakGjennomDriftsavvik(vedtakId)

        verify { oppdragsperiodeService.hentAlleOppdragsperiodeMedVedtaksId(vedtakId) }
        verify(exactly = 0) { kravService.sendKrav(any()) }
    }
}
