package no.nav.bidrag.sak.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.sak.Konvensjon
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.domain.Bidragssak
import no.nav.bidrag.sak.domain.Hendelse
import no.nav.bidrag.sak.repository.HendelseRepository
import no.nav.bidrag.transport.sak.OppdaterSakRequest
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

internal class HendelseServiceTest {
    private val hendelseRepository: HendelseRepository = mockk(relaxed = true)

    private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxed = true)

    private val hendelseService = HendelseService(hendelseRepository, applicationEventPublisher)

    private val bidragSak = Bidragssak("220022", "1701")

    @Test
    fun `opprettHendelser med ny sakskategori oppretter Hendelse for dette`() {
        val slot = slot<Hendelse>()
        every { hendelseRepository.save(capture(slot)) }.answers { slot.captured }

        val forventetHendelse =
            Hendelse(
                saksnummer = "220022",
                type = HendelseType.GJELDER_ENDRET,
                resultat = "Gjelder endret fra Nasjonal til Utland",
                enhet = "1701",
                søknad = null,
            )

        hendelseService.opprettHendelser(
            bidragSak,
            OppdaterSakRequest(saksnummer = Saksnummer("220022"), kategorikode = Sakskategori.UTLAND),
        )

        slot.captured.hendelseId shouldBe forventetHendelse.hendelseId
        slot.captured.enhet shouldBe forventetHendelse.enhet
        slot.captured.saksnummer shouldBe forventetHendelse.saksnummer
        slot.captured.resultat shouldBe forventetHendelse.resultat
    }

    @Test
    fun `opprettHendelser med ny konvensjon oppretter Hendelse for dette`() {
        val slot = slot<Hendelse>()
        every { hendelseRepository.save(capture(slot)) }.answers { slot.captured }

        val forventetHendelse =
            Hendelse(
                saksnummer = "220022",
                type = HendelseType.KONVENSJONSKOДЕ_REGISTRERT,
                resultat = "Konvensjonskode registrert - Haag 1973",
                enhet = "1701",
                søknad = null,
            )

        hendelseService.opprettHendelser(bidragSak, OppdaterSakRequest(saksnummer = Saksnummer("220022"), konvensjonskode = Konvensjon.H73))

        slot.captured.hendelseId shouldBe forventetHendelse.hendelseId
        slot.captured.enhet shouldBe forventetHendelse.enhet
        slot.captured.saksnummer shouldBe forventetHendelse.saksnummer
        slot.captured.resultat shouldBe forventetHendelse.resultat
    }

    @Test
    fun `opprettHendelser med ny referanse oppretter Hendelse for dette`() {
        val slot = slot<Hendelse>()
        every { hendelseRepository.save(capture(slot)) }.answers { slot.captured }

        val forventetHendelse =
            Hendelse(
                saksnummer = "220022",
                type = HendelseType.REFERANSENUMMER_REGISTRERT,
                resultat = "Referansenr.registrert - FfuReferanse her",
                enhet = "1701",
                søknad = null,
            )

        hendelseService.opprettHendelser(
            bidragSak,
            OppdaterSakRequest(saksnummer = Saksnummer("220022"), ffuReferansenr = "FfuReferanse her"),
        )

        slot.captured.hendelseId shouldBe forventetHendelse.hendelseId
        slot.captured.enhet shouldBe forventetHendelse.enhet
        slot.captured.saksnummer shouldBe forventetHendelse.saksnummer
        slot.captured.resultat shouldBe forventetHendelse.resultat
    }

    @Test
    fun `opprettKafkaHendelse sender forrige eierfogd`() {
        val slot = slot<SakHendelsePubliseringEvent>()
        every { applicationEventPublisher.publishEvent(capture(slot)) } returns Unit

        hendelseService.opprettKafkaHendelse(bidragSak, bidragSak, forrigeEierfogd = "4833")

        slot.captured.sakHendelse.forrigeEierfogd shouldBe Enhetsnummer("4833")
    }
}
