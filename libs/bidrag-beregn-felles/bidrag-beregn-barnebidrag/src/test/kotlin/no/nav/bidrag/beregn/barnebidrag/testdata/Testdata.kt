package no.nav.bidrag.beregn.barnebidrag.testdata

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.generer.testdata.sak.genererSaksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.response.BidragPeriode
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidrag
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragJustertForBPBarnetillegg
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragTilFordeling
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.SamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.vedtak.response.BehandlingsreferanseDto
import no.nav.bidrag.transport.behandling.vedtak.response.StønadsendringDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakForStønad
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.sak.BidragssakDto
import no.nav.bidrag.transport.sak.RolleDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

val SOKNAD_ID = 12412421414L
val saksnummer = genererSaksnummer()
val saksnummer2 = "1234567"
val saksnummer3 = "3333333"
val personIdentSøknadsbarn1 = "33333"
val personIdentSøknadsbarn2 = "44444"
val personIdentAnnetbarn = "55555"
val personIdentAnnetbarn2 = "66666"
val personIdentBidragsmottaker = "22222"
val personIdentBidragsmottaker2 = "25555"
val personIdentBidragspliktig = "11111"
val identSamhandler = "554433"

fun opprettStønadDto(
    periodeListe: List<StønadPeriodeDto>,
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
    opprettetTidspunkt: LocalDateTime = LocalDateTime.parse("2025-01-01T00:00:00"),
) = StønadDto(
    sak = Saksnummer(saksnummer),
    skyldner = if (stønadstype == Stønadstype.BIDRAG) Personident(personIdentBidragspliktig) else personidentNav,
    kravhaver = Personident(personIdentSøknadsbarn1),
    mottaker = Personident(personIdentBidragsmottaker),
    førsteIndeksreguleringsår = 2025,
    innkreving = Innkrevingstype.MED_INNKREVING,
    opprettetAv = "",
    opprettetTidspunkt = opprettetTidspunkt,
    endretAv = null,
    endretTidspunkt = null,
    stønadsid = 1,
    type = stønadstype,
    nesteIndeksreguleringsår = 2025,
    periodeListe = periodeListe,
)

fun opprettStønadPeriodeDto(
    periode: ÅrMånedsperiode = ÅrMånedsperiode(LocalDate.parse("2024-08-01"), null),
    beløp: BigDecimal? = BigDecimal.ONE,
    valutakode: String = "NOK",
    vedtakId: Int = 1,
) = StønadPeriodeDto(
    stønadsid = 1,
    periodeid = 1,
    periodeGjortUgyldigAvVedtaksid = null,
    vedtaksid = vedtakId,
    gyldigFra = LocalDateTime.parse("2024-01-01T00:00:00"),
    gyldigTil = null,
    periode = periode,
    beløp = beløp,
    valutakode = valutakode,
    resultatkode = "OK",
)

fun opprettVedtakForStønad(
    kravhaver: String,
    stønadstype: Stønadstype,
) = VedtakForStønad(
    vedtaksid = 1,
    type = Vedtakstype.FASTSETTELSE,
    kilde = Vedtakskilde.MANUELT,
    vedtakstidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    behandlingsreferanser =
    listOf(
        BehandlingsreferanseDto(
            kilde = BehandlingsrefKilde.BISYS_SØKNAD,
            referanse =
            if (kravhaver == personIdentSøknadsbarn1) {
                SOKNAD_ID.toString()
            } else if (kravhaver == personIdentSøknadsbarn2) {
                124124231414L.toString()
            } else {
                12412435521414L.toString()
            },
        ),
    ),
    kildeapplikasjon = "",
    stønadsendring =
    StønadsendringDto(
        type = stønadstype,
        sak = Saksnummer(saksnummer),
        skyldner = Personident(personIdentBidragspliktig),
        kravhaver = Personident(kravhaver),
        mottaker = Personident(personIdentBidragsmottaker),
        førsteIndeksreguleringsår = 0,
        innkreving = Innkrevingstype.MED_INNKREVING,
        beslutning = Beslutningstype.ENDRING,
        omgjørVedtakId = null,
        eksternReferanse = "123456",
        grunnlagReferanseListe = emptyList(),
        sisteVedtaksid = null,
        periodeListe =
        listOf(
            VedtakPeriodeDto(
                periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                beløp = BigDecimal(5160),
                valutakode = "NOK",
                resultatkode = "KBB",
                delytelseId = null,
                grunnlagReferanseListe = emptyList(),
            ),
        ),
    ),
)

