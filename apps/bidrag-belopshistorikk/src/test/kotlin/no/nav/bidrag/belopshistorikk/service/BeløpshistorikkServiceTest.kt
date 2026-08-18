package no.nav.bidrag.belopshistorikk.service

import no.nav.bidrag.belopshistorikk.BidragBeløpshistorikkTest
import no.nav.bidrag.belopshistorikk.BidragBeløpshistorikkTest.Companion.TEST_PROFILE
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.byggEngangsbeløpRequest
import no.nav.bidrag.belopshistorikk.TestUtil.Companion.byggEngangsbeløpRequest2
import no.nav.bidrag.belopshistorikk.persistence.repository.EngangsbeløpRepository
import no.nav.bidrag.belopshistorikk.persistence.repository.PeriodeRepository
import no.nav.bidrag.belopshistorikk.persistence.repository.StønadRepository
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.avrundetMedToDesimaler
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragssakerRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadsperiodeRequestDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [BidragBeløpshistorikkTest::class])
@ActiveProfiles(TEST_PROFILE)
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
@EnableMockOAuth2Server
class BeløpshistorikkServiceTest {
    @Autowired
    private lateinit var periodeRepository: PeriodeRepository

    @Autowired
    private lateinit var stønadRepository: StønadRepository

    @Autowired
    private lateinit var engangsbeløpRepository: EngangsbeløpRepository

    @Autowired
    private lateinit var beløpshistorikkService: BeløpshistorikkService

    @Autowired
    private lateinit var persistenceService: PersistenceService

    @BeforeEach
    fun `init`() {
        // Sletter alle forekomster
        periodeRepository.deleteAll()
        stønadRepository.deleteAll()
        engangsbeløpRepository.deleteAll()
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette ny stønad`() {
        // Oppretter ny stønad
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-02-01"), til = LocalDate.parse("2021-03-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        val nyStønadOpprettet = beløpshistorikkService.opprettStønad(opprettStønadRequest)

        assertAll(
            { assertThat(nyStønadOpprettet).isNotNull() },
        )
    }

    @Test
    // Returnerer stønad og alle perioder som ikke er markert som ugyldige
    @Suppress("NonAsciiCharacters")
    fun `skal finne alle gyldige perioder for en stønad`() {
        // Oppretter ny stønad
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-02-01"), til = LocalDate.parse("2021-03-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = 1,
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.02),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-04-01"), til = LocalDate.parse("2021-05-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val opprettetStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = opprettStønadRequest.type,
                    sak = opprettStønadRequest.sak,
                    skyldner = opprettStønadRequest.skyldner,
                    kravhaver = opprettStønadRequest.kravhaver,
                ),
            )

        assertAll(
            { assertThat(opprettetStønad).isNotNull() },
            { assertThat(opprettetStønad!!.periodeListe.size).isEqualTo(3) },
            { assertThat(opprettetStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-02")) },
            { assertThat(opprettetStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(opprettetStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(opprettetStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(opprettetStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(opprettetStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.02)) },
            { assertThat(opprettetStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(opprettetStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(opprettetStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal finne alle perioder for en stønad, også ugyldiggjorte - Ugyldiggjorte kommer etter gyldige perioder`() {
        // Oppretter ny stønad
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-02-01"), til = LocalDate.parse("2021-03-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = 1,
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.02),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-04-01"), til = LocalDate.parse("2021-05-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val funnetStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = opprettStønadRequest.type.toString(),
                skyldner = opprettStønadRequest.skyldner.verdi,
                kravhaver = opprettStønadRequest.kravhaver.verdi,
                sak = opprettStønadRequest.sak.toString(),
            )

