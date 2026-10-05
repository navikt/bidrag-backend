package no.nav.bidrag.arbeidsflyt.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.arbeidsflyt.consumer.BidragSakConsumer
import no.nav.bidrag.arbeidsflyt.persistence.entity.Sak
import no.nav.bidrag.arbeidsflyt.persistence.repository.SakRepository
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandleSakHendelseServiceTest {
    private val sakConsumer = mockk<BidragSakConsumer>()
    private val sakRepository = mockk<SakRepository>()
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val service = BehandleSakHendelseService(sakConsumer, sakRepository, oppgaveService)

    init {
        every { sakRepository.save(any<Sak>()) } answers { firstArg() }
    }

    private fun sakDto(kategori: Sakskategori) = BidragssakDto(
        eierfogd = Enhetsnummer("4806"),
        saksnummer = Saksnummer("123"),
        saksstatus = Bidragssakstatus.IN,
        kategori = kategori,
        opprettetDato = LocalDate.now(),
        levdeAdskilt = false,
        ukjentPart = false,
    )

    private fun hendelse(type: SakKafkaHendelsestype) = SakHendelse(Saksnummer("123"), type)

    @Test
    fun `skal ignorere opprettelse`() {
        service.behandleHendelse(hendelse(SakKafkaHendelsestype.OPPRETTELSE))

        verify(exactly = 0) { sakConsumer.hentSakUtenCache(any()) }
        verify(exactly = 0) { sakRepository.save(any()) }
    }

    @Test
    fun `skal lagre ny sak uten å endre oppgaver`() {
        every { sakConsumer.hentSakUtenCache("123") } returns sakDto(Sakskategori.NASJONAL)
        every { sakRepository.findBySaksnummer("123") } returns null

        service.behandleHendelse(hendelse(SakKafkaHendelsestype.ENDRING))

        verify { sakRepository.save(match { it.kategori == Sakskategori.NASJONAL && it.eierfogd == "4806" }) }
        verify(exactly = 0) { oppgaveService.endreBehandlingstypeForSak(any(), any()) }
    }

    @Test
    fun `skal endre behandlingstype på oppgaver når kategori endres`() {
        val dto = sakDto(Sakskategori.UTLAND)
        every { sakConsumer.hentSakUtenCache("123") } returns dto
        every { sakRepository.findBySaksnummer("123") } returns
            Sak(
                saksnummer = "123",
                sak = sakDto(Sakskategori.NASJONAL),
                kategori = Sakskategori.NASJONAL,
                eierfogd = "4806",
                saksstatus = Bidragssakstatus.IN,
                arbeidsfordeling = Arbeidsfordeling.EIERENHET,
                opprettetDato = LocalDate.now(),
            )

        service.behandleHendelse(hendelse(SakKafkaHendelsestype.ENDRING))

        verify { sakRepository.save(match { it.kategori == Sakskategori.UTLAND }) }
        verify { oppgaveService.endreBehandlingstypeForSak("123", Sakskategori.UTLAND) }
    }
}