fun opprettSakRespons() = BidragssakDto(
    eierfogd = Enhetsnummer("4806"),
    saksnummer = Saksnummer("123213"),
    saksstatus = Bidragssakstatus.IN,
    kategori = Sakskategori.NASJONAL,
    opprettetDato = LocalDate.now(),
    levdeAdskilt = false,
    ukjentPart = false,
    roller =
    listOf(
        RolleDto(
            Personident(personIdentBidragsmottaker),
            type = Rolletype.BIDRAGSMOTTAKER,
        ),
        RolleDto(
            Personident(personIdentBidragspliktig),
            type = Rolletype.BIDRAGSPLIKTIG,
        ),
        RolleDto(
            Personident(personIdentSøknadsbarn1),
            type = Rolletype.BARN,
        ),
    ),
)

fun opprettVedtakForStønadBidragsberegning(
    skyldner: String,
    kravhaver: String,
    mottaker: String,
    sak: String,
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
    behandlingsrefKilde: BehandlingsrefKilde = BehandlingsrefKilde.BEHANDLING_ID,
    beregnetBeløp: BigDecimal,
) = VedtakForStønad(
    vedtaksid = 1,
    type = Vedtakstype.ENDRING,
    kilde = Vedtakskilde.MANUELT,
    vedtakstidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    behandlingsreferanser =
    listOf(
        BehandlingsreferanseDto(
            kilde = behandlingsrefKilde,
            referanse = "BEHANDLINGSREF",
        ),
    ),
    kildeapplikasjon = "",
    stønadsendring =
    StønadsendringDto(
        type = stønadstype,
        sak = Saksnummer(sak),
        skyldner = Personident(skyldner),
        kravhaver = Personident(kravhaver),
        mottaker = Personident(mottaker),
        førsteIndeksreguleringsår = 0,
        innkreving = Innkrevingstype.MED_INNKREVING,
        beslutning = Beslutningstype.ENDRING,
        omgjørVedtakId = null,
        eksternReferanse = "123456",
        grunnlagReferanseListe = emptyList(),
        sisteVedtaksid = null,
        periodeListe =
        listOf(
            VedtakPeriodeDto(
                periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                beløp = beregnetBeløp,
                valutakode = "NOK",
                resultatkode = "KBB",
                delytelseId = null,
                grunnlagReferanseListe = emptyList(),
            ),
        ),
    ),
)