        assertAll(
            { assertThat(funnetStønad).isNotNull() },
            { assertThat(funnetStønad!!.periodeListe.size).isEqualTo(4) },
            { assertThat(funnetStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-02")) },
            { assertThat(funnetStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(funnetStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(funnetStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(funnetStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(funnetStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(funnetStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(funnetStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.02)) },
            { assertThat(funnetStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(funnetStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(funnetStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(funnetStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(funnetStønad!!.periodeListe[3].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(funnetStønad!!.periodeListe[3].periode.til).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(funnetStønad!!.periodeListe[3].periodeGjortUgyldigAvVedtaksid).isEqualTo(1) },
            { assertThat(funnetStønad!!.periodeListe[3].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal finne stønad fra sammensatt nøkkel`() {
        // Oppretter ny stønad

        val periodeListe =
            listOf(
                OpprettStønadsperiodeRequestDto(
                    ÅrMånedsperiode(fom = LocalDate.parse("2019-01-01"), til = LocalDate.parse("2019-07-01")),
                    vedtaksid = 1,
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = null,
                    beløp = BigDecimal.valueOf(1),
                    valutakode = "NOK",
                    resultatkode = "KOSTNADSBEREGNET_BIDRAG",
                ),
            )

        val nyStønadOpprettetStønadsid =
            persistenceService.opprettStønad(
                opprettStønadRequestDto = OpprettStønadRequestDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("SAK-001"),
                    skyldner = Personident("Skyldner123"),
                    kravhaver = Personident("Kravhaver123"),
                    mottaker = Personident("MottakerId123"),
                    nesteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    opprettetAv = "R153961",
                    periodeListe = periodeListe,
                ),
            )

        val nyStønadOpprettet = persistenceService.hentStønadFraId(nyStønadOpprettetStønadsid)

        // Finner stønaden som akkurat ble opprettet
        val stønadFunnet =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = Stønadstype.valueOf(nyStønadOpprettet!!.type),
                    sak = Saksnummer(nyStønadOpprettet.sak),
                    skyldner = Personident(nyStønadOpprettet.skyldner),
                    kravhaver = Personident(nyStønadOpprettet.kravhaver),
                ),
            )

        assertAll(
            { assertThat(stønadFunnet).isNotNull() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal finne stønad fra generert id`() {
        // Oppretter ny stønad
        val periodeListe =
            listOf(
                OpprettStønadsperiodeRequestDto(
                    ÅrMånedsperiode(fom = LocalDate.parse("2019-01-01"), til = LocalDate.parse("2019-07-01")),
                    vedtaksid = 1,
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = null,
                    beløp = BigDecimal.valueOf(1),
                    valutakode = "NOK",
                    resultatkode = "KOSTNADSBEREGNET_BIDRAG",
                ),
            )

        val nyStønadOpprettetStønadsid =
            persistenceService.opprettStønad(
                OpprettStønadRequestDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("SAK-001"),
                    skyldner = Personident("Skyldner123"),
                    kravhaver = Personident("Kravhaver123"),
                    mottaker = Personident("MottakerId123"),
                    nesteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    opprettetAv = "R153961",
                    periodeListe = periodeListe,
                ),
            )

        // Finner stønaden som akkurat ble opprettet
        val stønadFunnet = beløpshistorikkService.hentStønadFraId(nyStønadOpprettetStønadsid)

        assertAll(
            { assertThat(stønadFunnet).isNotNull() },
        )
    }

    // endrer eksisterende stønad og ugyldiggjør perioder som har blitt endret i nytt vedtak
    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal endre eksisterende stønad`() {
        // Oppretter først stønaden som skal endres etterpå
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-03-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-07-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-07-01"), til = LocalDate.parse("2021-12-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val originalStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(originalStønadRequest)
        val originalStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = originalStønadRequest.type.toString(),
                skyldner = originalStønadRequest.skyldner.verdi,
                kravhaver = originalStønadRequest.kravhaver.verdi,
                sak = originalStønadRequest.sak.toString(),
            )

        // Oppretter så ny request som skal oppdatere eksisterende stønad
        val endretStønadPeriodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-05-01"), til = LocalDate.parse("2021-06-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.01),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )
        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = LocalDate.parse("2021-08-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.02),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )
        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-08-01"), til = LocalDate.parse("2021-10-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.03),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )

        val endretStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = endretStønadPeriodeListe,
            )

        beløpshistorikkService.endreStønad(
            eksisterendeStønad = originalStønad!!,
            oppdatertStønad = endretStønadRequest,
            vedtakstidspunkt = LocalDateTime.now(),
        )
        val endretStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = endretStønadRequest.type.toString(),
                skyldner = endretStønadRequest.skyldner.verdi,
                kravhaver = endretStønadRequest.kravhaver.verdi,
                sak = endretStønadRequest.sak.toString(),
            )

        assertAll(
            // Perioder sorteres på periodeGjortUgyldigAvVedtaksid så fom-dato. Perioder med null i periodeGjortUgyldigAvVedtaksid kommer sist.
            { assertThat(endretStønad).isNotNull() },
            { assertThat(endretStønad!!.periodeListe.size).isEqualTo(8) },
            // Første periode er før perioder for nytt vedtak, blir ikke endret
            { assertThat(endretStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(endretStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Avkortet utgave av ugyldiggjort periode med til-dato lik fom-dato for nytt vedtak
            { assertThat(endretStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(endretStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(endretStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(5000.01)) },
            { assertThat(endretStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[3].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[3].periode.til).isEqualTo(YearMonth.parse("2021-08")) },
            { assertThat(endretStønad!!.periodeListe[3].beløp).isEqualTo(BigDecimal.valueOf(5000.02)) },
            { assertThat(endretStønad!!.periodeListe[3].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[4].periode.fom).isEqualTo(YearMonth.parse("2021-08")) },
            { assertThat(endretStønad!!.periodeListe[4].periode.til).isEqualTo(YearMonth.parse("2021-10")) },
            { assertThat(endretStønad!!.periodeListe[4].beløp).isEqualTo(BigDecimal.valueOf(5000.03)) },
            { assertThat(endretStønad!!.periodeListe[4].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Avkortet utgave av ugyldiggjort periode med fom-dato lik til-dato for nytt vedtak
            { assertThat(endretStønad!!.periodeListe[5].periode.fom).isEqualTo(YearMonth.parse("2021-10")) },
            { assertThat(endretStønad!!.periodeListe[5].periode.til).isEqualTo(YearMonth.parse("2021-12")) },
            { assertThat(endretStønad!!.periodeListe[5].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(endretStønad!!.periodeListe[5].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Perioden overlapper med nytt vedtak, settes til ugyldig
            { assertThat(endretStønad!!.periodeListe[6].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(endretStønad!!.periodeListe[6].periode.til).isEqualTo(YearMonth.parse("2021-07")) },
            { assertThat(endretStønad!!.periodeListe[6].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(endretStønad!!.periodeListe[6].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            // Perioden overlapper med nytt vedtak, settes til ugyldig
            { assertThat(endretStønad!!.periodeListe[7].periode.fom).isEqualTo(YearMonth.parse("2021-07")) },
            { assertThat(endretStønad!!.periodeListe[7].periode.til).isEqualTo(YearMonth.parse("2021-12")) },
            { assertThat(endretStønad!!.periodeListe[7].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(endretStønad!!.periodeListe[7].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
        )
    }

    // Perioder i eksisterende stønad skal ugyldiggjøres og erstattes med nye perioder med like data og justerte datoer
    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test på splitt av perioder med vedtak med periode midt i eksisterende stønad`() {
        // Oppretter først stønaden som skal endres etterpå
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2022-01-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val originalStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(originalStønadRequest)
        val originalStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = originalStønadRequest.type.toString(),
                skyldner = originalStønadRequest.skyldner.verdi,
                kravhaver = originalStønadRequest.kravhaver.verdi,
                sak = originalStønadRequest.sak.toString(),
            )

        // Oppretter så ny request som skal oppdatere eksisterende stønad
        val endretStønadPeriodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-05-01"), til = LocalDate.parse("2021-06-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.01),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )

        val endretStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = endretStønadPeriodeListe,
            )

        beløpshistorikkService.endreStønad(originalStønad!!, endretStønadRequest, LocalDateTime.now())
        val endretStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = endretStønadRequest.type.toString(),
                skyldner = endretStønadRequest.skyldner.verdi,
                kravhaver = endretStønadRequest.kravhaver.verdi,
                sak = endretStønadRequest.sak.toString(),
            )

        assertAll(
            // Perioder sorteres på periodeGjortUgyldigAvVedtaksid så fom-dato. Perioder med null i periodeGjortUgyldigAvVedtaksid kommer sist.
            { assertThat(endretStønad).isNotNull() },
            { assertThat(endretStønad!!.periodeListe.size).isEqualTo(4) },
            // Periode for eksisterende stønad ugyldigjøres og kopieres til to nye perioder, én for og én etter periode fra nytt vedtak.
            // Første periode
            { assertThat(endretStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.01)) },
            { assertThat(endretStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Siste periode fra eksisterende stønad
            { assertThat(endretStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2022-01")) },
            { assertThat(endretStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Perioden overlapper med nytt vedtak, settes til ugyldig
            { assertThat(endretStønad!!.periodeListe[3].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[3].periode.til).isEqualTo(YearMonth.parse("2022-01")) },
            { assertThat(endretStønad!!.periodeListe[3].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[3].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
        )
    }

    // Perioder i eksisterende stønad skal ugyldiggjøres og erstattes med nye perioder med like data og justerte datoer
    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test med null i tildato på ny vedtaksperiode`() {
        // Oppretter først stønaden som skal endres etterpå
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2022-01-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val originalStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(originalStønadRequest)
        val originalStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = originalStønadRequest.type.toString(),
                skyldner = originalStønadRequest.skyldner.verdi,
                kravhaver = originalStønadRequest.kravhaver.verdi,
                sak = originalStønadRequest.sak.toString(),
            )

        // Oppretter så ny request som skal oppdatere eksisterende stønad
        val endretStønadPeriodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-05-01"), til = null),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.01),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )

        val endretStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = endretStønadPeriodeListe,
            )

        beløpshistorikkService.endreStønad(originalStønad!!, endretStønadRequest, LocalDateTime.now())
        val endretStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = endretStønadRequest.type.toString(),
                skyldner = endretStønadRequest.skyldner.verdi,
                kravhaver = endretStønadRequest.kravhaver.verdi,
                sak = endretStønadRequest.sak.toString(),
            )

        assertAll(
            // Perioder sorteres på periodeGjortUgyldigAvVedtaksid så fom-dato. Perioder med null i periodeGjortUgyldigAvVedtaksid kommer sist.
            { assertThat(endretStønad).isNotNull() },
            { assertThat(endretStønad!!.periodeListe.size).isEqualTo(3) },
            // Periode for eksisterende stønad ugyldigjøres og kopieres til to nye perioder, én for og én etter periode fra nytt vedtak.
            // Første periode
            { assertThat(endretStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[1].periode.til).isNull() },
            { assertThat(endretStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.01)) },
            { assertThat(endretStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Perioden overlapper med nytt vedtak, settes til ugyldig
            { assertThat(endretStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2022-01")) },
            { assertThat(endretStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
        )
    }

    // Perioder i eksisterende stønad skal ugyldiggjøres og erstattes med nye perioder med like data og justerte datoer
    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test med null i tildato på eksisterende stønadsperiode`() {
        // Oppretter først stønaden som skal endres etterpå
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = null),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val originalStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(originalStønadRequest)
        val originalStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = originalStønadRequest.type.toString(),
                skyldner = originalStønadRequest.skyldner.verdi,
                kravhaver = originalStønadRequest.kravhaver.verdi,
                sak = originalStønadRequest.sak.toString(),
            )

        // Oppretter så ny request som skal oppdatere eksisterende stønad
        val endretStønadPeriodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-05-01"), til = LocalDate.parse("2021-06-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.01),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )

        val endretStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = endretStønadPeriodeListe,
            )

        beløpshistorikkService.endreStønad(originalStønad!!, endretStønadRequest, LocalDateTime.now())
        val endretStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = endretStønadRequest.type.toString(),
                skyldner = endretStønadRequest.skyldner.verdi,
                kravhaver = endretStønadRequest.kravhaver.verdi,
                sak = endretStønadRequest.sak.toString(),
            )

        assertAll(
            // Perioder sorteres på periodeGjortUgyldigAvVedtaksid så fom-dato. Perioder med null i periodeGjortUgyldigAvVedtaksid kommer sist.
            { assertThat(endretStønad).isNotNull() },
            { assertThat(endretStønad!!.periodeListe.size).isEqualTo(4) },
            // Periode for eksisterende stønad ugyldigjøres og kopieres til to nye perioder, én for og én etter periode fra nytt vedtak.
            // Første periode
            { assertThat(endretStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.01)) },
            { assertThat(endretStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Siste periode fra eksisterende stønad
            { assertThat(endretStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[2].periode.til).isNull() },
            { assertThat(endretStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Perioden overlapper med nytt vedtak, settes til ugyldig
            { assertThat(endretStønad!!.periodeListe[3].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[3].periode.til).isNull() },
            { assertThat(endretStønad!!.periodeListe[3].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[3].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
        )
    }

    // Alle perioder i eksisterende stønad som befinner seg innenfor fra- og tildato for nytt vedtak skal erstattes selv om det finnes
    // en identisk periode i det nye vedtaket.
    @Test
    @Suppress("NonAsciiCharacters")
    fun `Test med like perioder og endret beløp i én periode`() {
        // Oppretter først stønaden som skal endres etterpå
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-05-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-05-01"), til = LocalDate.parse("2021-06-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = null),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val originalStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(originalStønadRequest)
        val originalStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = originalStønadRequest.type.toString(),
                skyldner = originalStønadRequest.skyldner.verdi,
                kravhaver = originalStønadRequest.kravhaver.verdi,
                sak = originalStønadRequest.sak.toString(),
            )

        // Oppretter så ny request som skal oppdatere eksisterende stønad
        val endretStønadPeriodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()

        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-05-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-05-01"), til = LocalDate.parse("2021-06-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.01),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )

        endretStønadPeriodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = null),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val endretStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = endretStønadPeriodeListe,
            )

        beløpshistorikkService.endreStønad(originalStønad!!, endretStønadRequest, LocalDateTime.now())
        val endretStønad =
            beløpshistorikkService.hentStønadInkludertUgyldiggjortePerioder(
                stønadstype = endretStønadRequest.type.toString(),
                skyldner = endretStønadRequest.skyldner.verdi,
                kravhaver = endretStønadRequest.kravhaver.verdi,
                sak = endretStønadRequest.sak.toString(),
            )

        assertAll(
            // Perioder sorteres på periodeGjortUgyldigAvVedtaksid så fom-dato. Perioder med null i periodeGjortUgyldigAvVedtaksid kommer sist.
            { assertThat(endretStønad).isNotNull() },
            { assertThat(endretStønad!!.periodeListe.size).isEqualTo(6) },
            // Alle perioder for eksisterende stønad ugyldigjøres selv om noen av periodene er identiske
            // Første periode
            { assertThat(endretStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[0].vedtaksid).isEqualTo(2) },
            { assertThat(endretStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Periode fra nytt vedtak
            { assertThat(endretStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[1].vedtaksid).isEqualTo(2) },
            { assertThat(endretStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.01)) },
            { assertThat(endretStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Siste periode fra eksisterende stønad
            { assertThat(endretStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[2].periode.til).isNull() },
            { assertThat(endretStønad!!.periodeListe[2].vedtaksid).isEqualTo(2) },
            { assertThat(endretStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(endretStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isNull() },
            // Perioden overlapper med nytt vedtak, settes til ugyldig
            { assertThat(endretStønad!!.periodeListe[3].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(endretStønad!!.periodeListe[3].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[3].vedtaksid).isEqualTo(1) },
            { assertThat(endretStønad!!.periodeListe[3].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(endretStønad!!.periodeListe[3].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(endretStønad!!.periodeListe[4].periode.fom).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(endretStønad!!.periodeListe[4].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[4].vedtaksid).isEqualTo(1) },
            { assertThat(endretStønad!!.periodeListe[4].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(endretStønad!!.periodeListe[4].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(endretStønad!!.periodeListe[5].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(endretStønad!!.periodeListe[5].periode.til).isNull() },
            { assertThat(endretStønad!!.periodeListe[5].vedtaksid).isEqualTo(1) },
            { assertThat(endretStønad!!.periodeListe[5].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(endretStønad!!.periodeListe[5].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal finne historiske perioder for en stønad`() {
        // Oppretter ny stønad
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        // Legger først til periode som ikke skal returneres
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2022-01-01")),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.parse("2021-01-17T17:17:17.179121094"),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.parse("2020-10-17T10:12:14.169121094"),
                gyldigTil = LocalDateTime.parse("2021-01-17T17:17:17.179121094"),
                periodeGjortUgyldigAvVedtaksid = 2,
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-04-01"), til = LocalDate.parse("2021-06-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.parse("2020-10-17T10:12:14.169121094"),
                gyldigTil = LocalDateTime.parse("2021-01-17T17:17:17.179121094"),
                periodeGjortUgyldigAvVedtaksid = 2,
                beløp = BigDecimal.valueOf(5000.02),
                valutakode = "NOK",
                resultatkode = "Ny periode lagt til",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = LocalDate.parse("2022-01-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.parse("2020-10-17T10:12:14.169121094"),
                gyldigTil = LocalDateTime.parse("2021-01-17T17:17:17.179121094"),
                periodeGjortUgyldigAvVedtaksid = 2,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val funnetStønad =
            beløpshistorikkService.hentStønadHistorisk(
                HentStønadHistoriskRequest(
                    type = opprettStønadRequest.type,
                    sak = opprettStønadRequest.sak,
                    skyldner = opprettStønadRequest.skyldner,
                    kravhaver = opprettStønadRequest.kravhaver,
                    gyldigTidspunkt = LocalDateTime.parse("2020-12-31T23:00:00.169121094"),
                ),
            )

        assertAll(
            { assertThat(funnetStønad).isNotNull() },
            { assertThat(funnetStønad!!.periodeListe.size).isEqualTo(3) },
            { assertThat(funnetStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(funnetStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(funnetStønad!!.periodeListe[0].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(funnetStønad!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(funnetStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(funnetStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(funnetStønad!!.periodeListe[1].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(funnetStønad!!.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.02)) },
            { assertThat(funnetStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(funnetStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2022-01")) },
            { assertThat(funnetStønad!!.periodeListe[2].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(funnetStønad!!.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal finne alle stønader for angitt sak`() {
        // Oppretter ny stønad
        val periodeListe1 = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val periodeListe2 = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val periodeListe3 = mutableListOf<OpprettStønadsperiodeRequestDto>()
        // Oppretter stønad 1
        periodeListe1.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.now(), til = LocalDate.now().plusMonths(1)),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe1.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.now(), til = LocalDate.now().plusMonths(1)),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(100.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest1 =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner001"),
                kravhaver = Personident("Kravhaver001"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe1,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest1)

        // Oppretter stønad 2, ligger på en annen sak og skal ikke hentes
        periodeListe2.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.now(), til = LocalDate.now().plusMonths(1)),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(998.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest2 =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-002"),
                skyldner = Personident("Skyldner002"),
                kravhaver = Personident("Kravhaver002"),
                mottaker = Personident("Mottaker002"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe2,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest2)

        // Oppretter stønad 3, ligger på samme sak og skal hentes
        periodeListe3.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.now(), til = LocalDate.now().plusMonths(1)),
                vedtaksid = 3,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(4477.03),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest3 =
            OpprettStønadRequestDto(
                type = Stønadstype.FORSKUDD,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner001"),
                kravhaver = Personident("Kravhaver001"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe3,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest3)

        val funnedeStønaderListe = beløpshistorikkService.hentStønaderForSak(opprettStønadRequest1.sak.toString())

        assertAll(
            { assertThat(funnedeStønaderListe).size().isEqualTo(2) },
            { assertThat(funnedeStønaderListe[0].type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(funnedeStønaderListe[0].sak.toString()).isEqualTo(Saksnummer("SAK-001").toString()) },
            { assertThat(funnedeStønaderListe[0].skyldner.verdi).isEqualTo(Personident("Skyldner001").verdi) },
            { assertThat(funnedeStønaderListe[0].periodeListe.size).isEqualTo(2) },
            { assertThat(funnedeStønaderListe[0].periodeListe[0].periode.fom).isEqualTo(YearMonth.now()) },
            { assertThat(funnedeStønaderListe[0].periodeListe[0].periode.til).isEqualTo(YearMonth.now().plusMonths(1)) },
            { assertThat(funnedeStønaderListe[0].periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(funnedeStønaderListe[0].periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(funnedeStønaderListe[0].periodeListe[1].periode.fom).isEqualTo(YearMonth.now()) },
            { assertThat(funnedeStønaderListe[0].periodeListe[1].periode.til).isEqualTo(YearMonth.now().plusMonths(1)) },
            { assertThat(funnedeStønaderListe[0].periodeListe[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(funnedeStønaderListe[0].periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(100.01)) },
            { assertThat(funnedeStønaderListe[1].type).isEqualTo(Stønadstype.FORSKUDD) },
            { assertThat(funnedeStønaderListe[1].periodeListe.size).isEqualTo(1) },
            { assertThat(funnedeStønaderListe[1].sak.toString()).isEqualTo("SAK-001") },
            { assertThat(funnedeStønaderListe[1].skyldner.verdi).isEqualTo(Personident("Skyldner001").verdi) },
            { assertThat(funnedeStønaderListe[1].periodeListe.size).isEqualTo(1) },
            { assertThat(funnedeStønaderListe[1].periodeListe[0].periode.fom).isEqualTo(YearMonth.now()) },
            { assertThat(funnedeStønaderListe[1].periodeListe[0].periode.til).isEqualTo(YearMonth.now().plusMonths(1)) },
            { assertThat(funnedeStønaderListe[1].periodeListe[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(funnedeStønaderListe[1].periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(4477.03)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal finne alle løpende bidragssaker for angitt skyldner`() {
        val periodeListe1 = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val periodeListe2 = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val periodeListe3 = mutableListOf<OpprettStønadsperiodeRequestDto>()
        val periodeListe4 = mutableListOf<OpprettStønadsperiodeRequestDto>()

        periodeListe1.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(
                    fom = LocalDate.of(2024, 1, 1),
                    til = LocalDate.of(2024, 7, 1),
                ),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe1.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2024, 7, 1), til = null),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(100.01),
                valutakode = "SEK",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest1 =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner001"),
                kravhaver = Personident("Kravhaver001"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe1,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest1)

        // Oppretter stønad 2, er av type forskudd og skal ikke returneres
        periodeListe2.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2024, 7, 1), til = null),
                vedtaksid = 2,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(998.02),
                valutakode = "EUR",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest2 =
            OpprettStønadRequestDto(
                type = Stønadstype.FORSKUDD,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("NAV"),
                kravhaver = Personident("Kravhaver001"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe2,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest2)

        // Oppretter stønad 3, ligger på annen sak. Skal hentes.
        periodeListe3.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2024, 7, 1), til = null),
                vedtaksid = 3,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(4477.03),
                valutakode = "USD",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest3 =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-002"),
                skyldner = Personident("Skyldner001"),
                kravhaver = Personident("Kravhaver002"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe3,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest3)

        // Oppretter stønad 4, med perioder tilbake i tid.
        periodeListe4.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2022, 7, 1), til = LocalDate.of(2023, 1, 1)),
                vedtaksid = 4,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(999.06),
                valutakode = "ISK",
                resultatkode = "Alles gut",
            ),
        )
        val opprettStønadRequest4 =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-003"),
                skyldner = Personident("Skyldner001"),
                kravhaver = Personident("Kravhaver005"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe4,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest4)

        // Kjører tester med ulike datoer for å sjekke at riktige saker og løpende beløp hentes ut
        val respons1 = beløpshistorikkService.finnLøpendeBidragssaker(
            LøpendeBidragssakerRequest(skyldner = Personident("Skyldner001"), dato = LocalDate.of(2024, 8, 1)),
        )

        val respons2 = beløpshistorikkService.finnLøpendeBidragssaker(
            LøpendeBidragssakerRequest(skyldner = Personident("Skyldner001"), dato = LocalDate.of(2024, 6, 17)),
        )

        val respons3 = beløpshistorikkService.finnLøpendeBidragssaker(
            LøpendeBidragssakerRequest(Personident("Skyldner001"), LocalDate.of(2022, 8, 30)),
        )

        assertAll(
            { assertThat(respons1.bidragssakerListe).size().isEqualTo(2) },
            { assertThat(respons1.bidragssakerListe[0].sak.toString()).isEqualTo(Saksnummer("SAK-001").toString()) },
            { assertThat(respons1.bidragssakerListe[0].type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(respons1.bidragssakerListe[0].kravhaver).isEqualTo(Personident("Kravhaver001")) },
            { assertThat(respons1.bidragssakerListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(100.01)) },
            { assertThat(respons1.bidragssakerListe[0].valutakode).isEqualTo("SEK") },

            { assertThat(respons1.bidragssakerListe[1].sak.toString()).isEqualTo(Saksnummer("SAK-002").toString()) },
            { assertThat(respons1.bidragssakerListe[1].type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(respons1.bidragssakerListe[1].kravhaver).isEqualTo(Personident("Kravhaver002")) },
            { assertThat(respons1.bidragssakerListe[1].løpendeBeløp).isEqualTo(BigDecimal.valueOf(4477.03)) },
            { assertThat(respons1.bidragssakerListe[1].valutakode).isEqualTo("USD") },

            { assertThat(respons2.bidragssakerListe).size().isEqualTo(1) },
            { assertThat(respons2.bidragssakerListe[0].sak.toString()).isEqualTo(Saksnummer("SAK-001").toString()) },
            { assertThat(respons2.bidragssakerListe[0].type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(respons2.bidragssakerListe[0].kravhaver).isEqualTo(Personident("Kravhaver001")) },
            { assertThat(respons2.bidragssakerListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(respons2.bidragssakerListe[0].valutakode).isEqualTo("NOK") },

            { assertThat(respons3.bidragssakerListe).size().isEqualTo(1) },
            { assertThat(respons3.bidragssakerListe[0].sak.toString()).isEqualTo(Saksnummer("SAK-003").toString()) },
            { assertThat(respons3.bidragssakerListe[0].type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(respons3.bidragssakerListe[0].kravhaver).isEqualTo(Personident("Kravhaver005")) },
            { assertThat(respons3.bidragssakerListe[0].løpendeBeløp).isEqualTo(BigDecimal.valueOf(999.06)) },
            { assertThat(respons3.bidragssakerListe[0].valutakode).isEqualTo("ISK") },

        )
    }

    @Test
    // Returnerer stønad og alle perioder med beløp som ikke er markert som ugyldige
    @Suppress("NonAsciiCharacters")
    fun `skal finne alle gyldige periodebeløp for en stønad`() {
        // Oppretter ny stønad
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-02-01"), til = LocalDate.parse("2021-03-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "SEK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = 1,
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "SEK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(5000.02),
                valutakode = "SEK",
                resultatkode = "Ny periode lagt til",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-04-01"), til = LocalDate.parse("2021-05-01")),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "SEK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("MottakerId123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )

        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val opprettetStønad =
            beløpshistorikkService.hentStønadMedPeriodebeløp(
                HentStønadRequest(
                    type = opprettStønadRequest.type,
                    sak = opprettStønadRequest.sak,
                    skyldner = opprettStønadRequest.skyldner,
                    kravhaver = opprettStønadRequest.kravhaver,
                ),
            )

        assertAll(
            { assertThat(opprettetStønad).isNotNull() },
            { assertThat(opprettetStønad!!.førsteIndeksreguleringsår).isEqualTo(2024) },
            { assertThat(opprettetStønad!!.periodeBeløpListe.size).isEqualTo(3) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-02")) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[0].periode.til).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[0].valutakode).isEqualTo("SEK") },
            { assertThat(opprettetStønad!!.periodeBeløpListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[1].periode.til).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[1].beløp).isEqualTo(BigDecimal.valueOf(5000.02)) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[1].valutakode).isEqualTo("SEK") },
            { assertThat(opprettetStønad!!.periodeBeløpListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[2].periode.til).isEqualTo(YearMonth.parse("2021-05")) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(opprettetStønad!!.periodeBeløpListe[2].valutakode).isEqualTo("SEK") },

        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette nytt engangsbeløp`() {
        val opprettEngangsbeløpRequest = byggEngangsbeløpRequest()

        val nyttEngangsbeløpOpprettet = beløpshistorikkService.opprettEngangsbeløp(opprettEngangsbeløpRequest)

        val nyttEngangsbeløpHentet =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = Engangsbeløptype.SÆRBIDRAG,
                    sak = Saksnummer("SAK-001"),
                    skyldner = Personident("Skyldner1"),
                    kravhaver = Personident("Kravhaver1"),
                    referanse = "Referanse",
                ),
            )

        assertAll(
            { assertThat(nyttEngangsbeløpOpprettet).isNotNull() },
            { assertThat(nyttEngangsbeløpHentet).isNotNull() },
            { assertThat(nyttEngangsbeløpHentet!!.type).isEqualTo(Engangsbeløptype.SÆRBIDRAG) },
            { assertThat(nyttEngangsbeløpHentet!!.sak).isEqualTo(Saksnummer("SAK-001")) },
            { assertThat(nyttEngangsbeløpHentet!!.skyldner).isEqualTo(Personident("Skyldner1")) },
            { assertThat(nyttEngangsbeløpHentet!!.kravhaver).isEqualTo(Personident("Kravhaver1")) },
            { assertThat(nyttEngangsbeløpHentet!!.mottaker).isEqualTo(Personident("Mottaker1")) },
            { assertThat(nyttEngangsbeløpHentet!!.vedtaksid).isEqualTo(1) },
            { assertThat(nyttEngangsbeløpHentet!!.gyldigFra.toLocalDate()).isEqualTo(LocalDate.now()) },
            { assertThat(nyttEngangsbeløpHentet!!.gyldigTil).isNull() },
            { assertThat(nyttEngangsbeløpHentet!!.gjortUgyldigAvVedtaksid).isNull() },
            { assertThat(nyttEngangsbeløpHentet!!.beløp).isEqualTo(BigDecimal.valueOf(5000).avrundetMedToDesimaler) },
            { assertThat(nyttEngangsbeløpHentet!!.betaltBeløp).isEqualTo(BigDecimal.ZERO.avrundetMedToDesimaler) },
            { assertThat(nyttEngangsbeløpHentet!!.valutakode).isEqualTo("NOK") },
            { assertThat(nyttEngangsbeløpHentet!!.resultatkode).isEqualTo("SÆRBIDRAG_INNVILGET") },
            { assertThat(nyttEngangsbeløpHentet!!.innkreving).isEqualTo(Innkrevingstype.MED_INNKREVING) },
            { assertThat(nyttEngangsbeløpHentet!!.referanse).isEqualTo("Referanse") },
            { assertThat(nyttEngangsbeløpHentet!!.opprettetAv).isEqualTo("TEST") },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal endre eksisterende engangsbeløp`() {
        // Oppretter først engangsbeløpet som skal endres etterpå
        val originalEngangsbeløpRequest = byggEngangsbeløpRequest()

        beløpshistorikkService.opprettEngangsbeløp(originalEngangsbeløpRequest)
        val originaltEngangsbeløp =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = originalEngangsbeløpRequest.type,
                    sak = originalEngangsbeløpRequest.sak,
                    skyldner = originalEngangsbeløpRequest.skyldner,
                    kravhaver = originalEngangsbeløpRequest.kravhaver,
                    referanse = originalEngangsbeløpRequest.referanse!!,
                ),
            )

        // Oppretter så ny request som skal oppdatere eksisterende engangsbeløp
        val endretEngangsbeløpRequest = byggEngangsbeløpRequest2()

        beløpshistorikkService.endreEngangsbeløp(
            eksisterendeEngangsbeløp = originaltEngangsbeløp!!,
            oppdatertEngangsbeløp = endretEngangsbeløpRequest,
            vedtaksid = 1,
            vedtakstidspunkt = LocalDateTime.now(),
        )

        val oppdatertEngangsbeløp =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = endretEngangsbeløpRequest.type,
                    sak = endretEngangsbeløpRequest.sak,
                    skyldner = endretEngangsbeløpRequest.skyldner,
                    kravhaver = endretEngangsbeløpRequest.kravhaver,
                    referanse = endretEngangsbeløpRequest.referanse!!,
                ),
            )

        val historiskeEngangsbeløpListe =
            beløpshistorikkService.hentHistoriskeEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = endretEngangsbeløpRequest.type,
                    sak = endretEngangsbeløpRequest.sak,
                    skyldner = endretEngangsbeløpRequest.skyldner,
                    kravhaver = endretEngangsbeløpRequest.kravhaver,
                    referanse = endretEngangsbeløpRequest.referanse!!,
                ),
            )

        assertAll(
            { assertThat(originaltEngangsbeløp).isNotNull() },
            { assertThat(oppdatertEngangsbeløp).isNotNull() },
            { assertThat(historiskeEngangsbeløpListe).isNotNull() },
            { assertThat(historiskeEngangsbeløpListe.size).isEqualTo(2) },
            { assertThat(originaltEngangsbeløp.beløp).isEqualTo(BigDecimal.valueOf(5000).avrundetMedToDesimaler) },
            { assertThat(oppdatertEngangsbeløp!!.beløp).isEqualTo(BigDecimal.valueOf(6000).avrundetMedToDesimaler) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `finnLøpendeBidragIPeriodeForSkyldner skal returnere tom respons når ingen stønad finnes for skyldner`() {
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2023, 1, 1), til = LocalDate.of(2023, 6, 1)),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(1000),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner001"),
                kravhaver = Personident("Kravhaver001"),
                mottaker = Personident("Mottaker001"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val respons = beløpshistorikkService.finnLøpendeBidragIPeriodeForSkyldner(
            LøpendeBidragPeriodeRequest(
                skyldner = Personident("Skyldner999"),
                periode = ÅrMånedsperiode(fom = YearMonth.of(2023, 1), til = YearMonth.of(2023, 12)),
            ),
        )

        assertAll(
            { assertThat(respons).isNotNull() },
            { assertThat(respons.bidragListe).isEmpty() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `finnLøpendeBidragIPeriodeForSkyldner skal returnere løpende bidrag for overlappende perioder`() {
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2023, 1, 1), til = LocalDate.of(2023, 6, 1)),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(1000),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2024, 1, 1), til = LocalDate.of(2024, 6, 1)),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal.valueOf(2000),
                valutakode = "NOK",
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("Mottaker123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val respons = beløpshistorikkService.finnLøpendeBidragIPeriodeForSkyldner(
            LøpendeBidragPeriodeRequest(
                skyldner = Personident("Skyldner123"),
                periode = ÅrMånedsperiode(fom = YearMonth.of(2023, 1), til = YearMonth.of(2023, 12)),
            ),
        )

        assertAll(
            { assertThat(respons).isNotNull() },
            { assertThat(respons.bidragListe).hasSize(1) },
            { assertThat(respons.bidragListe[0].periodeListe).hasSize(1) },
            { assertThat(respons.bidragListe[0].periodeListe[0].periode.fom).isEqualTo(YearMonth.of(2023, 1)) },
            { assertThat(respons.bidragListe[0].periodeListe[0].periode.til).isEqualTo(YearMonth.of(2023, 6)) },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `finnLøpendeBidragIPeriodeForSkyldner skal håndtere null beløp og valutakode`() {
        val periodeListe = mutableListOf<OpprettStønadsperiodeRequestDto>()
        periodeListe.add(
            OpprettStønadsperiodeRequestDto(
                ÅrMånedsperiode(fom = LocalDate.of(2023, 1, 1), til = LocalDate.of(2023, 6, 1)),
                vedtaksid = 1,
                gyldigFra = LocalDateTime.now(),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = null,
                valutakode = null,
                resultatkode = "Alles gut",
            ),
        )

        val opprettStønadRequest =
            OpprettStønadRequestDto(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner123"),
                kravhaver = Personident("Kravhaver123"),
                mottaker = Personident("Mottaker123"),
                nesteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                opprettetAv = "R153961",
                periodeListe = periodeListe,
            )
        beløpshistorikkService.opprettStønad(opprettStønadRequest)

        val respons = beløpshistorikkService.finnLøpendeBidragIPeriodeForSkyldner(
            LøpendeBidragPeriodeRequest(
                skyldner = Personident("Skyldner123"),
                periode = ÅrMånedsperiode(fom = YearMonth.of(2023, 1), til = YearMonth.of(2023, 12)),
            ),
        )

        assertAll(
            { assertThat(respons).isNotNull() },
            { assertThat(respons.bidragListe).hasSize(1) },
            { assertThat(respons.bidragListe[0].periodeListe).hasSize(1) },
            { assertThat(respons.bidragListe[0].periodeListe[0].periode.fom).isEqualTo(YearMonth.of(2023, 1)) },
            { assertThat(respons.bidragListe[0].periodeListe[0].periode.til).isEqualTo(YearMonth.of(2023, 6)) },
            { assertThat(respons.bidragListe[0].periodeListe[0].løpendeBeløp).isEqualTo(BigDecimal.ZERO) },
            { assertThat(respons.bidragListe[0].periodeListe[0].valutakode).isEqualTo("NOK") },
        )
    }
}
