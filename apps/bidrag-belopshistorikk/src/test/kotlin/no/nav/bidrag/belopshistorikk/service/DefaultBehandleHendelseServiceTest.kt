package no.nav.bidrag.belopshistorikk.service

import no.nav.bidrag.belopshistorikk.BidragBeløpshistorikkTest
import no.nav.bidrag.belopshistorikk.persistence.repository.EngangsbeløpRepository
import no.nav.bidrag.belopshistorikk.persistence.repository.PeriodeRepository
import no.nav.bidrag.belopshistorikk.persistence.repository.StønadRepository
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.avrundetMedToDesimaler
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.Engangsbeløp
import no.nav.bidrag.transport.behandling.vedtak.Periode
import no.nav.bidrag.transport.behandling.vedtak.Sporingsdata
import no.nav.bidrag.transport.behandling.vedtak.Stønadsendring
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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

@DisplayName("DefaultBehandleHendelseServiceTest")
@ActiveProfiles(BidragBeløpshistorikkTest.TEST_PROFILE)
@SpringBootTest(classes = [BidragBeløpshistorikkTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
internal class DefaultBehandleHendelseServiceTest {
    @Autowired
    private lateinit var periodeRepository: PeriodeRepository

    @Autowired
    private lateinit var stønadRepository: StønadRepository

    @Autowired
    private lateinit var engangsbeløpRepository: EngangsbeløpRepository

    @Autowired
    private lateinit var behandleHendelseService: BehandleHendelseService

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
    fun `skal opprette ny stønad fra Hendelse`() {
        // Oppretter ny hendelse

        val periodeliste = mutableListOf<Periode>()
        periodeliste.add(
            Periode(
                ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = LocalDate.parse("2021-07-01")),
                BigDecimal.valueOf(17.01),
                "NOK",
                "Hunky Dory",
                "referanse1",
            ),
        )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = periodeliste,
            ),
        )

        val nyHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "X123456",
                opprettetAvNavn = "Navn",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(nyHendelse)

        val nyStønadOpprettet =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = nyHendelse.stønadsendringListe!![0].type,
                    sak = nyHendelse.stønadsendringListe!![0].sak,
                    skyldner = nyHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = nyHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(nyStønadOpprettet!!).isNotNull() },
            { assertThat(nyStønadOpprettet!!.type).isEqualTo(Stønadstype.BIDRAG) },
            { assertThat(nyStønadOpprettet!!.sak.toString()).isEqualTo(Saksnummer("SAK-001").toString()) },
            { assertThat(nyStønadOpprettet!!.skyldner.verdi).isEqualTo(Personident("Skyldner1").verdi) },
            { assertThat(nyStønadOpprettet!!.kravhaver.verdi).isEqualTo(Personident("Kravhaver1").verdi) },
            { assertThat(nyStønadOpprettet!!.mottaker.verdi).isEqualTo(Personident("Mottaker1").verdi) },
            { assertThat(nyStønadOpprettet!!.opprettetAv).isEqualTo("X123456") },
            { assertThat(nyStønadOpprettet!!.nesteIndeksreguleringsår).isEqualTo(2024) },
            { assertThat(nyStønadOpprettet!!.innkreving).isEqualTo(Innkrevingstype.MED_INNKREVING) },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-07")) },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].valutakode).isEqualTo("NOK") },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].resultatkode).isEqualTo("Hunky Dory") },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke opprette ny stønad fra Hendelse med ingen perioder`() {
        // Oppretter ny hendelse

        val periodeliste = mutableListOf<Periode>()

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = periodeliste,
            ),
        )

        val nyHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = null,
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(nyHendelse)

        val nyStønadOpprettet =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = nyHendelse.stønadsendringListe!![0].type,
                    sak = nyHendelse.stønadsendringListe!![0].sak,
                    skyldner = nyHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = nyHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(nyStønadOpprettet).isNull() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke opprette ny stønad fra Hendelse når beslutning er Stadfestelse eller Innkrevingstype = nei`() {
        // Oppretter ny hendelse

        val periodeliste = mutableListOf<Periode>()
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = LocalDate.parse("2021-07-01")),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.STADFESTELSE,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = periodeliste,
            ),
        )

        val nyHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = null,
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(nyHendelse)

        val nyStønadOpprettet =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = nyHendelse.stønadsendringListe!![0].type,
                    sak = nyHendelse.stønadsendringListe!![0].sak,
                    skyldner = nyHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = nyHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(nyStønadOpprettet).isNull() },
        )
    }

    // Tester at perioder som er endret i nytt vedtak blir satt til ugyldig og erstattet av nye perioder
    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal oppdatere eksisterende stønad med like fra- og til-datoer og ulike beløp, tester også på gyldigFra og gyldigTil`() {
        // Oppretter ny hendelse som etterpå skal oppdateres
        val originalPeriodeliste = mutableListOf<Periode>()
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-02-01")),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                ÅrMånedsperiode(LocalDate.parse("2021-02-01"), LocalDate.parse("2021-03-01")),
                BigDecimal.valueOf(17.02),
                "NOK",
                "Hunky Dory",
                "referanse2",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse3",
            ),
        )

        val originalStønadsendringListe = mutableListOf<Stønadsendring>()
        originalStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = originalPeriodeliste,
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-10-17T10:12:14.169121000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = originalStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)
        val originalStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = originalHendelse.stønadsendringListe!![0].type,
                    sak = originalHendelse.stønadsendringListe!![0].sak,
                    skyldner = originalHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = originalHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        // Oppretter hendelse for nytt vedtak på samme stønad, stønaden over skal da oppdateres. Det er kun midterste periode her som er endret
        // og skal oppdateres
        val periodeliste = mutableListOf<Periode>()
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-02-01")),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-02-01"), til = LocalDate.parse("2021-03-01")),
                beløp = BigDecimal.valueOf(100.02),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse2",
            ),
        )
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = LocalDate.parse("2021-04-01")),
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse3",
            ),
        )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = periodeliste,
            ),
        )

        val hendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 2,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-10-20T20:12:14.246785000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(hendelse)
        val oppdatertStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = hendelse.stønadsendringListe!![0].type,
                    sak = hendelse.stønadsendringListe!![0].sak,
                    skyldner = hendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = hendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        val allePerioderInkludertUgyldiggjorte = persistenceService.hentPerioderForStønadInkludertUgyldiggjorte(oppdatertStønad!!.stønadsid)

        assertAll(
            { assertThat(originalStønad!!).isNotNull() },
            { assertThat(originalStønad!!.periodeListe.size).isEqualTo(3) },
            { assertThat(oppdatertStønad.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-01")) },
            { assertThat(oppdatertStønad.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-02")) },
            { assertThat(oppdatertStønad.periodeListe[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(oppdatertStønad.periodeListe[0].valutakode).isEqualTo("NOK") },
            { assertThat(oppdatertStønad.periodeListe[0].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(oppdatertStønad.periodeListe[0].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785")) },
            { assertThat(oppdatertStønad.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-02")) },
            { assertThat(oppdatertStønad.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(oppdatertStønad.periodeListe[1].beløp).isEqualTo(BigDecimal.valueOf(100.02)) },
            { assertThat(oppdatertStønad.periodeListe[1].valutakode).isEqualTo("NOK") },
            { assertThat(oppdatertStønad.periodeListe[1].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(oppdatertStønad.periodeListe[1].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785")) },
            { assertThat(oppdatertStønad.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(oppdatertStønad.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2021-04")) },
            { assertThat(oppdatertStønad.periodeListe[2].beløp).isEqualTo(BigDecimal.valueOf(17.03)) },
            { assertThat(oppdatertStønad.periodeListe[2].valutakode).isEqualTo("NOK") },
            { assertThat(oppdatertStønad.periodeListe[2].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(oppdatertStønad.periodeListe[2].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785")) },
            { assertThat(allePerioderInkludertUgyldiggjorte.size).isEqualTo(6) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-17T10:12:14.169121000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].gyldigTil).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
        )
    }

    // Tester at ugyldiggjorte perioder og nyopprettede perioder som følge av en splitt får satt riktig gyldigFra og gyldigTil
    @Test
    @Suppress("NonAsciiCharacters")
    fun `test på at gyldigFra og gyldigTil blir satt riktig ved splitt av perioder`() {
        // Oppretter ny hendelse som etterpå skal oppdateres
        val originalPeriodeliste = mutableListOf<Periode>()
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2022-01-01")),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )

        val originalStønadsendringListe = mutableListOf<Stønadsendring>()
        originalStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = originalPeriodeliste,
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-10-17T10:12:14.169121000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = originalStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)

        // Oppretter hendelse for nytt vedtak på samme stønad, stønaden over skal da oppdateres. Den originale perioden skal ugyldiggjøres og
        // verdiene videreføres i to perioder, én før den nye perioden og én etter.
        val foersteEndringPeriodeliste = mutableListOf<Periode>()

        foersteEndringPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = LocalDate.parse("2021-08-01")),
                beløp = BigDecimal.valueOf(100.01),
                valutakode = "NOK",
                resultatkode = "Endring1",
                delytelseId = "referanse1",
            ),
        )

        val foersteEndringStønadsendringListe = mutableListOf<Stønadsendring>()
        foersteEndringStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = foersteEndringPeriodeliste,
            ),
        )

        val førsteEndringHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 2,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-10-20T20:12:14.246785000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = foersteEndringStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(førsteEndringHendelse)

        // Oppretter hendelse for nytt vedtak på samme stønad. Den siste av de nyopprettede splittperiode skal ugyldiggjøres, splttes på nytt, og
        // verdiene videreføres i to perioder, én før den nye perioden og én etter.
        val andreEndringPeriodeliste = mutableListOf<Periode>()

        andreEndringPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-10-01"), til = LocalDate.parse("2021-11-01")),
                beløp = BigDecimal.valueOf(200.02),
                valutakode = "NOK",
                resultatkode = "Endring2",
                delytelseId = "referanse2",
            ),
        )

        val andreEndringStønadsendringListe = mutableListOf<Stønadsendring>()
        andreEndringStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = andreEndringPeriodeliste,
            ),
        )

        val andreEndringHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 3,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-10-30T01:22:17.246755000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = andreEndringStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(andreEndringHendelse)

        val oppdatertStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = førsteEndringHendelse.stønadsendringListe!![0].type,
                    sak = førsteEndringHendelse.stønadsendringListe!![0].sak,
                    skyldner = førsteEndringHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = førsteEndringHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        val allePerioderInkludertUgyldiggjorte = persistenceService.hentPerioderForStønadInkludertUgyldiggjorte(oppdatertStønad!!.stønadsid)

        assertAll(
            { assertThat(oppdatertStønad.periodeListe.size).isEqualTo(5) },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].fom).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].til).isEqualTo(LocalDate.parse("2021-06-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[0].gyldigTil).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].fom).isEqualTo(LocalDate.parse("2021-06-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].til).isEqualTo(LocalDate.parse("2021-08-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].beløp).isEqualTo(BigDecimal.valueOf(100.01)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].resultatkode).isEqualTo("Endring1") },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[1].gyldigTil).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].fom).isEqualTo(LocalDate.parse("2021-08-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].til).isEqualTo(LocalDate.parse("2021-10-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-30T01:22:17.246755000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[2].gyldigTil).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].fom).isEqualTo(LocalDate.parse("2021-10-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].til).isEqualTo(LocalDate.parse("2021-11-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].beløp).isEqualTo(BigDecimal.valueOf(200.02)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].resultatkode).isEqualTo("Endring2") },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-30T01:22:17.246755000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[3].gyldigTil).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].fom).isEqualTo(LocalDate.parse("2021-11-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].til).isEqualTo(LocalDate.parse("2022-01-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-30T01:22:17.246755000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[4].gyldigTil).isNull() },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].fom).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].til).isEqualTo(LocalDate.parse("2022-01-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-17T10:12:14.169121000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[5].gyldigTil).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].fom).isEqualTo(LocalDate.parse("2021-08-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].til).isEqualTo(LocalDate.parse("2022-01-01")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].periodeGjortUgyldigAvVedtaksid).isEqualTo(3) },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].gyldigFra).isEqualTo(LocalDateTime.parse("2020-10-20T20:12:14.246785000")) },
            { assertThat(allePerioderInkludertUgyldiggjorte[6].gyldigTil).isEqualTo(LocalDateTime.parse("2020-10-30T01:22:17.246755000")) },
        )
    }

    // Tester at løpende stønad får satt sluttdato ved vedtak om opphør og at perioder lenger frem i tid ugyldiggjøres,
    // tester også nytt vedtak om gjenopptak av stønad
    @Test
    @Suppress("NonAsciiCharacters")
    fun `test på periodisering ved opphør og eksisterende perioder frem i tid`() {
        // Oppretter ny hendelse som etterpå skal oppdateres
        val originalPeriodeliste = mutableListOf<Periode>()
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-07-01")),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-07-01"), til = null),
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse2",
            ),
        )

        val originalStønadsendringListe = mutableListOf<Stønadsendring>()
        originalStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = originalPeriodeliste,
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-12-17T10:12:14.169121000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = originalStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)

        // Oppretter hendelse for vedtak om opphør på samme stønad, stønaden over skal da oppdateres. Den originale perioden skal få satt til =
        // periodeFra på opphørsperiode
        val opphoerPeriodeliste = mutableListOf<Periode>()

        opphoerPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = null),
                beløp = null,
                valutakode = null,
                resultatkode = "Opphoer",
                delytelseId = null,
            ),
        )

        val opphoerStønadsendringListe = mutableListOf<Stønadsendring>()
        opphoerStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = opphoerPeriodeliste,
            ),
        )

        val opphørHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 2,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2021-05-20T20:12:14.246785000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = opphoerStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(opphørHendelse)

        // Oppretter hendelse for nytt vedtak for å gjenoppta samme stønad.
        val gjenopptagelsePeriodeliste = mutableListOf<Periode>()

        gjenopptagelsePeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2022-02-01"), til = null),
                beløp = BigDecimal.valueOf(200.02),
                valutakode = "NOK",
                resultatkode = "Endring2",
                delytelseId = "referanse2",
            ),
        )

        val gjenopptagelseStønadsendringListe = mutableListOf<Stønadsendring>()
        gjenopptagelseStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = gjenopptagelsePeriodeliste,
            ),
        )

        val gjenopptagelseHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 3,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2022-01-30T01:22:17.246755000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = gjenopptagelseStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(gjenopptagelseHendelse)

        val gjenopptattStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = opphørHendelse.stønadsendringListe!![0].type,
                    sak = opphørHendelse.stønadsendringListe!![0].sak,
                    skyldner = opphørHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = opphørHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        val allePerioderEtterGjenopptagelse = persistenceService.hentPerioderForStønadInkludertUgyldiggjorte(gjenopptattStønad!!.stønadsid)

        assertAll(
            { assertThat(gjenopptattStønad.periodeListe.size).isEqualTo(2) },
            { assertThat(allePerioderEtterGjenopptagelse.size).isEqualTo(4) },
            { assertThat(allePerioderEtterGjenopptagelse[0].fom).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(allePerioderEtterGjenopptagelse[0].til).isEqualTo(LocalDate.parse("2021-06-01")) },
            { assertThat(allePerioderEtterGjenopptagelse[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderEtterGjenopptagelse[0].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterGjenopptagelse[0].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderEtterGjenopptagelse[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderEtterGjenopptagelse[0].gyldigFra).isEqualTo(LocalDateTime.parse("2021-05-20T20:12:14.246785000")) },
            { assertThat(allePerioderEtterGjenopptagelse[0].gyldigTil).isNull() },
            { assertThat(allePerioderEtterGjenopptagelse[1].fom).isEqualTo(LocalDate.parse("2022-02-01")) },
            { assertThat(allePerioderEtterGjenopptagelse[1].til).isNull() },
            { assertThat(allePerioderEtterGjenopptagelse[1].beløp).isEqualTo(BigDecimal.valueOf(200.02)) },
            { assertThat(allePerioderEtterGjenopptagelse[1].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterGjenopptagelse[1].resultatkode).isEqualTo("Endring2") },
            { assertThat(allePerioderEtterGjenopptagelse[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderEtterGjenopptagelse[1].gyldigFra).isEqualTo(LocalDateTime.parse("2022-01-30T01:22:17.246755000")) },
            { assertThat(allePerioderEtterGjenopptagelse[1].gyldigTil).isNull() },
            { assertThat(allePerioderEtterGjenopptagelse[2].fom).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(allePerioderEtterGjenopptagelse[2].til).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(allePerioderEtterGjenopptagelse[2].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderEtterGjenopptagelse[2].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterGjenopptagelse[2].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderEtterGjenopptagelse[2].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(allePerioderEtterGjenopptagelse[2].gyldigFra).isEqualTo(LocalDateTime.parse("2020-12-17T10:12:14.169121000")) },
            { assertThat(allePerioderEtterGjenopptagelse[2].gyldigTil).isEqualTo(LocalDateTime.parse("2021-05-20T20:12:14.246785000")) },
            { assertThat(allePerioderEtterGjenopptagelse[3].fom).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(allePerioderEtterGjenopptagelse[3].til).isNull() },
            { assertThat(allePerioderEtterGjenopptagelse[3].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(allePerioderEtterGjenopptagelse[3].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterGjenopptagelse[3].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderEtterGjenopptagelse[3].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(allePerioderEtterGjenopptagelse[3].gyldigFra).isEqualTo(LocalDateTime.parse("2020-12-17T10:12:14.169121000")) },
            { assertThat(allePerioderEtterGjenopptagelse[3].gyldigTil).isEqualTo(LocalDateTime.parse("2021-05-20T20:12:14.246785000")) },
        )
    }

    // Tester at løpende stønad med til = null får satt sluttdato ved vedtak om opphør
    @Test
    @Suppress("NonAsciiCharacters")
    fun `test på periodisering ved opphør`() {
        // Oppretter ny hendelse som etterpå skal opphøres
        val originalPeriodeliste = mutableListOf<Periode>()
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = LocalDate.parse("2021-07-01")),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-07-01"), til = null),
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse2",
            ),
        )

        val originalStønadsendringListe = mutableListOf<Stønadsendring>()
        originalStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = originalPeriodeliste,
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2020-12-17T10:12:14.169121000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = originalStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)

        // Oppretter hendelse for vedtak om opphør på samme stønad, stønaden over skal da oppdateres. Den originale perioden skal få
        // satt til = periodeFra på opphørsperiode
        val opphoerPeriodeliste = mutableListOf<Periode>()

        opphoerPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-11-01"), til = null),
                beløp = null,
                valutakode = null,
                resultatkode = "Opphoer",
                delytelseId = null,
            ),
        )

        val opphoerStønadsendringListe = mutableListOf<Stønadsendring>()
        opphoerStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = opphoerPeriodeliste,
            ),
        )

        val opphoerHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 2,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.parse("2021-05-20T20:12:14.246785000"),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = opphoerStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(opphoerHendelse)

        val oppdatertStønadEtterOpphoer =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = opphoerHendelse.stønadsendringListe!![0].type,
                    sak = opphoerHendelse.stønadsendringListe!![0].sak,
                    skyldner = opphoerHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = opphoerHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        val allePerioderEtterOpphoer =
            persistenceService.hentPerioderForStønadInkludertUgyldiggjorte(
                oppdatertStønadEtterOpphoer!!.stønadsid,
            )

        assertAll(
            { assertThat(oppdatertStønadEtterOpphoer.periodeListe.size).isEqualTo(2) },
            { assertThat(allePerioderEtterOpphoer.size).isEqualTo(3) },
            { assertThat(allePerioderEtterOpphoer[0].fom).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(allePerioderEtterOpphoer[0].til).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(allePerioderEtterOpphoer[0].beløp).isEqualTo(BigDecimal.valueOf(17.01)) },
            { assertThat(allePerioderEtterOpphoer[0].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterOpphoer[0].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderEtterOpphoer[0].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderEtterOpphoer[0].gyldigFra).isEqualTo(LocalDateTime.parse("2020-12-17T10:12:14.169121000")) },
            { assertThat(allePerioderEtterOpphoer[0].gyldigTil).isNull() },
            { assertThat(allePerioderEtterOpphoer[1].fom).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(allePerioderEtterOpphoer[1].til).isEqualTo(LocalDate.parse("2021-11-01")) },
            { assertThat(allePerioderEtterOpphoer[1].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(allePerioderEtterOpphoer[1].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterOpphoer[1].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderEtterOpphoer[1].periodeGjortUgyldigAvVedtaksid).isNull() },
            { assertThat(allePerioderEtterOpphoer[1].gyldigFra).isEqualTo(LocalDateTime.parse("2021-05-20T20:12:14.246785000")) },
            { assertThat(allePerioderEtterOpphoer[1].gyldigTil).isNull() },
            { assertThat(allePerioderEtterOpphoer[2].fom).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(allePerioderEtterOpphoer[2].til).isNull() },
            { assertThat(allePerioderEtterOpphoer[2].beløp).isEqualTo(BigDecimal.valueOf(17.02)) },
            { assertThat(allePerioderEtterOpphoer[2].valutakode).isEqualTo("NOK") },
            { assertThat(allePerioderEtterOpphoer[2].resultatkode).isEqualTo("Hunky Dory") },
            { assertThat(allePerioderEtterOpphoer[2].periodeGjortUgyldigAvVedtaksid).isEqualTo(2) },
            { assertThat(allePerioderEtterOpphoer[2].gyldigFra).isEqualTo(LocalDateTime.parse("2020-12-17T10:12:14.169121000")) },
            { assertThat(allePerioderEtterOpphoer[2].gyldigTil).isEqualTo(LocalDateTime.parse("2021-05-20T20:12:14.246785000")) },
        )
    }

    // Tester at mottaker blir oppdatert på eksisterende stønad
    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal oppdatere mottaker på eksisterende stønad`() {
        // Oppretter ny hendelse som etterpå skal oppdateres

        val originalPeriodeliste = mutableListOf<Periode>()
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-01-01"), til = null),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )

        val originalStønadsendringListe = mutableListOf<Stønadsendring>()
        originalStønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = originalPeriodeliste,
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = originalStønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)
        val originalStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = originalHendelse.stønadsendringListe!![0].type,
                    sak = originalHendelse.stønadsendringListe!![0].sak,
                    skyldner = originalHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = originalHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker2"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = emptyList(),
            ),
        )

        val hendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING_MOTTAKER,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(hendelse)
        val oppdatertStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = hendelse.stønadsendringListe!![0].type,
                    sak = hendelse.stønadsendringListe!![0].sak,
                    skyldner = hendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = hendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(originalStønad!!).isNotNull() },
            { assertThat(originalStønad!!.mottaker.verdi).isEqualTo(Personident("Mottaker1").verdi) },
            { assertThat(oppdatertStønad!!.mottaker.verdi).isEqualTo(Personident("Mottaker2").verdi) },
        )
    }

    // Tester at mottaker blir oppdatert på eksisterende stønad
    @Test
    @Suppress("NonAsciiCharacters")
    fun `test på at forsøk på å oppdatere mottaker på ikke-eksisterende stønad ikke forårsaker exceptions eller opprettelse av stønad`() {
        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker2"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = emptyList(),
            ),
        )

        val hendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING_MOTTAKER,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(hendelse)

        val ø =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = hendelse.stønadsendringListe!![0].type,
                    sak = hendelse.stønadsendringListe!![0].sak,
                    skyldner = hendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = hendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(ø).isNull() },
        )
    }

    // Tester at perioder på mottatt hendelse blir sortert etter fomdato og at til på lagret periode blir satt til lik
    // neste fomDato hvis mottatt til = null og det ikke er siste periode
    @Test
    @Suppress("NonAsciiCharacters")
    fun `test sortering av perioder på hendelse og justering av til på lagret periode hvis den er null`() {
        // Oppretter ny hendelse som etterpå skal oppdateres
        val originalPeriodeliste = mutableListOf<Periode>()
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2017-01-01"), til = null),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse1",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2014-02-01"), til = null),
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse2",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = null),
                beløp = BigDecimal.valueOf(17.03),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse3",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-03-01"), til = null),
                beløp = BigDecimal.valueOf(17.04),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse4",
            ),
        )
        originalPeriodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2010-03-01"), til = null),
                beløp = BigDecimal.valueOf(17.05),
                valutakode = "NOK",
                resultatkode = "Hunky Dory",
                delytelseId = "referanse5",
            ),
        )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("Sak1"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = originalPeriodeliste,
            ),
        )

        val hendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(hendelse)
        val opprettetStønad =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = hendelse.stønadsendringListe!![0].type,
                    sak = hendelse.stønadsendringListe!![0].sak,
                    skyldner = hendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = hendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(opprettetStønad!!).isNotNull() },
            { assertThat(opprettetStønad!!.periodeListe.size).isEqualTo(5) },
            { assertThat(opprettetStønad!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2010-03")) },
            { assertThat(opprettetStønad!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2014-02")) },
            { assertThat(opprettetStønad!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2014-02")) },
            { assertThat(opprettetStønad!!.periodeListe[1].periode.til).isEqualTo(YearMonth.parse("2017-01")) },
            { assertThat(opprettetStønad!!.periodeListe[2].periode.fom).isEqualTo(YearMonth.parse("2017-01")) },
            { assertThat(opprettetStønad!!.periodeListe[2].periode.til).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(opprettetStønad!!.periodeListe[3].periode.fom).isEqualTo(YearMonth.parse("2021-03")) },
            { assertThat(opprettetStønad!!.periodeListe[3].periode.til).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(opprettetStønad!!.periodeListe[4].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(opprettetStønad!!.periodeListe[4].periode.til).isNull() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal ikke opprette ny stønad fra Hendelse når beløp = null på alle perioder`() {
        // Oppretter ny hendelse

        val periodeliste = mutableListOf<Periode>()
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = LocalDate.parse("2021-07-01")),
                beløp = null,
                valutakode = "NOK",
                resultatkode = "AHI",
                delytelseId = "referanse1",
            ),
        )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = periodeliste,
            ),
        )

        val nyHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(nyHendelse)

        val nyStønadOpprettet =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = nyHendelse.stønadsendringListe!![0].type,
                    sak = nyHendelse.stønadsendringListe!![0].sak,
                    skyldner = nyHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = nyHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(nyStønadOpprettet).isNull() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `sjekk at justering av til bruker fomdato for neste periode selv om neste periode har beløp = null`() {
        // Oppretter ny hendelse

        val periodeliste = mutableListOf<Periode>()
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-06-01"), til = null),
                beløp = BigDecimal.valueOf(17.01),
                valutakode = "NOK",
                resultatkode = "Alles gut",
                delytelseId = null,
            ),
        )
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-09-01"), til = null),
                beløp = null,
                valutakode = "NOK",
                resultatkode = "AHI",
                delytelseId = null,
            ),
        )
        periodeliste.add(
            Periode(
                periode = ÅrMånedsperiode(fom = LocalDate.parse("2021-12-01"), til = null),
                beløp = BigDecimal.valueOf(17.02),
                valutakode = "NOK",
                resultatkode = "Alles gut",
                delytelseId = null,
            ),
        )

        val stønadsendringListe = mutableListOf<Stønadsendring>()
        stønadsendringListe.add(
            Stønadsendring(
                type = Stønadstype.BIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                førsteIndeksreguleringsår = 2024,
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                eksternReferanse = null,
                periodeListe = periodeliste,
            ),
        )

        val nyHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ALDERSJUSTERING,
                id = 1,
                opprettetAv = "R153961",
                opprettetAvNavn = "Sigge Saksbehandler",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = stønadsendringListe,
                engangsbeløpListe = emptyList(),
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(nyHendelse)

        val nyStønadOpprettet =
            beløpshistorikkService.hentStønad(
                HentStønadRequest(
                    type = nyHendelse.stønadsendringListe!![0].type,
                    sak = nyHendelse.stønadsendringListe!![0].sak,
                    skyldner = nyHendelse.stønadsendringListe!![0].skyldner,
                    kravhaver = nyHendelse.stønadsendringListe!![0].kravhaver,
                ),
            )

        assertAll(
            { assertThat(nyStønadOpprettet!!.periodeListe.size).isEqualTo(2) },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].periode.fom).isEqualTo(YearMonth.parse("2021-06")) },
            { assertThat(nyStønadOpprettet!!.periodeListe[0].periode.til).isEqualTo(YearMonth.parse("2021-09")) },
            { assertThat(nyStønadOpprettet!!.periodeListe[1].periode.fom).isEqualTo(YearMonth.parse("2021-12")) },
            { assertThat(nyStønadOpprettet!!.periodeListe[1].periode.til).isNull() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal opprette nytt engangsbeløp fra Hendelse`() {
        val engangsbeløpListe = mutableListOf<Engangsbeløp>()
        engangsbeløpListe.add(
            Engangsbeløp(
                type = Engangsbeløptype.SÆRBIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                beløp = BigDecimal.valueOf(5000),
                valutakode = "NOK",
                resultatkode = "SÆRBIDRAG_INNVILGET",
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                referanse = "Referanse",
                delytelseId = "DelytelseId",
                eksternReferanse = "EksternReferanse",
                betaltBeløp = BigDecimal.valueOf(1000),
            ),
        )

        val nyHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING,
                id = 1,
                opprettetAv = "X123456",
                opprettetAvNavn = "Navn",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = emptyList(),
                engangsbeløpListe = engangsbeløpListe,
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(nyHendelse)

        val nyttEngangsbeløpOpprettet =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = nyHendelse.engangsbeløpListe!![0].type,
                    sak = nyHendelse.engangsbeløpListe!![0].sak,
                    skyldner = nyHendelse.engangsbeløpListe!![0].skyldner,
                    kravhaver = nyHendelse.engangsbeløpListe!![0].kravhaver,
                    referanse = nyHendelse.engangsbeløpListe!![0].referanse,
                ),
            )

        assertAll(
            { assertThat(nyttEngangsbeløpOpprettet).isNotNull() },
            { assertThat(nyttEngangsbeløpOpprettet!!.type).isEqualTo(Engangsbeløptype.SÆRBIDRAG) },
            { assertThat(nyttEngangsbeløpOpprettet!!.sak.toString()).isEqualTo(Saksnummer("SAK-001").toString()) },
            { assertThat(nyttEngangsbeløpOpprettet!!.skyldner.verdi).isEqualTo(Personident("Skyldner1").verdi) },
            { assertThat(nyttEngangsbeløpOpprettet!!.kravhaver.verdi).isEqualTo(Personident("Kravhaver1").verdi) },
            { assertThat(nyttEngangsbeløpOpprettet!!.mottaker.verdi).isEqualTo(Personident("Mottaker1").verdi) },
            { assertThat(nyttEngangsbeløpOpprettet!!.vedtaksid).isEqualTo(1) },
            { assertThat(nyttEngangsbeløpOpprettet!!.gyldigFra.toLocalDate()).isEqualTo(LocalDate.now()) },
            { assertThat(nyttEngangsbeløpOpprettet!!.gyldigTil).isNull() },
            { assertThat(nyttEngangsbeløpOpprettet!!.gjortUgyldigAvVedtaksid).isNull() },
            { assertThat(nyttEngangsbeløpOpprettet!!.beløp).isEqualTo(BigDecimal.valueOf(5000).avrundetMedToDesimaler) },
            { assertThat(nyttEngangsbeløpOpprettet!!.betaltBeløp).isEqualTo(BigDecimal.valueOf(1000).avrundetMedToDesimaler) },
            { assertThat(nyttEngangsbeløpOpprettet!!.valutakode).isEqualTo("NOK") },
            { assertThat(nyttEngangsbeløpOpprettet!!.resultatkode).isEqualTo("SÆRBIDRAG_INNVILGET") },
            { assertThat(nyttEngangsbeløpOpprettet!!.innkreving).isEqualTo(Innkrevingstype.MED_INNKREVING) },
            { assertThat(nyttEngangsbeløpOpprettet!!.referanse).isEqualTo("Referanse") },
            { assertThat(nyttEngangsbeløpOpprettet!!.opprettetAv).isEqualTo("X123456") },
            { assertThat(nyttEngangsbeløpOpprettet!!.opprettetTidspunkt.toLocalDate()).isEqualTo(LocalDate.now()) },
            { assertThat(nyttEngangsbeløpOpprettet!!.endretAv).isNull() },
            { assertThat(nyttEngangsbeløpOpprettet!!.endretTidspunkt).isNull() },
        )
    }

    // Tester at mottaker blir oppdatert på eksisterende engangsbeløp
    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal oppdatere mottaker på eksisterende engangsbeløp`() {
        val originalEngangsbeløpListe = mutableListOf<Engangsbeløp>()
        originalEngangsbeløpListe.add(
            Engangsbeløp(
                type = Engangsbeløptype.SÆRBIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                beløp = BigDecimal.valueOf(5000),
                valutakode = "NOK",
                resultatkode = "SÆRBIDRAG_INNVILGET",
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                referanse = "Referanse",
                delytelseId = "DelytelseId",
                eksternReferanse = "EksternReferanse",
                betaltBeløp = BigDecimal.valueOf(1000),
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING,
                id = 1,
                opprettetAv = "X123456",
                opprettetAvNavn = "Navn",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = emptyList(),
                engangsbeløpListe = originalEngangsbeløpListe,
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)
        val originaltEngangsbeløp =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = originalHendelse.engangsbeløpListe!![0].type,
                    sak = originalHendelse.engangsbeløpListe!![0].sak,
                    skyldner = originalHendelse.engangsbeløpListe!![0].skyldner,
                    kravhaver = originalHendelse.engangsbeløpListe!![0].kravhaver,
                    referanse = originalHendelse.engangsbeløpListe!![0].referanse,
                ),
            )

        val engangsbeløpListe = mutableListOf<Engangsbeløp>()
        engangsbeløpListe.add(
            Engangsbeløp(
                type = Engangsbeløptype.SÆRBIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker2"),
                beløp = BigDecimal.valueOf(5000),
                valutakode = "NOK",
                resultatkode = "SÆRBIDRAG_INNVILGET",
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                referanse = "Referanse",
                delytelseId = "DelytelseId",
                eksternReferanse = "EksternReferanse",
                betaltBeløp = BigDecimal.valueOf(1000),
            ),
        )

        val hendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING_MOTTAKER,
                id = 1,
                opprettetAv = "X123456",
                opprettetAvNavn = "Navn",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = emptyList(),
                engangsbeløpListe = engangsbeløpListe,
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(hendelse)
        val oppdatertEngangsbeløp =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = hendelse.engangsbeløpListe!![0].type,
                    sak = hendelse.engangsbeløpListe!![0].sak,
                    skyldner = hendelse.engangsbeløpListe!![0].skyldner,
                    kravhaver = hendelse.engangsbeløpListe!![0].kravhaver,
                    referanse = hendelse.engangsbeløpListe!![0].referanse,
                ),
            )

        assertAll(
            { assertThat(originaltEngangsbeløp).isNotNull() },
            { assertThat(oppdatertEngangsbeløp).isNotNull() },
            { assertThat(originaltEngangsbeløp!!.mottaker.verdi).isEqualTo(Personident("Mottaker1").verdi) },
            { assertThat(oppdatertEngangsbeløp!!.mottaker.verdi).isEqualTo(Personident("Mottaker2").verdi) },
        )
    }

    // Tester at beløp blir oppdatert på eksisterende engangsbeløp
    @Test
    @Suppress("NonAsciiCharacters")
    fun `skal oppdatere beløp på eksisterende engangsbeløp`() {
        val originalEngangsbeløpListe = mutableListOf<Engangsbeløp>()
        originalEngangsbeløpListe.add(
            Engangsbeløp(
                type = Engangsbeløptype.SÆRBIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                beløp = BigDecimal.valueOf(5000),
                valutakode = "NOK",
                resultatkode = "SÆRBIDRAG_INNVILGET",
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                referanse = "Referanse",
                delytelseId = "DelytelseId",
                eksternReferanse = "EksternReferanse",
                betaltBeløp = BigDecimal.valueOf(1000),
            ),
        )

        val originalHendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING,
                id = 1,
                opprettetAv = "X123456",
                opprettetAvNavn = "Navn",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = emptyList(),
                engangsbeløpListe = originalEngangsbeløpListe,
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(originalHendelse)
        val originaltEngangsbeløp =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = originalHendelse.engangsbeløpListe!![0].type,
                    sak = originalHendelse.engangsbeløpListe!![0].sak,
                    skyldner = originalHendelse.engangsbeløpListe!![0].skyldner,
                    kravhaver = originalHendelse.engangsbeløpListe!![0].kravhaver,
                    referanse = originalHendelse.engangsbeløpListe!![0].referanse,
                ),
            )

        val engangsbeløpListe = mutableListOf<Engangsbeløp>()
        engangsbeløpListe.add(
            Engangsbeløp(
                type = Engangsbeløptype.SÆRBIDRAG,
                sak = Saksnummer("SAK-001"),
                skyldner = Personident("Skyldner1"),
                kravhaver = Personident("Kravhaver1"),
                mottaker = Personident("Mottaker1"),
                beløp = BigDecimal.valueOf(6000),
                valutakode = "NOK",
                resultatkode = "SÆRBIDRAG_INNVILGET",
                innkreving = Innkrevingstype.MED_INNKREVING,
                beslutning = Beslutningstype.ENDRING,
                omgjørVedtakId = null,
                referanse = "Referanse",
                delytelseId = "DelytelseId",
                eksternReferanse = "EksternReferanse",
                betaltBeløp = BigDecimal.valueOf(1000),
            ),
        )

        val hendelse =
            VedtakHendelse(
                kilde = Vedtakskilde.MANUELT,
                type = Vedtakstype.ENDRING,
                id = 1,
                opprettetAv = "X123456",
                opprettetAvNavn = "Navn",
                kildeapplikasjon = "Bisys",
                vedtakstidspunkt = LocalDateTime.now(),
                enhetsnummer = Enhetsnummer("enhetsnummer1"),
                innkrevingUtsattTilDato = null,
                fastsattILand = null,
                opprettetTidspunkt = LocalDateTime.now(),
                stønadsendringListe = emptyList(),
                engangsbeløpListe = engangsbeløpListe,
                behandlingsreferanseListe = emptyList(),
                sporingsdata = Sporingsdata(""),
            )

        behandleHendelseService.behandleHendelse(hendelse)
        val oppdatertEngangsbeløp =
            beløpshistorikkService.hentEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = hendelse.engangsbeløpListe!![0].type,
                    sak = hendelse.engangsbeløpListe!![0].sak,
                    skyldner = hendelse.engangsbeløpListe!![0].skyldner,
                    kravhaver = hendelse.engangsbeløpListe!![0].kravhaver,
                    referanse = hendelse.engangsbeløpListe!![0].referanse,
                ),
            )

        val historiskeEngangsbeløpListe =
            beløpshistorikkService.hentHistoriskeEngangsbeløp(
                HentEngangsbeløpRequest(
                    type = hendelse.engangsbeløpListe!![0].type,
                    sak = hendelse.engangsbeløpListe!![0].sak,
                    skyldner = hendelse.engangsbeløpListe!![0].skyldner,
                    kravhaver = hendelse.engangsbeløpListe!![0].kravhaver,
                    referanse = hendelse.engangsbeløpListe!![0].referanse,
                ),
            )

        assertAll(
            { assertThat(originaltEngangsbeløp).isNotNull() },
            { assertThat(oppdatertEngangsbeløp).isNotNull() },
            { assertThat(historiskeEngangsbeløpListe).isNotNull() },
            { assertThat(historiskeEngangsbeløpListe.size).isEqualTo(2) },
            { assertThat(originaltEngangsbeløp!!.beløp).isEqualTo(BigDecimal.valueOf(5000).avrundetMedToDesimaler) },
            { assertThat(oppdatertEngangsbeløp!!.beløp).isEqualTo(BigDecimal.valueOf(6000).avrundetMedToDesimaler) },
        )
    }
}
