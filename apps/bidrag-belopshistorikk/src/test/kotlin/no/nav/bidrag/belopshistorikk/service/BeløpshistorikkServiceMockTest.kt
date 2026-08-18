package no.nav.bidrag.belopshistorikk.service

import no.nav.bidrag.belopshistorikk.TestUtil.Companion.KRAVHAVER_IDENT
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.MOTTAKER_IDENT
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.SKYLDNER_IDENT
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.byggEngangsbeløpRequest
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.byggEngangsbeløpResponseFlereEngangsbeløp
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.byggStønadRequest
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.byggStønadResponseFlereStønader
import no.nav.bidrag.belopshistorikk.bo.PeriodeBo
import no.nav.bidrag.belopshistorikk.service.BeløpshistorikkServiceMockTest.MockitoHelper.any
import no.nav.bidrag.belopshistorikk.service.BeløpshistorikkServiceMockTest.MockitoHelper.capture
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettEngangsbeløpRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadRequestDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
class BeløpshistorikkServiceMockTest {
    @InjectMocks
    private lateinit var beløpshistorikkService: BeløpshistorikkService

    @Mock
    private lateinit var persistenceServiceMock: PersistenceService

    @Mock
    private lateinit var identUtilsMock: IdentUtils

    @Captor
    private lateinit var opprettStønadRequestDto: ArgumentCaptor<OpprettStønadRequestDto>

    @Captor
    private lateinit var opprettEngangsbeløpRequestDto: ArgumentCaptor<OpprettEngangsbeløpRequestDto>

    @Captor
    private lateinit var periodeBoCaptor: ArgumentCaptor<PeriodeBo>