fun opprettVedtakDtoForBidragsberegning(
    skyldner: String,
    kravhaver: String,
    mottaker: String,
    sak: String,
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
    beregnetBeløp: BigDecimal,
) = VedtakDto(
    kilde = Vedtakskilde.MANUELT,
    fastsattILand = "",
    type = Vedtakstype.ENDRING,
    opprettetAv = "",
    opprettetAvNavn = "",
    kildeapplikasjon = "bidrag-behandling",
    vedtakstidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    enhetsnummer = Enhetsnummer("4444"),
    innkrevingUtsattTilDato = null,
    opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    engangsbeløpListe = emptyList(),
    behandlingsreferanseListe =
    listOf(
        BehandlingsreferanseDto(
            kilde = BehandlingsrefKilde.BEHANDLING_ID,
            referanse = "BEHANDLINGSREF",
        ),
    ),
    grunnlagListe = listOf(
        GrunnlagDto(
            referanse = "sluttberegning_20240701",
            type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
            innhold = POJONode(
                SluttberegningBarnebidrag(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    beregnetBeløp = beregnetBeløp,
                    resultatBeløp = beregnetBeløp,
                    uMinusNettoBarnetilleggBM = beregnetBeløp,
                    bruttoBidragEtterBarnetilleggBM = beregnetBeløp,
                    nettoBidragEtterBarnetilleggBM = beregnetBeløp,
                    bruttoBidragJustertForEvneOg25Prosent = beregnetBeløp,
                    bruttoBidragEtterBarnetilleggBP = beregnetBeløp,
                    nettoBidragEtterSamværsfradrag = beregnetBeløp,
                    bpAndelAvUVedDeltBostedFaktor = BigDecimal.ZERO,
                    bpAndelAvUVedDeltBostedBeløp = BigDecimal.ZERO,
                ),
            ),
            grunnlagsreferanseListe = listOf("samværsperiode_20240701"),
        ),
        GrunnlagDto(
            referanse = "samværsperiode_20240701",
            type = Grunnlagstype.SAMVÆRSPERIODE,
            innhold = POJONode(
                SamværsperiodeGrunnlag(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                ),
            ),
        ),
    ),
    unikReferanse = "",
    stønadsendringListe = listOf(
        StønadsendringDto(
            type = stønadstype,
            sak = Saksnummer(sak),
            skyldner = Personident(skyldner),
            kravhaver = Personident(kravhaver),
            mottaker = Personident(mottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = listOf("sluttberegning_20240701"),
            sisteVedtaksid = null,
            periodeListe =
            listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    beløp = beregnetBeløp,
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = listOf("sluttberegning_20240701"),
                ),
            ),
        ),
    ),
)

fun opprettVedtakDtoForLøpendeBidrag(
    skyldner: String,
    kravhaver: String,
    mottaker: String,
    sak: String,
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
    beregnetBeløp: BigDecimal,
    resultatBeløp: BigDecimal,
) = VedtakDto(
    kilde = Vedtakskilde.MANUELT,
    fastsattILand = "",
    type = Vedtakstype.ENDRING,
    opprettetAv = "",
    opprettetAvNavn = "",
    kildeapplikasjon = "bidrag-behandling",
    vedtakstidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    enhetsnummer = Enhetsnummer("4444"),
    innkrevingUtsattTilDato = null,
    opprettetTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00"),
    engangsbeløpListe = emptyList(),
    behandlingsreferanseListe =
    listOf(
        BehandlingsreferanseDto(
            kilde = BehandlingsrefKilde.BEHANDLING_ID,
            referanse = "BEHANDLINGSREF",
        ),
    ),
    grunnlagListe = listOf(
        GrunnlagDto(
            referanse = "sluttberegning_20240701",
            type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
            innhold = POJONode(
                SluttberegningBarnebidragV2(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    beregnetBeløp = beregnetBeløp,
                    resultatBeløp = resultatBeløp,
                ),
            ),
            grunnlagsreferanseListe = listOf(
                "samværsperiode_20240701",
                "bidrag_til_fordeling_20240701",
                "bidrag_justert_for_bp_barnetillegg_20240701",
            ),
        ),
        GrunnlagDto(
            referanse = "samværsperiode_20240701",
            type = Grunnlagstype.SAMVÆRSPERIODE,
            innhold = POJONode(
                SamværsperiodeGrunnlag(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                ),
            ),
        ),
        GrunnlagDto(
            referanse = "bidrag_til_fordeling_20240701",
            type = Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING,
            innhold = POJONode(
                DelberegningBidragTilFordeling(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    bidragTilFordeling = BigDecimal.valueOf(2000),
                    uMinusNettoBarnetilleggBM = BigDecimal.valueOf(3000),
                    bpAndelAvUMinusSamværsfradrag = BigDecimal.valueOf(2000),
                    nettoBidragEtterBarnetilleggBM = BigDecimal.valueOf(1500),
                    bruttoBidragEtterBarnetilleggBM = BigDecimal.valueOf(2000),
                    erBidragJustertForNettoBarnetilleggBM = false,
                ),
            ),
        ),
        GrunnlagDto(
            referanse = "bidrag_justert_for_bp_barnetillegg_20240701",
            type = Grunnlagstype.DELBEREGNING_BIDRAG_JUSTERT_FOR_BP_BARNETILLEGG,
            innhold = POJONode(
                DelberegningBidragJustertForBPBarnetillegg(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    bidragJustertForNettoBarnetilleggBP = BigDecimal.valueOf(1000),
                    erBidragJustertTilNettoBarnetilleggBP = true,
                ),
            ),
        ),
    ),
    unikReferanse = "",
    stønadsendringListe = listOf(
        StønadsendringDto(
            type = stønadstype,
            sak = Saksnummer(sak),
            skyldner = Personident(skyldner),
            kravhaver = Personident(kravhaver),
            mottaker = Personident(mottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = listOf("sluttberegning_20240701"),
            sisteVedtaksid = null,
            periodeListe =
            listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    beløp = beregnetBeløp,
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = listOf("sluttberegning_20240701"),
                ),
            ),
        ),
    ),
)

