package no.nav.bidrag.behandling.database.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.persistence.EntityManager
import no.nav.bidrag.behandling.TestContainerRunner
import no.nav.bidrag.behandling.controller.v2.AdminController
import no.nav.bidrag.behandling.database.datamodell.extensions.BehandlingMetadataDo
import no.nav.bidrag.behandling.database.datamodell.extensions.BehandlingMetadataDo.Companion.FF_SIST_SYNKRONISERT
import no.nav.bidrag.behandling.database.datamodell.extensions.BehandlingMetadataDo.Companion.OPPRETTELSE_AV_FF_BEHANDLING_FEILET
import no.nav.bidrag.behandling.database.datamodell.extensions.LasterGrunnlagAsyncStatus
import no.nav.bidrag.behandling.dto.v1.behandling.OpprettBehandlingRequest
import no.nav.bidrag.behandling.utils.testdata.oppretteTestbehandling
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
class BehandlingMetadataTest : TestContainerRunner() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var adminController: AdminController

    @Autowired
    lateinit var entityManager: EntityManager

    private val sistSynkronisert = LocalDateTime.of(2025, 1, 1, 12, 0)

    @Test
    fun `skal lagre reset av grunnlagsstatus uten å overskrive andre metadata`() {
        val behandling = oppretteTestbehandling()
        behandling.metadata = BehandlingMetadataDo.from(
            mapOf(
                FF_SIST_SYNKRONISERT to sistSynkronisert.toString(),
                "laster_grunnlag_async_status" to LasterGrunnlagAsyncStatus.BESTILT.name,
                "laster_grunnlag_async_tidspunkt" to sistSynkronisert.toString(),
            ),
        )
        behandling.grunnlagSistInnhentet = sistSynkronisert
        val id = requireNotNull(behandlingRepository.save(behandling).id)
        entityManager.flush()
        entityManager.clear()

        adminController.resetHentGrunnlag(id)
        entityManager.clear()

        val oppdatert = behandlingRepository.findBehandlingById(id).get()
        oppdatert.grunnlagSistInnhentet shouldBe null
        val metadata = requireNotNull(oppdatert.metadata)
        metadata["laster_grunnlag_async_status"] shouldBe LasterGrunnlagAsyncStatus.FERDIG.name
        val tidspunkt = requireNotNull(metadata["laster_grunnlag_async_tidspunkt"])
        tidspunkt shouldNotBe sistSynkronisert.toString()
        metadata.getFFSistSynkronisert() shouldBe sistSynkronisert
    }

    @Test
    fun `skal lagre nullstilling av FF feilflagget uten å overskrive andre metadata`() {
        val behandling = oppretteTestbehandling()
        behandling.metadata = BehandlingMetadataDo.from(mapOf(FF_SIST_SYNKRONISERT to sistSynkronisert.toString()))
        val id = requireNotNull(behandlingRepository.save(behandling).id)
        entityManager.flush()
        behandlingRepository.markerOpprettelseAvFFFeilet(id)
        entityManager.clear()
        behandlingRepository.findBehandlingById(id).get().metadata?.getOpprettelseEllerOppdateringAvFFFeilet() shouldBe true

        behandlingRepository.resetOpprettelseAvFFFeilet(id)
        entityManager.clear()

        val metadata = requireNotNull(behandlingRepository.findBehandlingById(id).get().metadata)
        metadata.getOpprettelseEllerOppdateringAvFFFeilet() shouldBe false
        metadata.getFFSistSynkronisert() shouldBe sistSynkronisert
    }

    @Test
    fun `skal lese FF opprettelsesfeil fra begge nøkler etter lagring`() {
        val gammelNøkkel = "opprettelseAvFFRollerFeilet"
        val gammelFeil = OpprettBehandlingRequest(
            vedtakstype = Vedtakstype.FASTSETTELSE,
            søktFomDato = sistSynkronisert.toLocalDate(),
            mottattdato = LocalDate.of(2025, 1, 3),
            søknadFra = SøktAvType.BIDRAGSMOTTAKER,
            saksnummer = "1900000",
            behandlerenhet = "4806",
            roller = emptySet(),
            søknadsid = 1,
        )
        val nyFeil = gammelFeil.copy(søknadsid = 2)
        val behandling = oppretteTestbehandling()
        val gammelVerdi = commonObjectmapper.writeValueAsString(setOf(gammelFeil))
        behandling.metadata = BehandlingMetadataDo.from(mapOf(gammelNøkkel to gammelVerdi))
        val id = requireNotNull(behandlingRepository.save(behandling).id)
        val nyVerdi = commonObjectmapper.writeValueAsString(setOf(nyFeil))

        behandlingRepository.markerOpprettelseAvFFBehandlingFeilet(id, nyVerdi)
        entityManager.clear()

        val metadata = requireNotNull(behandlingRepository.findBehandlingById(id).get().metadata)
        metadata[OPPRETTELSE_AV_FF_BEHANDLING_FEILET] shouldBe nyVerdi
        metadata[gammelNøkkel] shouldBe gammelVerdi
        metadata.getOppprettelseAvBehandlingFeilet() shouldBe setOf(gammelFeil, nyFeil)
    }
}