    @Test
    fun `skal opprette ny komplett stønad`() {
        `when`(persistenceServiceMock.opprettStønad(capture(opprettStønadRequestDto))).thenReturn(1)
        doNothing().`when`(persistenceServiceMock).opprettPeriode(capture(periodeBoCaptor), eq(1))

        val nyStønadOpprettetStønadId = beløpshistorikkService.opprettStønad(byggStønadRequest())

        val stønadDto = opprettStønadRequestDto.value
        val periodeDtoListe = periodeBoCaptor.allValues

        Mockito.verify(persistenceServiceMock, Mockito.times(1))
            .opprettStønad(any(OpprettStønadRequestDto::class.java))

        assertAll(
            { assertThat(nyStønadOpprettetStønadId).isNotNull() },
            // Sjekk stønadDto
            { assertThat(stønadDto).isNotNull() },
            { assertThat(stønadDto.type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(stønadDto.sak).isEqualTo(Saksnummer("SAK-001")) },
            { assertThat(stønadDto.skyldner).isEqualTo(Personident(SKYLDNER_IDENT)) },
            { assertThat(stønadDto.kravhaver).isEqualTo(Personident(KRAVHAVER_IDENT)) },
            { assertThat(stønadDto.mottaker).isEqualTo(Personident(MOTTAKER_IDENT)) },
            { assertThat(stønadDto.opprettetAv).isEqualTo("X123456") },
            { assertThat(stønadDto.nesteIndeksreguleringsår).isEqualTo(2024) },
            { assertThat(stønadDto.innkreving).isEqualTo(Innkrevingstype.MED_INNKREVING) },
            // Sjekk PeriodeDto
            { assertThat(periodeDtoListe).isNotNull() },
            { assertThat(periodeDtoListe.size).isEqualTo(2) },
            { assertThat(periodeDtoListe[0].periode.fom).isEqualTo(YearMonth.parse("2019-01")) },
            { assertThat(periodeDtoListe[0].periode.til).isEqualTo(YearMonth.parse("2019-07")) },
            { assertThat(periodeDtoListe[0].vedtaksid).isEqualTo(321) },
            { assertThat(periodeDtoListe[0].beløp).isEqualTo(BigDecimal.valueOf(3490)) },
            { assertThat(periodeDtoListe[0].valutakode).isEqualTo("NOK") },
            { assertThat(periodeDtoListe[0].resultatkode).isEqualTo("KOSTNADSBEREGNET_BIDRAG") },
            { assertThat(periodeDtoListe[1].periode.fom).isEqualTo(YearMonth.parse("2019-07")) },
            { assertThat(periodeDtoListe[1].periode.til).isEqualTo(YearMonth.parse("2020-01")) },
            { assertThat(periodeDtoListe[1].vedtaksid).isEqualTo(323) },
            { assertThat(periodeDtoListe[1].beløp).isEqualTo(BigDecimal.valueOf(3520)) },
            { assertThat(periodeDtoListe[1].valutakode).isEqualTo("NOK") },
            { assertThat(periodeDtoListe[1].resultatkode).isEqualTo("KOSTNADSBEREGNET_BIDRAG") },
        )
    }

    @Test
    // Skal hente stønad og perioder der det hentes to stønader fra ulike identer
    @Suppress("NonAsciiCharacters")
    fun `skal hente stønad og perioder der det hentes to stønader fra ulike identer`() {
        `when`(identUtilsMock.hentAlleIdenter(Personident("Skyldner123"))).thenReturn(listOf("Skyldner123", "Skyldner456"))
        `when`(identUtilsMock.hentAlleIdenter(Personident("Kravhaver123"))).thenReturn(listOf("Kravhaver123"))
        `when`(
            persistenceServiceMock.hentStønad(
                stønadType = Stønadstype.BIDRAG.toString(),
                skyldnerIdentListe = listOf("Skyldner123", "Skyldner456"),
                kravhaverIdentListe = listOf("Kravhaver123"),
                sak = "SAK-001",
            ),
        ).thenReturn(byggStønadResponseFlereStønader())
        `when`(
            persistenceServiceMock.hentPerioderForStønad(1),
        ).thenReturn(emptyList())

        val hentStønadRequest = HentStønadRequest(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer("SAK-001"),
            skyldner = Personident("Skyldner123"),
            kravhaver = Personident("Kravhaver123"),
        )
        val returnertStønad = beløpshistorikkService.hentStønad(hentStønadRequest)

        assertAll(
            { assertThat(returnertStønad).isNotNull() },
            { assertThat(returnertStønad?.stønadsid).isEqualTo(1) },
            { assertThat(returnertStønad?.skyldner).isEqualTo(Personident("Skyldner123")) },
        )
    }

    @Test
    fun `skal opprette nytt engangsbeløp`() {
        `when`(persistenceServiceMock.opprettEngangsbeløp(capture(opprettEngangsbeløpRequestDto))).thenReturn(1)

        val nyttEngangsbeløpOpprettet = beløpshistorikkService.opprettEngangsbeløp(byggEngangsbeløpRequest())

        val engangsbeløpDto = opprettEngangsbeløpRequestDto.value

        Mockito.verify(persistenceServiceMock, Mockito.times(1))
            .opprettEngangsbeløp(any(OpprettEngangsbeløpRequestDto::class.java))

        assertAll(
            { assertThat(nyttEngangsbeløpOpprettet).isNotNull() },
            // Sjekk engangsbeløpDto
            { assertThat(engangsbeløpDto).isNotNull() },
            { assertThat(engangsbeløpDto.type).isEqualTo(Engangsbeløptype.SÆRBIDRAG) },
            { assertThat(engangsbeløpDto.sak).isEqualTo(Saksnummer("SAK-001")) },
            { assertThat(engangsbeløpDto.skyldner).isEqualTo(Personident("Skyldner1")) },
            { assertThat(engangsbeløpDto.kravhaver).isEqualTo(Personident("Kravhaver1")) },
            { assertThat(engangsbeløpDto.mottaker).isEqualTo(Personident("Mottaker1")) },
            { assertThat(engangsbeløpDto.vedtaksid).isEqualTo(1) },
            { assertThat(engangsbeløpDto.gyldigFra.toLocalDate()).isEqualTo(LocalDate.now()) },
            { assertThat(engangsbeløpDto.gyldigTil).isNull() },
            { assertThat(engangsbeløpDto.gjortUgyldigAvVedtaksid).isNull() },
            { assertThat(engangsbeløpDto.beløp).isEqualTo(BigDecimal.valueOf(5000)) },
            { assertThat(engangsbeløpDto.betaltBeløp).isEqualTo(BigDecimal.ZERO) },
            { assertThat(engangsbeløpDto.valutakode).isEqualTo("NOK") },
            { assertThat(engangsbeløpDto.resultatkode).isEqualTo("SÆRBIDRAG_INNVILGET") },
            { assertThat(engangsbeløpDto.innkreving).isEqualTo(Innkrevingstype.MED_INNKREVING) },
            { assertThat(engangsbeløpDto.referanse).isEqualTo("Referanse") },
            { assertThat(engangsbeløpDto.opprettetAv).isEqualTo("TEST") },
        )
    }

    @Test
    // Skal hente engangsbeløp der det hentes to engangsbeløp fra ulike identer
    @Suppress("NonAsciiCharacters")
    fun `skal hente engangsbeløp der det hentes to engangsbeløp fra ulike identer`() {
        `when`(identUtilsMock.hentAlleIdenter(Personident("Skyldner123"))).thenReturn(listOf("Skyldner123", "Skyldner456"))
        `when`(identUtilsMock.hentAlleIdenter(Personident("Kravhaver123"))).thenReturn(listOf("Kravhaver123"))
        `when`(
            persistenceServiceMock.hentEngangsbeløp(
                engangsbeløpType = Engangsbeløptype.SÆRBIDRAG.toString(),
                skyldnerIdentListe = listOf("Skyldner123", "Skyldner456"),
                kravhaverIdentListe = listOf("Kravhaver123"),
                sak = "SAK-001",
                referanse = "Referanse-001",
            ),
        ).thenReturn(byggEngangsbeløpResponseFlereEngangsbeløp())

        val hentEngangsbeløpRequest = HentEngangsbeløpRequest(
            type = Engangsbeløptype.SÆRBIDRAG,
            sak = Saksnummer("SAK-001"),
            skyldner = Personident("Skyldner123"),
            kravhaver = Personident("Kravhaver123"),
            referanse = "Referanse-001",
        )
        val returnertEngangsbeløp = beløpshistorikkService.hentEngangsbeløp(hentEngangsbeløpRequest)

        assertAll(
            { assertThat(returnertEngangsbeløp).isNotNull() },
            { assertThat(returnertEngangsbeløp?.engangsbeløpsid).isEqualTo(1) },
            { assertThat(returnertEngangsbeløp?.skyldner).isEqualTo(Personident("Skyldner123")) },
        )
    }

    object MockitoHelper {
        // use this in place of captor.capture() if you are trying to capture an argument that is not nullable
        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

        fun <T> any(type: Class<T>): T = Mockito.any(type)
    }
}