fun opprettBidragBeregningResponsDto(
    periode: ÅrMånedsperiode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
    kravhaver: String,
    sak: String,
    datoSøknad: LocalDate = LocalDate.now().minusMonths(3),
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
    beregnetBeløp: BigDecimal,
    faktiskBeløp: BigDecimal,
    beløpSamvær: BigDecimal = BigDecimal.ZERO,
) = listOf(
    BidragBeregningResponsDto.BidragBeregning(
        periode = periode,
        saksnummer = sak,
        personidentBarn = Personident(kravhaver),
        datoSøknad = datoSøknad,
        beregnetBeløp = beregnetBeløp,
        faktiskBeløp = faktiskBeløp,
        beløpSamvær = beløpSamvær,
        stønadstype = stønadstype,
        samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
    ),
)

fun opprettGrunnlagsobjektForSøknadsbarn(søknadsbarnIdentMap: Map<Personident, String>): List<GrunnlagDto> = søknadsbarnIdentMap.map { barn ->
    GrunnlagDto(
        referanse = "person_PERSON_SØKNADSBARN_" + barn.key.verdi,
        gjelderReferanse = "bPReferanse",
        gjelderBarnReferanse = null,
        type = Grunnlagstype.PERSON_SØKNADSBARN,
        innhold = POJONode(
            Person(
                ident = barn.key,
                navn = null,
                fødselsdato = LocalDate.now().minusYears(10),
                bidragsmottaker = "bMReferanse",
                delAvOpprinneligBehandling = true,
            ),
        ),
    )
}

fun opprettGrunnlagobjektForBidragspliktig(ident: String): GrunnlagDto = GrunnlagDto(
    referanse = ident,
    gjelderReferanse = ident,
    gjelderBarnReferanse = null,
    type = Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
    innhold = POJONode(
        Person(
            ident = Personident(ident),
            navn = null,
            fødselsdato = LocalDate.now().minusYears(35),
            bidragsmottaker = null,
            delAvOpprinneligBehandling = true,
        ),
    ),
)

