package no.nav.bidrag.behandling.database.repository

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.bidrag.behandling.TestContainerRunner
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.Omgjøringsdetaljer
import no.nav.bidrag.behandling.utils.testdata.oppretteTestbehandling
import no.nav.bidrag.behandling.utils.testdata.testdataBP
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

private const val OPPRINNELIG_VEDTAKSID = 990001
private const val OMGJORT_VEDTAKSID_I_HOVEDBEHANDLING = 990002
private const val ANNET_OMGJORT_VEDTAKSID_I_KJEDEN = 990003

@Transactional
class BehandlingRepositoryFinnHovedbehandlingTest : TestContainerRunner() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `skal finne hovedbehandling for FF når den omgjør samme vedtak`() {
        // Given
        val hovedbehandling = lagreKlageHovedbehandlingForFF()

        // When
        val funnet =
            behandlingRepository.finnHovedbehandlingForBpVedFF(
                testdataBP.ident,
                OPPRINNELIG_VEDTAKSID,
                OMGJORT_VEDTAKSID_I_HOVEDBEHANDLING,
            )

        // Then
        funnet.shouldNotBeNull()
        funnet.id shouldBe hovedbehandling.id
    }

    @Test
    fun `skal ikke finne hovedbehandling for FF når den omgjør et annet vedtak i samme omgjøringskjede`() {
        // Given
        lagreKlageHovedbehandlingForFF()

        // When
        val funnet =
            behandlingRepository.finnHovedbehandlingForBpVedFF(
                testdataBP.ident,
                OPPRINNELIG_VEDTAKSID,
                ANNET_OMGJORT_VEDTAKSID_I_KJEDEN,
            )

        // Then
        funnet.shouldBeNull()
    }

    @Test
    fun `skal finne hovedbehandling for FF uavhengig av omgjort vedtak når omgjørVedtakId ikke er satt`() {
        // Given
        val hovedbehandling = lagreKlageHovedbehandlingForFF()

        // When
        val funnet = behandlingRepository.finnHovedbehandlingForBpVedFF(testdataBP.ident, OPPRINNELIG_VEDTAKSID)

        // Then
        funnet.shouldNotBeNull()
        funnet.id shouldBe hovedbehandling.id
    }

    private fun lagreKlageHovedbehandlingForFF(): Behandling {
        val behandling = oppretteTestbehandling(inkludereBp = true, behandlingstype = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        behandling.omgjøringsdetaljer =
            Omgjøringsdetaljer(
                opprinneligVedtakId = OPPRINNELIG_VEDTAKSID,
                omgjørVedtakId = OMGJORT_VEDTAKSID_I_HOVEDBEHANDLING,
            )
        return behandlingRepository.save(behandling)
    }
}
