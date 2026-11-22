package no.nav.bidrag.behandling.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.bidrag.behandling.consumer.BidragDokumentConsumer
import no.nav.bidrag.behandling.consumer.BidragDokumentProduksjonConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.extensions.BehandlingMetadataDo
import no.nav.bidrag.behandling.database.datamodell.extensions.BehandlingMetadataDo.Companion.NOTATBESTILLINGER
import no.nav.bidrag.behandling.database.datamodell.extensions.Notatbestilling
import no.nav.bidrag.behandling.dto.v2.notat.NotatDel
import no.nav.bidrag.behandling.transformers.behandling.notatTittel
import no.nav.bidrag.behandling.transformers.behandling.tilReferanseId
import no.nav.bidrag.behandling.utils.testdata.oppretteTestbehandling
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakNotatDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NotatOpplysningerServiceOpprettNotatTest {
    private val saksnummer = "1900000"
    private val begrunnelsePdf = "BEGRUNNELSE".toByteArray()
    private val opplysningerPdf = "OPPLYSNINGER".toByteArray()
    private val komplettPdf = "KOMPLETT".toByteArray()

    private val behandlingService = mockk<BehandlingService>()
    private val dokumentProduksjonConsumer = mockk<BidragDokumentProduksjonConsumer>()
    private val dokumentConsumer = mockk<BidragDokumentConsumer>()
    private val notatDto = mockk<VedtakNotatDto>(relaxed = true)
    private val journalpostForespørsler = mutableListOf<OpprettJournalpostRequest>()

    private lateinit var behandling: Behandling
    private lateinit var notatOpplysningerService: NotatOpplysningerService

    @BeforeEach
    fun init() {
        behandling =
            oppretteTestbehandling(setteDatabaseider = true).apply {
                vedtakstidspunkt = LocalDateTime.now()
                vedtakFattetAv = "Z999999"
                vedtakFattetAvEnhet = "4806"
            }
        notatOpplysningerService =
            spyk(NotatOpplysningerService(behandlingService, mockk(), dokumentProduksjonConsumer, dokumentConsumer, mockk()))
        every { notatOpplysningerService.hentNotatOpplysningerForBehandling(any(), any()) } returns notatDto
        every { behandlingService.hentBehandlingById(behandling.id!!) } returns behandling
        every { behandlingService.lagreNotatJournalpostId(any(), any(), any(), any()) } just Runs
        every { dokumentProduksjonConsumer.opprettNotat(any(), NotatDel.KOMPLETT) } returns komplettPdf
        every { dokumentProduksjonConsumer.opprettNotat(any(), NotatDel.BEGRUNNELSE) } returns begrunnelsePdf
        every { dokumentProduksjonConsumer.opprettNotat(any(), NotatDel.OPPLYSNINGER) } returns opplysningerPdf
        every { dokumentConsumer.opprettJournalpost(capture(journalpostForespørsler)) } answers {
            OpprettJournalpostResponse(journalpostId = "jp-${journalpostForespørsler.size}")
        }
    }

    @AfterEach
    fun rydd() {
        clearAllMocks()
    }

    private fun bestill(bestilling: Notatbestilling) {
        behandling.metadata =
            BehandlingMetadataDo.from(mapOf(NOTATBESTILLINGER to commonObjectmapper.writeValueAsString(listOf(bestilling))))
    }

    @Test
    fun `skal opprette ett samlet notat når bestillingen ikke er splittet`() {
        // gitt
        bestill(Notatbestilling(saksnummer))

        // når
        val journalpostId = notatOpplysningerService.opprettNotat(behandling.id!!, saksnummer = saksnummer)

        // så
        journalpostId shouldBe "jp-1"
        verify(exactly = 1) { dokumentProduksjonConsumer.opprettNotat(notatDto, any()) }
        verify(exactly = 1) { dokumentProduksjonConsumer.opprettNotat(notatDto, NotatDel.KOMPLETT) }
        journalpostForespørsler shouldHaveSize 1
        assertSoftly(journalpostForespørsler.single()) {
            referanseId shouldBe behandling.tilReferanseId(saksnummer)
            tilknyttSaker shouldBe listOf(saksnummer)
            dokumenter.single().tittel shouldBe behandling.notatTittel()
            dokumenter.single().fysiskDokument shouldBe komplettPdf
        }
        verify(exactly = 1) { behandlingService.lagreNotatJournalpostId(behandling.id!!, "jp-1", saksnummer, NotatDel.KOMPLETT) }
    }

    @Test
    fun `skal opprette samlet notat for gammel behandling uten notatbestillinger`() {
        // når
        val journalpostId = notatOpplysningerService.opprettNotat(behandling.id!!)

        // så
        journalpostId shouldBe "jp-1"
        verify(exactly = 1) { dokumentProduksjonConsumer.opprettNotat(notatDto, NotatDel.KOMPLETT) }
        journalpostForespørsler.single().referanseId shouldBe behandling.tilReferanseId()
        verify(exactly = 1) { behandlingService.lagreNotatJournalpostId(behandling.id!!, "jp-1", null, NotatDel.KOMPLETT) }
    }

    @Test
    fun `skal opprette begrunnelse og opplysninger som separate journalposter når bestillingen er splittet`() {
        // gitt
        bestill(Notatbestilling(saksnummer, splittet = true))

        // når
        val journalpostId = notatOpplysningerService.opprettNotat(behandling.id!!, saksnummer = saksnummer)

        // så
        journalpostId shouldBe "jp-1"
        verify(exactly = 1) { notatOpplysningerService.hentNotatOpplysningerForBehandling(behandling, saksnummer) }
        verify(exactly = 0) { dokumentProduksjonConsumer.opprettNotat(any(), NotatDel.KOMPLETT) }
        journalpostForespørsler shouldHaveSize 2
        assertSoftly(journalpostForespørsler.first()) {
            referanseId shouldBe "${behandling.tilReferanseId(saksnummer)}_begrunnelse"
            dokumenter.single().tittel shouldBe behandling.notatTittel()
            dokumenter.single().fysiskDokument shouldBe begrunnelsePdf
        }
        assertSoftly(journalpostForespørsler.last()) {
            referanseId shouldBe "${behandling.tilReferanseId(saksnummer)}_opplysninger"
            dokumenter.single().tittel shouldBe "${behandling.notatTittel()} - opplysninger fra offentlige registre"
            dokumenter.single().fysiskDokument shouldBe opplysningerPdf
        }
        verifyOrder {
            dokumentProduksjonConsumer.opprettNotat(notatDto, NotatDel.BEGRUNNELSE)
            dokumentConsumer.opprettJournalpost(any())
            behandlingService.lagreNotatJournalpostId(behandling.id!!, "jp-1", saksnummer, NotatDel.BEGRUNNELSE)
            dokumentProduksjonConsumer.opprettNotat(notatDto, NotatDel.OPPLYSNINGER)
            dokumentConsumer.opprettJournalpost(any())
            behandlingService.lagreNotatJournalpostId(behandling.id!!, "jp-2", saksnummer, NotatDel.OPPLYSNINGER)
        }
    }

    @Test
    fun `skal kun opprette opplysninger når begrunnelsen allerede er journalført`() {
        // gitt
        bestill(Notatbestilling(saksnummer, splittet = true, begrunnelseJournalpostId = "begrunnelse-1"))

        // når
        val journalpostId = notatOpplysningerService.opprettNotat(behandling.id!!, saksnummer = saksnummer)

        // så
        journalpostId shouldBe "begrunnelse-1"
        verify(exactly = 0) { dokumentProduksjonConsumer.opprettNotat(any(), NotatDel.BEGRUNNELSE) }
        verify(exactly = 1) { dokumentProduksjonConsumer.opprettNotat(notatDto, NotatDel.OPPLYSNINGER) }
        journalpostForespørsler.single().referanseId shouldBe "${behandling.tilReferanseId(saksnummer)}_opplysninger"
        verify(exactly = 1) { behandlingService.lagreNotatJournalpostId(any(), any(), any(), any()) }
        verify(exactly = 1) { behandlingService.lagreNotatJournalpostId(behandling.id!!, "jp-1", saksnummer, NotatDel.OPPLYSNINGER) }
    }

    @Test
    fun `skal ikke opprette noe når splittet notat er fullført`() {
        // gitt
        bestill(Notatbestilling(saksnummer, splittet = true, begrunnelseJournalpostId = "begrunnelse-1", opplysningerJournalpostId = "opplysninger-1"))

        // når
        val journalpostId = notatOpplysningerService.opprettNotatManuelt(behandling.id!!)

        // så
        journalpostId shouldBe "begrunnelse-1"
        verify(exactly = 0) { notatOpplysningerService.hentNotatOpplysningerForBehandling(any(), any()) }
        verify(exactly = 0) { dokumentProduksjonConsumer.opprettNotat(any(), any()) }
        verify(exactly = 0) { dokumentConsumer.opprettJournalpost(any()) }
        verify(exactly = 0) { behandlingService.lagreNotatJournalpostId(any(), any(), any(), any()) }
    }
}