fun opprettLøpendeBidrag() = listOf(
    LøpendeBidrag(
        sak = Saksnummer(saksnummer),
        type = Stønadstype.BIDRAG,
        kravhaver = Personident(personIdentSøknadsbarn1),
        mottaker = Personident(personIdentBidragsmottaker),
        periodeListe = listOf(
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2022, 5), YearMonth.of(2022, 8)),
                løpendeBeløp = BigDecimal.valueOf(22250),
                valutakode = "NOK",
            ),
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2022, 8), YearMonth.of(2024, 5)),
                løpendeBeløp = BigDecimal.valueOf(22280),
                valutakode = "NOK",
            ),
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 5), null),
                løpendeBeløp = BigDecimal.valueOf(32450),
                valutakode = "NOK",
            ),
        ),
    ),
    LøpendeBidrag(
        sak = Saksnummer(saksnummer),
        type = Stønadstype.BIDRAG,
        kravhaver = Personident(personIdentSøknadsbarn2),
        mottaker = Personident(personIdentBidragsmottaker),
        periodeListe = listOf(
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 5), YearMonth.of(2024, 7)),
                løpendeBeløp = BigDecimal.valueOf(42450),
                valutakode = "NOK",
            ),
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 7), null),
                løpendeBeløp = BigDecimal.valueOf(42470),
                valutakode = "NOK",
            ),
        ),
    ),
    LøpendeBidrag(
        sak = Saksnummer(saksnummer2),
        type = Stønadstype.BIDRAG,
        kravhaver = Personident(personIdentAnnetbarn),
        mottaker = Personident(personIdentBidragsmottaker2),
        periodeListe = listOf(
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2020, 2), YearMonth.of(2024, 2)),
                løpendeBeløp = BigDecimal.valueOf(52020),
                valutakode = "NOK",
            ),
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 2), null),
                løpendeBeløp = BigDecimal.valueOf(52420),
                valutakode = "NOK",
            ),
        ),
    ),
)

fun opprettLøpendeOppfostringsOgUtlandsBidrag() = listOf(
    LøpendeBidrag(
        sak = Saksnummer(saksnummer2),
        type = Stønadstype.OPPFOSTRINGSBIDRAG,
        kravhaver = Personident(personIdentAnnetbarn),
        mottaker = Personident(personIdentBidragsmottaker),
        periodeListe = listOf(
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 5), null),
                løpendeBeløp = BigDecimal.valueOf(1000),
                valutakode = "NOK",
            ),
        ),
    ),
    LøpendeBidrag(
        sak = Saksnummer(saksnummer3),
        type = Stønadstype.BIDRAG,
        kravhaver = Personident(personIdentAnnetbarn2),
        mottaker = Personident(personIdentBidragsmottaker2),
        periodeListe = listOf(
            BidragPeriode(
                periode = ÅrMånedsperiode(YearMonth.of(2024, 2), null),
                løpendeBeløp = BigDecimal.valueOf(2000),
                valutakode = "NOK",
            ),
        ),
    ),
)

fun opprettFlereVedtakBBMOgBehandlingBarn1(): List<VedtakForStønad> = listOf(
    VedtakForStønad(
        vedtaksid = 1,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusMonths(22),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "1",
            ),
        ),
        kildeapplikasjon = "",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2022-07-01"), LocalDate.parse("2022-09-01")),
                    beløp = BigDecimal(12270),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2022-09-01"), null),
                    beløp = BigDecimal(12290),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
    VedtakForStønad(
        vedtaksid = 2,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusMonths(10),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "2",
            ),
        ),
        kildeapplikasjon = "",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2022-05-01"), LocalDate.parse("2022-08-01")),
                    beløp = BigDecimal(22250),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2022-08-01"), null),
                    beløp = BigDecimal(22280),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
    VedtakForStønad(
        vedtaksid = 3,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "3",
            ),
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BEHANDLING_ID,
                referanse = "33",
            ),
        ),
        kildeapplikasjon = "",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), null),
                    beløp = BigDecimal(32450),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
)

fun opprettFlereVedtakBBMOgBehandlingBarn2(): List<VedtakForStønad> = listOf(
    VedtakForStønad(
        vedtaksid = 4,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusMonths(4),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "4",
            ),
        ),
        kildeapplikasjon = "",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentSøknadsbarn2),
            mottaker = Personident(personIdentBidragsmottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = null,
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), LocalDate.parse("2024-07-01")),
                    beløp = BigDecimal(42450),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
                    beløp = BigDecimal(42470),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
)

fun opprettFlereVedtakBBMOgBehandlingBarn3(): List<VedtakForStønad> = listOf(
    VedtakForStønad(
        vedtaksid = 5,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusYears(5),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "5",
            ),
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BEHANDLING_ID,
                referanse = "55",
            ),
        ),
        kildeapplikasjon = "",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer2),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentAnnetbarn),
            mottaker = Personident(personIdentBidragsmottaker2),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2020-02-01"), LocalDate.parse("2024-02-01")),
                    beløp = BigDecimal(52020),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-02-01"), null),
                    beløp = BigDecimal(52420),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
)

