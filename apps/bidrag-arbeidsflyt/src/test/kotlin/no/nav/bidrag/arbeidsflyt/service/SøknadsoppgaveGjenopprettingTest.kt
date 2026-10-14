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
import no.nav.bidrag.arbeidsflyt.persistence.entity.Behandling
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseBarn
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelseType
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.dokument.Sporingsdata
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Test
    fun `ferdigstiller ikke oppgave ved ny hendelse når behandling allerede er avsluttet og oppgave kreves`() {
        val hendelse = behandlingHendelse()
        every { behandlingService.finnForBehandlingsidEllerSøknadsid(123L, 456L) } returns
            Behandling(
                id = 1L,
                behandlingsid = 123L,
                søknadsid = 456L,
                status = BehandlingStatusType.VEDTAK_FATTET,
                mottattDato = LocalDate.parse("2026-01-01"),
                enhet = "4806",
            )
        every { bbmConsumer.hentSøknad(HentSøknadRequest(456L)) } returns HentSøknadResponse(søknad())
        every {
            oppgaveService.finnOppgaverForSøknad(any(), any(), any(), any(), any())
        } returns OppgaverForHendelse(listOf(søknadsoppgave()))

        behandlingHendelseService.behandleHendelse(hendelse)

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

    private fun behandlingHendelse() = BehandlingHendelse(
        type = BehandlingHendelseType.ENDRET,
        status = BehandlingStatusType.VEDTAK_FATTET,
        vedtakstype = Vedtakstype.ENDRING,
        behandlingsid = 123L,
        opprettetTidspunkt = LocalDateTime.now(),
        endretTidspunkt = LocalDateTime.now(),
        behandlerEnhet = "4806",
        søknadsid = 456L,
        mottattDato = LocalDate.parse("2026-01-01"),
        sporingsdata = Sporingsdata("test", "test", "test", enhetsnummer = "4806"),
        barn =
        listOf(
            BehandlingHendelseBarn(
                saksnummer = "123456",
                behandlingstype = Behandlingstype.ENDRING,
                behandlingstema = Behandlingstema.BIDRAG,
                status = Behandlingstatus.MIDLERTIDLIG_VEDTAK,
                stønadstype = Stønadstype.BIDRAG,
                engangsbeløptype = null,
                søktAv = SøktAvType.BIDRAGSMOTTAKER,
                søktFraDato = LocalDate.parse("2026-01-01"),
                ident = "12345678901",
                søknadsid = 456L,
                behandlerEnhet = "4806",
            ),
        ),
    )
}
