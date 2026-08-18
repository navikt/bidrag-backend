package no.nav.bidrag.regnskap.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.BidragVedtakConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.persistence.repository.OppdragRepository
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.behandling.vedtak.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class PatchServiceTest {

    @RelaxedMockK
    private lateinit var oppdragsperiodeRepository: OppdragsperiodeRepository

    @RelaxedMockK
    private lateinit var oppdragRepository: OppdragRepository

    @RelaxedMockK
    private lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @RelaxedMockK
    private lateinit var identUtils: IdentUtils

    @InjectMockKs
    private lateinit var patchService: PatchService

    @BeforeEach
    fun setUp() {
        every { identUtils.hentNyesteIdent(any()) } returnsArgument 0
    }

    @Test
    fun skalOppdatereTommeReferanser() {
        val oppdrag = TestData.opprettOppdrag(stonadType = null, engangsbelopType = Engangsbeløptype.GEBYR_MOTTAKER)
        val gebyrBm = TestData.opprettOppdragsperiode(oppdrag = oppdrag, referanse = "")
        oppdrag.oppdragsperioder = listOf(gebyrBm)

        every { oppdragsperiodeRepository.save<Oppdragsperiode>(any()) } returns gebyrBm

        every { oppdragsperiodeRepository.findAllByReferanse("") } returns oppdrag.oppdragsperioder
        val engangsbeløp = EngangsbeløpDto(
            Engangsbeløptype.GEBYR_MOTTAKER,
            Saksnummer(oppdrag.sakId),
            Personident(oppdrag.skyldnerIdent),
            Personident(oppdrag.kravhaverIdent!!),
            Personident(oppdrag.mottakerIdent),
            null,
            null,
            "123",
            Innkrevingstype.MED_INNKREVING,
            Beslutningstype.STADFESTELSE,
            null,
            "123",
            null,
            null,
            emptyList(),
            null,
        )
        every { bidragVedtakConsumer.hentVedtak(any()) } returns VedtakDto(
            kilde = Vedtakskilde.AUTOMATISK,
            type = Vedtakstype.FASTSETTELSE,
            vedtaksid = 123,
            opprettetAv = "H123",
            opprettetAvNavn = "Test User",
            kildeapplikasjon = "TestApp",
            vedtakstidspunkt = LocalDateTime.now(),
            unikReferanse = "12345",
            enhetsnummer = Enhetsnummer("1234"),
            innkrevingUtsattTilDato = null,
            fastsattILand = null,
            opprettetTidspunkt = LocalDateTime.now(),
            grunnlagListe = emptyList(),
            stønadsendringListe = emptyList(),
            engangsbeløpListe = listOf(engangsbeløp),
            behandlingsreferanseListe = emptyList(),
        )

        patchService.patchTommeReferanser()

        verify(exactly = 1) { oppdragsperiodeRepository.save(any(Oppdragsperiode::class)) }
    }

    @Test
    fun skalLoggeFeilOmIngenEngangsbeløpMatcher() {
        val oppdrag = TestData.opprettOppdrag(stonadType = null, engangsbelopType = Engangsbeløptype.GEBYR_MOTTAKER)
        val gebyrBm = TestData.opprettOppdragsperiode(oppdrag = oppdrag, referanse = "")
        oppdrag.oppdragsperioder = listOf(gebyrBm)

        every { oppdragsperiodeRepository.save<Oppdragsperiode>(any()) } returns gebyrBm

        every { oppdragsperiodeRepository.findAllByReferanse("") } returns oppdrag.oppdragsperioder
        val engangsbeløp = EngangsbeløpDto(
            Engangsbeløptype.GEBYR_MOTTAKER,
            Saksnummer(oppdrag.sakId),
            Personident("1234321"),
            Personident(oppdrag.kravhaverIdent!!),
            Personident(oppdrag.mottakerIdent),
            null,
            null,
            "123",
            Innkrevingstype.MED_INNKREVING,
            Beslutningstype.STADFESTELSE,
            null,
            "123",
            null,
            null,
            emptyList(),
            null,
        )
        every { bidragVedtakConsumer.hentVedtak(any()) } returns VedtakDto(
            kilde = Vedtakskilde.AUTOMATISK,
            type = Vedtakstype.FASTSETTELSE,
            vedtaksid = 123,
            opprettetAv = "H123",
            opprettetAvNavn = "Test User",
            kildeapplikasjon = "TestApp",
            vedtakstidspunkt = LocalDateTime.now(),
            unikReferanse = "12345",
            enhetsnummer = Enhetsnummer("1234"),
            innkrevingUtsattTilDato = null,
            fastsattILand = null,
            opprettetTidspunkt = LocalDateTime.now(),
            grunnlagListe = emptyList(),
            stønadsendringListe = emptyList(),
            engangsbeløpListe = listOf(engangsbeløp),
            behandlingsreferanseListe = emptyList(),
        )

        patchService.patchTommeReferanser()

        verify(exactly = 0) { oppdragsperiodeRepository.save(any(Oppdragsperiode::class)) }
    }

    @Test
    fun skalLoggeFeilOmFlereEnnEtEngangsbeløpMatcher() {
        val oppdrag = TestData.opprettOppdrag(stonadType = null, engangsbelopType = Engangsbeløptype.GEBYR_MOTTAKER)
        val gebyrBm = TestData.opprettOppdragsperiode(oppdrag = oppdrag, referanse = "")
        oppdrag.oppdragsperioder = listOf(gebyrBm)

        every { oppdragsperiodeRepository.save<Oppdragsperiode>(any()) } returns gebyrBm

        every { oppdragsperiodeRepository.findAllByReferanse("") } returns oppdrag.oppdragsperioder
        val engangsbeløp = EngangsbeløpDto(
            Engangsbeløptype.GEBYR_MOTTAKER,
            Saksnummer(oppdrag.sakId),
            Personident(oppdrag.skyldnerIdent),
            Personident(oppdrag.kravhaverIdent!!),
            Personident(oppdrag.mottakerIdent),
            null,
            null,
            "123",
            Innkrevingstype.MED_INNKREVING,
            Beslutningstype.STADFESTELSE,
            null,
            "123",
            null,
            null,
            emptyList(),
            null,
        )
        every { bidragVedtakConsumer.hentVedtak(any()) } returns VedtakDto(
            kilde = Vedtakskilde.AUTOMATISK,
            type = Vedtakstype.FASTSETTELSE,
            vedtaksid = 123,
            opprettetAv = "H123",
            opprettetAvNavn = "Test User",
            kildeapplikasjon = "TestApp",
            vedtakstidspunkt = LocalDateTime.now(),
            unikReferanse = "12345",
            enhetsnummer = Enhetsnummer("1234"),
            innkrevingUtsattTilDato = null,
            fastsattILand = null,
            opprettetTidspunkt = LocalDateTime.now(),
            grunnlagListe = emptyList(),
            stønadsendringListe = emptyList(),
            engangsbeløpListe = listOf(engangsbeløp, engangsbeløp),
            behandlingsreferanseListe = emptyList(),
        )

        patchService.patchTommeReferanser()

        verify(exactly = 0) { oppdragsperiodeRepository.save(any(Oppdragsperiode::class)) }
    }
}