fun opprettBeregningResponsBBM(
    datoSøknad: LocalDate = LocalDate.now().minusMonths(3),
    stønadstype: Stønadstype = Stønadstype.BIDRAG,
) = listOf(
    BidragBeregningResponsDto.BidragBeregning(
        periode = ÅrMånedsperiode(LocalDate.parse("2022-05-01"), LocalDate.parse("2022-08-01")),
        saksnummer = saksnummer,
        personidentBarn = Personident(personIdentSøknadsbarn1),
        datoSøknad = datoSøknad,
        beregnetBeløp = BigDecimal.valueOf(22250),
        faktiskBeløp = BigDecimal.valueOf(22250),
        beløpSamvær = BigDecimal.ZERO,
        stønadstype = stønadstype,
        samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
    ),
    BidragBeregningResponsDto.BidragBeregning(
        periode = ÅrMånedsperiode(LocalDate.parse("2022-08-01"), null),
        saksnummer = saksnummer,
        personidentBarn = Personident(personIdentSøknadsbarn1),
        datoSøknad = datoSøknad,
        beregnetBeløp = BigDecimal.valueOf(22280),
        faktiskBeløp = BigDecimal.valueOf(22280),
        beløpSamvær = BigDecimal.ZERO,
        stønadstype = stønadstype,
        samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
    ),
    BidragBeregningResponsDto.BidragBeregning(
        periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), LocalDate.parse("2024-07-01")),
        saksnummer = saksnummer,
        personidentBarn = Personident(personIdentSøknadsbarn2),
        datoSøknad = datoSøknad,
        beregnetBeløp = BigDecimal.valueOf(42450),
        faktiskBeløp = BigDecimal.valueOf(42450),
        beløpSamvær = BigDecimal.ZERO,
        stønadstype = stønadstype,
        samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
    ),
    BidragBeregningResponsDto.BidragBeregning(
        periode = ÅrMånedsperiode(LocalDate.parse("2024-07-01"), null),
        saksnummer = saksnummer,
        personidentBarn = Personident(personIdentSøknadsbarn2),
        datoSøknad = datoSøknad,
        beregnetBeløp = BigDecimal.valueOf(42470),
        faktiskBeløp = BigDecimal.valueOf(42470),
        beløpSamvær = BigDecimal.ZERO,
        stønadstype = stønadstype,
        samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
    ),
)

fun hentVedtak3() = VedtakDto(
    kilde = Vedtakskilde.MANUELT,
    fastsattILand = "",
    type = Vedtakstype.ENDRING,
    opprettetAv = "",
    opprettetAvNavn = "",
    kildeapplikasjon = "bidrag-behandling",
    vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
    enhetsnummer = null,
    innkrevingUtsattTilDato = null,
    opprettetTidspunkt = LocalDateTime.now(),
    engangsbeløpListe = emptyList(),
    behandlingsreferanseListe = emptyList(),
    grunnlagListe = opprettGrunnlagSluttberegningBarn1(),
    unikReferanse = "",
    stønadsendringListe = listOf(
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentSøknadsbarn1),
            mottaker = Personident(personIdentBidragsmottaker),
            skyldner = Personident(personIdentBidragspliktig),
            sak = Saksnummer(saksnummer),
            førsteIndeksreguleringsår = null,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = null,
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe =
            listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-05"), null),
                    resultatkode = Resultatkode.BEREGNET_BIDRAG.name,
                    beløp = BigDecimal(32450),
                    delytelseId = null,
                    valutakode = "NOK",
                    grunnlagReferanseListe = listOf(opprettGrunnlagSluttberegningBarn1().first().referanse),
                ),
            ),
        ),
    ),
)

