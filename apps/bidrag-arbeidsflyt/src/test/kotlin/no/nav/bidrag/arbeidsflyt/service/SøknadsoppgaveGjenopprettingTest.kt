package no.nav.bidrag.arbeidsflyt.service

import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.arbeidsflyt.consumer.BidragBBMConsumer
import no.nav.bidrag.arbeidsflyt.consumer.BidragBehandlingConsumer
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.dto.METADATA_NØKKEL_BEHANDLING_ID
import no.nav.bidrag.arbeidsflyt.dto.METADATA_NØKKEL_SØKNAD_ID
import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.model.OppgaverForHendelse
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import java.time.LocalDate

class SøknadsoppgaveGjenopprettingTest {
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val bbmConsumer = mockk<BidragBBMConsumer>()
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val behandlingHendelseService =
        BehandleBehandlingHendelseService(
            oppgaveService = oppgaveService,
            sakConsumer = mockk<BidragSakConsumer>(relaxed = true),
            bbmConsumer = bbmConsumer,
            behandlingConsumer = mockk<BidragBehandlingConsumer>(relaxed = true),
            persistenceService = mockk<PersistenceService>(relaxed = true),
            behandlingService = behandlingService,
        )
    private val oppgaveHendelseService =
        BehandleOppgaveHendelseService(
            persistenceService = mockk(relaxed = true),
            oppgaveService = oppgaveService,
            journalpostService = mockk(relaxed = true),
            applicationContext = mockk<ApplicationContext>(relaxed = true),
            arbeidsfordelingService = mockk(relaxed = true),
            behandlingService = behandlingService,
            behandlingHendelseService = behandlingHendelseService,
            bbmConsumer = bbmConsumer,
            meterRegistry = mockk<MeterRegistry>(relaxed = true),
        )

    @Test
    fun `gjenoppretter oppgave når avsluttet søknad fortsatt krever oppgave`() {
        val oppgave = søknadsoppgave()
        every { bbmConsumer.hentSøknad(any()) } returns HentSøknadResponse(søknad())
        every {
            oppgaveService.finnOppgaverForSøknad(any(), any(), any(), any(), any())
        } returns OppgaverForHendelse(emptyList())

        oppgaveHendelseService.opprettSøknadsoppgaveHvisBehandlingIkkeAvsluttet(oppgave)

        verify { oppgaveService.opprettOppgave(any()) }
    }

    @Test
    fun `ferdigstiller ikke oppgave når avsluttet søknad fortsatt krever oppgave`() {
        every { oppgaveService.finnOppgaverForBehandling(123L) } returns listOf(søknadsoppgave())
        every { bbmConsumer.hentSøknad(HentSøknadRequest(456L)) } returns HentSøknadResponse(søknad())

        behandlingHendelseService.ferdigstillSøknadsoppgaverForSøknadSomErSlettet(123L)

        verify(exactly = 0) { oppgaveService.oppdaterOppgave(any()) }
    }

    private fun søknadsoppgave() = OppgaveData(
        id = 1L,
        saksreferanse = "123456",
        beskrivelse = "Behandle søknad",
        fristFerdigstillelse = LocalDate.parse("2026-01-01"),
        opprettetAvEnhetsnr = "4806",
        oppgavetype = "BEH_SAK",
        tema = "BID",
        status = OppgaveStatus.FERDIGSTILT,
        metadata = mapOf(METADATA_NØKKEL_SØKNAD_ID to "456", METADATA_NØKKEL_BEHANDLING_ID to "123"),
    )

    private fun søknad() = HentSøknad(
        søknadsid = 456L,
        søknadMottattDato = LocalDate.parse("2026-01-01"),
        behandlingstema = Behandlingstema.BIDRAG,
        saksnummer = "123456",
        innkreving = true,
        søktAvType = SøktAvType.BIDRAGSMOTTAKER,
        behandlingStatusType = BehandlingStatusType.VEDTAK_FATTET,
        partISøknadListe =
        listOf(
            PartISøknad(
                personident = "12345678901",
                rolletype = Rolletype.BARN,
                behandlingstatus = Behandlingstatus.MIDLERTIDLIG_VEDTAK,
            ),
        ),
    )
}
