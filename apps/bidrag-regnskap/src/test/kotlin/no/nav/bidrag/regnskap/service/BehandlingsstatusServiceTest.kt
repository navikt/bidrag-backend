package no.nav.bidrag.regnskap.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.domene.enums.regnskap.behandlingsstatus.Batchstatus
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehandlingsstatusServiceTest {

    private val skattConsumer: SkattConsumer = mockk()
    private val persistenceService: PersistenceService = mockk()
    private val reskontroService: ReskontroService = mockk()
    private lateinit var behandlingsstatusService: BehandlingsstatusService

    @BeforeEach
    fun setUp() {
        every { reskontroService.sammenlignOversendteKonteringerMedReskontro(any()) } returns hashMapOf()
        behandlingsstatusService = BehandlingsstatusService(skattConsumer, persistenceService, reskontroService)
    }

    @Test
    fun `skal hente konteringer med ikke godkjent behandlingsstatus`() {
        val referanse1 = "referanse1"
        val referanse2 = "referanse2"

        val kontering1 = TestData.opprettKontering(sisteReferansekode = referanse1)
        val kontering2 = TestData.opprettKontering(sisteReferansekode = referanse2)

        every { persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk() } returns listOf(kontering1, kontering2)

        val resultat = behandlingsstatusService.hentKonteringerMedIkkeGodkjentBehandlingsstatus()

        assertEquals(2, resultat.size)
        assertTrue(resultat[referanse1]?.contains(kontering1) ?: false)
        assertTrue(resultat[referanse2]?.contains(kontering2) ?: false)

        verify { persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk() }
    }

    @Test
    fun `skal returnere tom map nar det ikke finnes konteringer`() {
        every { persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk() } returns emptyList()

        val resultat = behandlingsstatusService.hentKonteringerMedIkkeGodkjentBehandlingsstatus()

        assertTrue(resultat.isEmpty())

        verify { persistenceService.hentAlleKonteringerUtenBehandlingsstatusOk() }
    }

    @Test
    fun `skal oppdatere konteringer som er godkjent`() {
        val batchUid = "batch1"
        val oppdrag = TestData.opprettOppdrag(harFeiledeKonteringer = true)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag, konteringer = listOf())
        val kontering = TestData.opprettKontering(
            sisteReferansekode = batchUid,
            oppdragsperiode = oppdragsperiode,
        )
        oppdragsperiode.konteringer = listOf(kontering)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)
        val konteringer = mutableSetOf(kontering)
        every { persistenceService.konteringRepository.findAllBySisteReferansekode(batchUid) } returns listOf(kontering)
        val behandlingsstatusResponse = BehandlingsstatusResponse(emptyList(), Batchstatus.Done, batchUid, 1, 0, 1)

        every { skattConsumer.sjekkBehandlingsstatus(batchUid).body } returns behandlingsstatusResponse

        val resultat = behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringer(
            hashMapOf(batchUid to konteringer),
        )

        assertTrue(resultat.isEmpty())
        assertNotNull(kontering.behandlingsstatusOkTidspunkt)
        verify { skattConsumer.sjekkBehandlingsstatus(batchUid) }
    }

    @Test
    fun `skal returnere feilmelding hvis batch ikke er ferdig`() {
        val batchUid = "batch1"
        val oppdrag = TestData.opprettOppdrag(harFeiledeKonteringer = false)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering = TestData.opprettKontering(
            sisteReferansekode = batchUid,
            oppdragsperiode = oppdragsperiode,
        )
        oppdragsperiode.konteringer = listOf(kontering)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)
        val konteringer = mutableSetOf(kontering)
        every { persistenceService.konteringRepository.findAllBySisteReferansekode(batchUid) } returns listOf(kontering)
        val behandlingsstatusResponse = BehandlingsstatusResponse(emptyList(), Batchstatus.Failed, batchUid, 1, 1, 0)

        every { skattConsumer.sjekkBehandlingsstatus(batchUid).body } returns behandlingsstatusResponse

        val resultat = behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringer(
            hashMapOf(batchUid to konteringer),
        )

        assertEquals(1, resultat.size)
        assertEquals(
            "Behandling av konteringer for batchuid $batchUid har feilet: $behandlingsstatusResponse\n",
            resultat[batchUid],
        )
        verify { skattConsumer.sjekkBehandlingsstatus(batchUid) }
    }

    @Test
    fun `skal håndtere exception ved henting av behandlingsstatus`() {
        val batchUid = "batch1"
        val oppdrag = TestData.opprettOppdrag(harFeiledeKonteringer = false)
        val oppdragsperiode = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
        val kontering = TestData.opprettKontering(
            sisteReferansekode = batchUid,
            oppdragsperiode = oppdragsperiode,
        )
        oppdragsperiode.konteringer = listOf(kontering)
        oppdrag.oppdragsperioder = listOf(oppdragsperiode)
        val konteringer = mutableSetOf(kontering)
        every { persistenceService.konteringRepository.findAllBySisteReferansekode(batchUid) } returns listOf(kontering)

        every { skattConsumer.sjekkBehandlingsstatus(batchUid).body } throws RuntimeException("Simulert feil")

        val resultat = behandlingsstatusService.hentBehandlingsstatusForIkkeGodkjenteKonteringer(
            hashMapOf(batchUid to konteringer),
        )

        assertEquals(1, resultat.size)
        assertTrue(resultat[batchUid]!!.contains("Simulert feil"))
        verify { skattConsumer.sjekkBehandlingsstatus(batchUid) }
    }
}