fun hentVedtak5() = VedtakDto(
    kilde = Vedtakskilde.MANUELT,
    fastsattILand = "",
    type = Vedtakstype.ENDRING,
    opprettetAv = "",
    opprettetAvNavn = "",
    kildeapplikasjon = "bidrag-behandling",
    vedtakstidspunkt = LocalDateTime.now().minusYears(5),
    enhetsnummer = null,
    innkrevingUtsattTilDato = null,
    opprettetTidspunkt = LocalDateTime.now(),
    engangsbeløpListe = emptyList(),
    behandlingsreferanseListe = emptyList(),
    grunnlagListe = opprettGrunnlagSluttberegningBarn3(),
    unikReferanse = "",
    stønadsendringListe = listOf(
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            kravhaver = Personident(personIdentAnnetbarn),
            mottaker = Personident(personIdentBidragsmottaker2),
            skyldner = Personident(personIdentBidragspliktig),
            sak = Saksnummer(saksnummer2),
            førsteIndeksreguleringsår = null,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = null,
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe =
            listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(YearMonth.parse("2020-02"), YearMonth.parse("2024-02")),
                    resultatkode = Resultatkode.BEREGNET_BIDRAG.name,
                    beløp = BigDecimal(52020),
                    delytelseId = null,
                    valutakode = "NOK",
                    grunnlagReferanseListe = listOf(opprettGrunnlagSluttberegningBarn3().first().referanse),
                ),
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(YearMonth.parse("2024-02"), null),
                    resultatkode = Resultatkode.BEREGNET_BIDRAG.name,
                    beløp = BigDecimal(52420),
                    delytelseId = null,
                    valutakode = "NOK",
                    grunnlagReferanseListe = listOf(opprettGrunnlagSluttberegningBarn3()[1].referanse),
                ),
            ),
        ),
    ),
)

fun opprettGrunnlagSluttberegningBarn1(): List<GrunnlagDto> = listOf(
    GrunnlagDto(
        referanse = "sluttberegning_barnebidrag",
        type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
        grunnlagsreferanseListe = listOf(
            opprettGrunnlagDelberegningAndel().referanse,
            opprettGrunnlagDelberegningUnderholdskostnad().referanse,
            "samvær_SAMVÆRSPERIODE_20240101_person_PERSON_SØKNADSBARN_1",
        ),
        innhold =
        POJONode(
            SluttberegningBarnebidragV2(
                periode = ÅrMånedsperiode(YearMonth.parse("2024-05"), null),
                beregnetBeløp = BigDecimal("32450"),
                resultatBeløp = BigDecimal("32450"),
            ),
        ),
        gjelderReferanse = personIdentBidragspliktig,
        gjelderBarnReferanse = personIdentSøknadsbarn1,
    ),
    GrunnlagDto(
        referanse = "samvær_SAMVÆRSPERIODE_20240101_person_PERSON_SØKNADSBARN_1",
        type = Grunnlagstype.SAMVÆRSPERIODE,
        innhold =
        POJONode(
            SamværsperiodeGrunnlag(
                periode = ÅrMånedsperiode(YearMonth.parse("2024-05"), null),
                samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                manueltRegistrert = true,
            ),
        ),
        grunnlagsreferanseListe = emptyList(),
        gjelderReferanse = personIdentBidragspliktig,
        gjelderBarnReferanse = personIdentSøknadsbarn1,
    ),
)

fun opprettGrunnlagSluttberegningBarn3(): List<GrunnlagDto> = listOf(
    GrunnlagDto(
        referanse = "sluttberegning_barnebidrag_2020_02",
        type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
        grunnlagsreferanseListe = listOf(
            opprettGrunnlagDelberegningAndel().referanse,
            opprettGrunnlagDelberegningUnderholdskostnad().referanse,
            "samvær_SAMVÆRSPERIODE_20240101_person_PERSON_BARN_3",
        ),
        innhold =
        POJONode(
            SluttberegningBarnebidragV2(
                periode = ÅrMånedsperiode(YearMonth.parse("2020-02"), YearMonth.parse("2024-02")),
                beregnetBeløp = BigDecimal("52020"),
                resultatBeløp = BigDecimal("52020"),
            ),
        ),
        gjelderReferanse = personIdentBidragspliktig,
        gjelderBarnReferanse = personIdentAnnetbarn,
    ),
    GrunnlagDto(
        referanse = "sluttberegning_barnebidrag_2024_02",
        type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
        grunnlagsreferanseListe = listOf(
            opprettGrunnlagDelberegningAndel().referanse,
            opprettGrunnlagDelberegningUnderholdskostnad().referanse,
            "samvær_SAMVÆRSPERIODE_20240101_person_PERSON_BARN_3",
        ),
        innhold =
        POJONode(
            SluttberegningBarnebidragV2(
                periode = ÅrMånedsperiode(YearMonth.parse("2024-02"), null),
                beregnetBeløp = BigDecimal("52420"),
                resultatBeløp = BigDecimal("52420"),
            ),
        ),
        gjelderReferanse = personIdentBidragspliktig,
        gjelderBarnReferanse = personIdentAnnetbarn,
    ),
    GrunnlagDto(
        referanse = "samvær_SAMVÆRSPERIODE_20240101_person_PERSON_BARN_3",
        type = Grunnlagstype.SAMVÆRSPERIODE,
        innhold =
        POJONode(
            SamværsperiodeGrunnlag(
                periode = ÅrMånedsperiode(YearMonth.parse("2020-02"), null),
                samværsklasse = Samværsklasse.SAMVÆRSKLASSE_0,
                manueltRegistrert = true,
            ),
        ),
        grunnlagsreferanseListe = emptyList(),
        gjelderReferanse = personIdentBidragspliktig,
        gjelderBarnReferanse = personIdentSøknadsbarn1,
    ),
)

fun opprettVedtakOppfostringBarn1(): List<VedtakForStønad> = listOf(
    VedtakForStønad(
        vedtaksid = 1,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusMonths(22),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "1",
            ),
        ),
        kildeapplikasjon = "",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.OPPFOSTRINGSBIDRAG,
            sak = Saksnummer(saksnummer2),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentAnnetbarn),
            mottaker = Personident(identSamhandler),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-05-01"), null),
                    beløp = BigDecimal(1000),
                    valutakode = "NOK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
)

fun opprettVedtakUtlandBarn2(): List<VedtakForStønad> = listOf(
    VedtakForStønad(
        vedtaksid = 2,
        type = Vedtakstype.FASTSETTELSE,
        kilde = Vedtakskilde.MANUELT,
        vedtakstidspunkt = LocalDateTime.now().minusMonths(5),
        behandlingsreferanser =
        listOf(
            BehandlingsreferanseDto(
                kilde = BehandlingsrefKilde.BISYS_SØKNAD,
                referanse = "2",
            ),
        ),
        kildeapplikasjon = "bisys",
        stønadsendring =
        StønadsendringDto(
            type = Stønadstype.BIDRAG,
            sak = Saksnummer(saksnummer3),
            skyldner = Personident(personIdentBidragspliktig),
            kravhaver = Personident(personIdentAnnetbarn2),
            mottaker = Personident(personIdentBidragsmottaker),
            førsteIndeksreguleringsår = 0,
            innkreving = Innkrevingstype.MED_INNKREVING,
            beslutning = Beslutningstype.ENDRING,
            omgjørVedtakId = null,
            eksternReferanse = "123456",
            grunnlagReferanseListe = emptyList(),
            sisteVedtaksid = null,
            periodeListe = listOf(
                VedtakPeriodeDto(
                    periode = ÅrMånedsperiode(LocalDate.parse("2024-02-01"), null),
                    beløp = BigDecimal(2000),
                    valutakode = "SEK",
                    resultatkode = "KBB",
                    delytelseId = null,
                    grunnlagReferanseListe = emptyList(),
                ),
            ),
        ),
    ),
)
