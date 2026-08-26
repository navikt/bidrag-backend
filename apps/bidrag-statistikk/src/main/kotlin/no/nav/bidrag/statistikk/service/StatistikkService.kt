package no.nav.bidrag.statistikk.service

import no.nav.bidrag.beregn.core.util.justerVedtakstidspunktVedtakshendelse
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.vedtak.Beslutningstype
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.statistikk.consumer.BidragVedtakConsumer
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragspliktigesAndel
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningFaktiskTilsynsutgift
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningNettoBarnetillegg
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningNettoTilsynsutgift
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSamværsfradrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningUnderholdskostnad
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningVoksneIHustand
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.KopiSamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SamværsperiodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragAldersjustering
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningForskudd
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnOgKonverterGrunnlagSomErReferertAv
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedReferanseKonvertert
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.særbidragskategori
import no.nav.bidrag.transport.behandling.felles.grunnlag.utgiftsposter
import no.nav.bidrag.transport.behandling.statistikk.BidragHendelse
import no.nav.bidrag.transport.behandling.statistikk.BidragPeriode
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import no.nav.bidrag.transport.behandling.statistikk.ForskuddPeriode
import no.nav.bidrag.transport.behandling.statistikk.Inntekt
import no.nav.bidrag.transport.behandling.statistikk.SærbidragHendelse
import no.nav.bidrag.transport.behandling.vedtak.VedtakHendelse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.erDelvedtak
import no.nav.bidrag.transport.behandling.vedtak.response.erOrkestrertVedtak
import no.nav.bidrag.transport.behandling.vedtak.response.referertVedtaksid
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional
class StatistikkService(val hendelserService: HendelserService, val bidragVedtakConsumer: BidragVedtakConsumer) {

    val bisys = "bisys"

    // Behandler mottatt vedtak og sender videre på statistikk-topics
    fun behandleVedtakshendelse(vedtakHendelse: VedtakHendelse) {
        val vedtakDto = hentVedtak(vedtakHendelse.id)
        if (vedtakDto == null) {
            LOGGER.warn("Vedtak med vedtaksid ${vedtakHendelse.id} ikke funnet ved hent av vedtak fra bidrag-vedtak, hopper over vedtakshendelse")
            SECURE_LOGGER.warn(
                "Vedtak med vedtaksid ${vedtakHendelse.id} ikke funnet ved hent av vedtak fra bidrag-vedtak, hopper over vedtakshendelse",
            )
            return
        }

        LOGGER.info("Henter komplett vedtak for vedtaksid: ${vedtakHendelse.id}")
        SECURE_LOGGER.debug("Henter komplett vedtak for vedtaksid: {} vedtak: {}", vedtakHendelse.id, vedtakDto)

        behandleVedtakHendelseForskudd(vedtakHendelse, vedtakDto)

        behandleVedtakHendelseBidrag(vedtakHendelse, vedtakDto)

        behandleVedtakHendelseSærbidrag(vedtakHendelse, vedtakDto)
    }

    private fun behandleVedtakHendelseForskudd(vedtakHendelse: VedtakHendelse, vedtakDto: VedtakDto) {
        vedtakDto.stønadsendringListe.filter { it.type == Stønadstype.FORSKUDD && it.beslutning == Beslutningstype.ENDRING }
            .forEach { stønadsendring ->
                val forskuddHendelse = ForskuddHendelse(
                    vedtaksid = vedtakHendelse.id,
                    vedtakstidspunkt = vedtakHendelse.justerVedtakstidspunktVedtakshendelse().vedtakstidspunkt,
                    type = vedtakHendelse.type.name,
                    saksnr = stønadsendring.sak.verdi,
                    kravhaver = stønadsendring.kravhaver.verdi,
                    mottaker = stønadsendring.mottaker.verdi,
                    historiskVedtak = vedtakDto.kildeapplikasjon.contains(bisys),
                    forskuddPeriodeListe = stønadsendring.periodeListe.map { periode ->
                        val grunnlagsdata =
                            finnGrunnlagsdataForskudd(vedtakDto.grunnlagListe, periode.grunnlagReferanseListe, stønadsendring.skyldner.verdi)

                        if ((
                                grunnlagsdata?.barnetsAldersgruppe == null ||
                                    grunnlagsdata.antallBarnIEgenHusstand == null ||
                                    grunnlagsdata.sivilstand == null ||
                                    grunnlagsdata.barnBorMedMottaker == null ||
                                    grunnlagsdata.mottakerInntektListe?.isEmpty() == true
                                ) &&
                            !vedtakDto.kildeapplikasjon.contains(bisys)
                        ) {
                            SECURE_LOGGER.info(
                                "Fullstendig grunnlag ikke funnet for forskuddsvedtak med vedtaksid: {}, vedtakstype: {}, " +
                                    "resultatkode: {}, beløp: {}",
                                vedtakHendelse.id,
                                vedtakDto.type,
                                periode.resultatkode,
                                periode.beløp,
                            )
                        }
                        ForskuddPeriode(
                            periodeFra = LocalDate.of(periode.periode.fom.year, periode.periode.fom.month, 1),
                            periodeTil = if (periode.periode.til == null) {
                                null
                            } else {
                                LocalDate.of(
                                    periode.periode.til!!.year,
                                    periode.periode.til!!.month,
                                    1,
                                )
                            },
                            beløp = periode.beløp,
                            resultat = periode.resultatkode,
                            barnetsAldersgruppe = grunnlagsdata?.barnetsAldersgruppe,
                            antallBarnIEgenHusstand = grunnlagsdata?.antallBarnIEgenHusstand,
                            sivilstand = grunnlagsdata?.sivilstand,
                            barnBorMedMottaker = grunnlagsdata?.barnBorMedMottaker,
                            mottakerInntektListe = grunnlagsdata?.mottakerInntektListe ?: emptyList(),
                            kravhaverInntektListe = grunnlagsdata?.kravhaverInntektListe ?: emptyList(),
                        )
                    },
                )
                hendelserService.opprettForskuddshendelse(forskuddHendelse)
            }
    }

    private fun behandleVedtakHendelseBidrag(vedtakHendelse: VedtakHendelse, vedtakDto: VedtakDto) {
        val vedtakErAldersjustering = vedtakHendelse.type == Vedtakstype.ALDERSJUSTERING
        // Spesialinnhenting av grunnlag på aldersjusteringsvedtak skal bare gjøres for vedtak som ikke er fra Bisys.
        // Aldersjusteringsvedtak gjort i Bisys har samme grunnlagsstruktur som ordinære bidragsvedtak.
        val vedtakFraBisys = vedtakHendelse.kildeapplikasjon.contains(bisys)
        vedtakDto.stønadsendringListe.filter {
            (
                it.type == Stønadstype.BIDRAG ||
                    it.type == Stønadstype.BIDRAG18AAR ||
                    it.type == Stønadstype.OPPFOSTRINGSBIDRAG
                ) &&
                it.beslutning == Beslutningstype.ENDRING
        }
            .forEach { stønadsendring ->
                val bidragHendelse = BidragHendelse(
                    vedtaksid = vedtakHendelse.id,
                    vedtakstidspunkt = vedtakHendelse.justerVedtakstidspunktVedtakshendelse().vedtakstidspunkt,
                    stønadstype = stønadsendring.type,
                    type = vedtakHendelse.type.name,
                    saksnr = stønadsendring.sak.verdi,
                    skyldner = stønadsendring.skyldner.verdi,
                    kravhaver = stønadsendring.kravhaver.verdi,
                    mottaker = stønadsendring.mottaker.verdi,
                    historiskVedtak = vedtakFraBisys,
                    innkreving = stønadsendring.innkreving == Innkrevingstype.MED_INNKREVING,
                    bidragPeriodeListe = stønadsendring.periodeListe.map { periode ->
                        val grunnlagsdata =
                            finnGrunnlagsdataBidragsperiode(
                                vedtakErAldersjustering,
                                vedtakFraBisys,
                                vedtakDto.grunnlagListe,
                                periode.grunnlagReferanseListe,
                                stønadsendring.kravhaver.verdi,
                            )

                        // Sjekker på de grunnlagstypene som alltid skal være med og logger hvis noen av de mangler
                        if ((
                                grunnlagsdata?.bidragsevne == null ||
                                    grunnlagsdata.underholdskostnad == null ||
                                    grunnlagsdata.skyldnersAndelUnderholdskostnad == null ||
                                    grunnlagsdata.skyldnerBorMedAndreVoksne == null ||
                                    grunnlagsdata.samværsklasse == null ||
                                    grunnlagsdata.skyldnerInntektListe?.isEmpty() == true
                                ) &&
                            !vedtakFraBisys &&
                            !vedtakErAldersjustering
                        ) {
                            SECURE_LOGGER.info(
                                "Fullstendig grunnlag ikke funnet for bidragsvedtak med vedtaksid: {}, vedtakstype: {}, resultatkode: {}, beløp: {}",
                                vedtakHendelse.id,
                                vedtakDto.type,
                                periode.resultatkode,
                                periode.beløp,
                            )
                        }
                        BidragPeriode(
                            periodeFra = LocalDate.of(periode.periode.fom.year, periode.periode.fom.month, 1),
                            periodeTil = if (periode.periode.til == null) {
                                null
                            } else {
                                LocalDate.of(
                                    periode.periode.til!!.year,
                                    periode.periode.til!!.month,
                                    1,
                                )
                            },
                            beløp = periode.beløp,
                            valutakode = periode.valutakode,
                            resultat = periode.resultatkode,
                            bidragsevne = grunnlagsdata?.bidragsevne,
                            underholdskostnad = grunnlagsdata?.underholdskostnad,
                            skyldnersAndelUnderholdskostnad = grunnlagsdata?.skyldnersAndelUnderholdskostnad,
                            nettoTilsynsutgift = grunnlagsdata?.nettoTilsynsutgift,
                            faktiskUtgift = grunnlagsdata?.faktiskUtgift,
                            samværsfradrag = grunnlagsdata?.samværsfradrag,
                            nettoBarnetilleggSkyldner = grunnlagsdata?.nettoBarnetilleggSkyldner,
                            nettoBarnetilleggMottaker = grunnlagsdata?.nettoBarnetilleggMottaker,
                            skyldnerBorMedAndreVoksne = grunnlagsdata?.skyldnerBorMedAndreVoksne,
                            samværsklasse = grunnlagsdata?.samværsklasse,
                            skyldnerInntektListe = grunnlagsdata?.skyldnerInntektListe ?: emptyList(),
                            mottakerInntektListe = grunnlagsdata?.mottakerInntektListe ?: emptyList(),
                            kravhaverInntektListe = grunnlagsdata?.kravhaverInntektListe ?: emptyList(),
                        )
                    },
                )
                hendelserService.opprettBidragshendelse(bidragHendelse)
            }
    }

    private fun behandleVedtakHendelseSærbidrag(vedtakHendelse: VedtakHendelse, vedtakDto: VedtakDto) {
        val vedtakFraBisys = vedtakHendelse.kildeapplikasjon.contains(bisys)
        vedtakDto.engangsbeløpListe.filter {
            (it.type == Engangsbeløptype.SÆRBIDRAG || it.type == Engangsbeløptype.SAERTILSKUDD) && it.beslutning == Beslutningstype.ENDRING
        }
            .forEach { særbidrag ->
                val grunnlagsdata =
                    finnGrunnlagsdataSærbidrag(
                        vedtakFraBisys = vedtakFraBisys,
                        vedtakDto.grunnlagListe,
                        særbidrag.grunnlagReferanseListe,
                        særbidrag.kravhaver.verdi,
                    )
                val særbidragshendelse = SærbidragHendelse(
                    vedtaksid = vedtakHendelse.id,
                    vedtakstidspunkt = vedtakHendelse.justerVedtakstidspunktVedtakshendelse().vedtakstidspunkt,
                    type = vedtakHendelse.type.name,
                    kategori = grunnlagsdata?.kategori,
                    saksnr = særbidrag.sak.verdi,
                    skyldner = særbidrag.skyldner.verdi,
                    kravhaver = særbidrag.kravhaver.verdi,
                    mottaker = særbidrag.mottaker.verdi,
                    referanse = særbidrag.referanse,
                    beløp = særbidrag.beløp,
                    valutakode = særbidrag.valutakode,
                    resultat = særbidrag.resultatkode,
                    innkreving = særbidrag.innkreving == Innkrevingstype.MED_INNKREVING,
                    omgjørVedtakId = særbidrag.omgjørVedtakId,
                    historiskVedtak = vedtakDto.kildeapplikasjon.contains(bisys),
                    kravbeløp = grunnlagsdata?.kravbeløp,
                    godkjentBeløp = grunnlagsdata?.godkjentBeløp,
                    betaltBeløp = særbidrag.betaltBeløp,
                    skyldnerInntektListe = grunnlagsdata?.skyldnerInntektListe ?: emptyList(),
                    mottakerInntektListe = grunnlagsdata?.mottakerInntektListe ?: emptyList(),
                    kravhaverInntektListe = grunnlagsdata?.kravhaverInntektListe ?: emptyList(),
                )
                hendelserService.opprettSærbidragshendelse(særbidragshendelse)
            }
    }

    fun hentVedtak(vedtaksid: Int): VedtakDto? {
        val vedtak = bidragVedtakConsumer.hentVedtak(vedtaksid) ?: return null
        if ((vedtak.erDelvedtak || vedtak.erOrkestrertVedtak) && vedtak.type == Vedtakstype.INNKREVING) return null
        val faktiskVedtak =
            if (vedtak.erOrkestrertVedtak) {
                bidragVedtakConsumer.hentVedtak(vedtak.referertVedtaksid!!)
            } else {
                vedtak
            } ?: return null
        /*        if (faktiskVedtak.grunnlagListe.isEmpty()) {
                    LOGGER.info("Vedtak $vedtaksid fattet av system ${vedtak.kildeapplikasjon} mangler grunnlag")
                    SECURE_LOGGER.info("Vedtak fattet av system ${vedtak.kildeapplikasjon} mangler grunnlag. Vedtak: $vedtak")

                    return null
                }*/
        return faktiskVedtak
    }

    private fun finnGrunnlagsdataForskudd(
        grunnlagListe: List<GrunnlagDto>,
        grunnlagsreferanseListePeriode: List<Grunnlagsreferanse>,
        kravhaver: String,
    ): GrunnlagsdataForskudd? {
        // Sjekker først om perioden har grunnlag, hvis ikke returneres null
        if (grunnlagListe.isEmpty()) {
            return null
        }

        val referanseBM = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
        val søknadsbarnReferanse = finnReferanseTilIdent(grunnlagListe, kravhaver)

        // Finn grunnlagsdata
        val respons = GrunnlagsdataForskudd(
            barnetsAldersgruppe = grunnlagListe.finnBarnetsAldersgruppeForPeriode(grunnlagsreferanseListePeriode),
            antallBarnIEgenHusstand = grunnlagListe.finnAntallBarnIEgenHusstandForPeriode(grunnlagsreferanseListePeriode),
            sivilstand = grunnlagListe.finnSivilstandForPeriode(grunnlagsreferanseListePeriode),
            barnBorMedMottaker = grunnlagListe.finnOmbarnBorMedMottakerIPeriode(grunnlagsreferanseListePeriode),
            mottakerInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, referanseBM, grunnlagListe),
            kravhaverInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, søknadsbarnReferanse, grunnlagListe),
        )

        return respons
    }

    private fun finnGrunnlagsdataBidragsperiode(
        vedtakErAldersjustering: Boolean,
        vedtakFraBisys: Boolean,
        grunnlagListe: List<GrunnlagDto>,
        grunnlagsreferanseListePeriode: List<Grunnlagsreferanse>,
        kravhaver: String,
    ): GrunnlagsdataBidrag? {
        // Sjekker først om perioden har grunnlag, hvis ikke returneres null
        if (grunnlagListe.isEmpty()) {
            return null
        }

        // Finn grunnlagsdata. For vedtak fra Bisys returneres ikke grunnlagsdata enn så lenge pga dårlig datakvalitet i grunnlagsoverføring
        val respons = if (vedtakFraBisys) {
            GrunnlagsdataBidrag(
                bidragsevne = null,
                underholdskostnad = null,
                skyldnersAndelUnderholdskostnad = null,
                nettoTilsynsutgift = null,
                faktiskUtgift = null,
                samværsfradrag = null,
                nettoBarnetilleggSkyldner = null,
                nettoBarnetilleggMottaker = null,
                skyldnerBorMedAndreVoksne = null,
                samværsklasse = null,
                skyldnerInntektListe = null,
                mottakerInntektListe = null,
                kravhaverInntektListe = null,
            )
        } else {
            val referanseBP = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            val referanseBM = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            val søknadsbarnReferanse = finnReferanseTilIdent(grunnlagListe, kravhaver)

            GrunnlagsdataBidrag(
                bidragsevne = grunnlagListe.finnBidragevneForPeriode(grunnlagsreferanseListePeriode),
                underholdskostnad = grunnlagListe.finnUnderholdskostnadForPeriode(grunnlagsreferanseListePeriode),
                skyldnersAndelUnderholdskostnad = grunnlagListe.finnSkyldnersAndelUnderholdskostnadForPeriode(
                    vedtakErAldersjustering,
                    vedtakFraBisys,
                    grunnlagsreferanseListePeriode,
                ),
                nettoTilsynsutgift = grunnlagListe.finnNettoTilsynsutgiftForPeriode(grunnlagsreferanseListePeriode),
                faktiskUtgift = grunnlagListe.finnFaktiskUtgiftForPeriode(grunnlagsreferanseListePeriode),
                samværsfradrag = grunnlagListe.finnSamværsfradragForPeriode(grunnlagsreferanseListePeriode),
                nettoBarnetilleggSkyldner = grunnlagListe.finnNettoBarnetilleggForPeriode(grunnlagsreferanseListePeriode, referanseBP),
                nettoBarnetilleggMottaker = grunnlagListe.finnNettoBarnetilleggForPeriode(grunnlagsreferanseListePeriode, referanseBM),
                skyldnerBorMedAndreVoksne = grunnlagListe.finnSkyldnerBorMedAndreVoksneIPeriode(grunnlagsreferanseListePeriode),
                samværsklasse = grunnlagListe.finnSamværsklasseIPeriode(vedtakErAldersjustering, vedtakFraBisys, grunnlagsreferanseListePeriode),
                skyldnerInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, referanseBP, grunnlagListe),
                mottakerInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, referanseBM, grunnlagListe),
                kravhaverInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, søknadsbarnReferanse, grunnlagListe),
            )
        }

        return respons
    }

    private fun finnGrunnlagsdataSærbidrag(
        vedtakFraBisys: Boolean,
        grunnlagListe: List<GrunnlagDto>,
        grunnlagsreferanseListePeriode: List<Grunnlagsreferanse>,
        kravhaver: String,
    ): GrunnlagsdataSærbidrag? {
        // Sjekker først om perioden har grunnlag, hvis ikke returneres null
        if (grunnlagListe.isEmpty()) {
            return null
        }

        // Finn grunnlagsdata. For vedtak fra Bisys returneres ikke grunnlagsdata enn så lenge pga dårlig datakvalitet i grunnlagsoverføring
        val respons = if (vedtakFraBisys) {
            GrunnlagsdataSærbidrag(
                kategori = null,
                kravbeløp = null,
                godkjentBeløp = null,
                skyldnerInntektListe = null,
                mottakerInntektListe = null,
                kravhaverInntektListe = null,
            )
        } else {
            val referanseBP = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            val referanseBM = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            val søknadsbarnReferanse = finnReferanseTilIdent(grunnlagListe, kravhaver)

            GrunnlagsdataSærbidrag(
                kategori = grunnlagListe.særbidragskategori?.kategori,
                kravbeløp = grunnlagListe.utgiftsposter.sumOf { it.kravbeløp },
                godkjentBeløp = grunnlagListe.utgiftsposter.sumOf { it.godkjentBeløp },
                skyldnerInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, referanseBP, grunnlagListe),
                mottakerInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, referanseBM, grunnlagListe),
                kravhaverInntektListe = grunnlagListe.finnInntekterRolle(grunnlagsreferanseListePeriode, søknadsbarnReferanse, grunnlagListe),
            )
        }

        return respons
    }

    fun List<GrunnlagDto>.finnBarnetsAldersgruppeForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        return sluttberegning.innholdTilObjekt<SluttberegningForskudd>().aldersgruppe.name
    }

    fun List<GrunnlagDto>.finnAntallBarnIEgenHusstandForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Double {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return 0.0
        val antallBarnIEgenHusstandPeriode = finnOgKonverterGrunnlagSomErReferertAv<DelberegningBarnIHusstand>(
            Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND,
            sluttberegning,
        ).firstOrNull()
        return antallBarnIEgenHusstandPeriode?.innhold?.antallBarn ?: 0.0
    }

    fun List<GrunnlagDto>.finnSivilstandForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): String? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val sivilstandPeriode = finnOgKonverterGrunnlagSomErReferertAv<SivilstandPeriode>(
            Grunnlagstype.SIVILSTAND_PERIODE,
            sluttberegning,
        ).firstOrNull()
        return sivilstandPeriode?.innhold?.sivilstand?.name
    }

    fun List<GrunnlagDto>.finnOmbarnBorMedMottakerIPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val bostatusPeriode = finnOgKonverterGrunnlagSomErReferertAv<BostatusPeriode>(
            Grunnlagstype.BOSTATUS_PERIODE,
            sluttberegning,
        ).firstOrNull()
        return bostatusPeriode?.innhold?.bostatus == Bostatuskode.MED_FORELDER
    }

    fun List<GrunnlagDto>.finnInntekterForskudd(grunnlagsreferanseListe: List<Grunnlagsreferanse>): List<Inntekt>? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val inntekter = finnOgKonverterGrunnlagSomErReferertAv<InntektsrapporteringPeriode>(
            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            sluttberegning,
        )
        return inntekter.map { inntekt ->
            Inntekt(
                type = inntekt.innhold.inntektsrapportering.name,
                beløp = inntekt.innhold.beløp,
            )
        }
    }

    fun List<GrunnlagDto>.finnBidragevneForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val bidragevne = finnOgKonverterGrunnlagSomErReferertAv<DelberegningBidragsevne>(
            Grunnlagstype.DELBEREGNING_BIDRAGSEVNE,
            sluttberegning,
        ).firstOrNull {
            it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                (
                    sluttberegning.gjelderBarnReferanse != null &&
                        it.referanse.contains(
                            sluttberegning.gjelderBarnReferanse!!,
                        )
                    )
        }
        return bidragevne?.innhold?.beløp
    }

    fun List<GrunnlagDto>.finnUnderholdskostnadForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val underholdskostand = finnOgKonverterGrunnlagSomErReferertAv<DelberegningUnderholdskostnad>(
            Grunnlagstype.DELBEREGNING_UNDERHOLDSKOSTNAD,
            sluttberegning,
        ).firstOrNull {
            it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                (sluttberegning.gjelderBarnReferanse != null && it.referanse.contains(sluttberegning.gjelderBarnReferanse!!))
        }
        return underholdskostand?.innhold?.underholdskostnad
    }

    fun List<GrunnlagDto>.finnSkyldnersAndelUnderholdskostnadForPeriode(
        vedtakErAldersjustering: Boolean,
        vedtakFraBisys: Boolean,
        grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    ): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        if (vedtakErAldersjustering && !vedtakFraBisys) {
            return sluttberegning.innholdTilObjekt<SluttberegningBarnebidragAldersjustering>().bpAndelBeløp
        } else {
            val bPsAndelUnderholdskostand = finnOgKonverterGrunnlagSomErReferertAv<DelberegningBidragspliktigesAndel>(
                Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL,
                sluttberegning,
            ).firstOrNull {
                it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                    (sluttberegning.gjelderBarnReferanse != null && it.referanse.contains(sluttberegning.gjelderBarnReferanse!!))
            }
            return bPsAndelUnderholdskostand?.innhold?.andelBeløp
        }
    }

    fun List<GrunnlagDto>.finnNettoTilsynsutgiftForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val nettoTilsynsutgift = finnOgKonverterGrunnlagSomErReferertAv<DelberegningNettoTilsynsutgift>(
            Grunnlagstype.DELBEREGNING_NETTO_TILSYNSUTGIFT,
            sluttberegning,
        ).firstOrNull {
            it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                (
                    sluttberegning.gjelderBarnReferanse != null &&
                        it.referanse.contains(
                            sluttberegning.gjelderBarnReferanse!!,
                        )
                    )
        }
        return nettoTilsynsutgift?.innhold?.nettoTilsynsutgift
    }

    fun List<GrunnlagDto>.finnFaktiskUtgiftForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val faktiskUtgift = finnOgKonverterGrunnlagSomErReferertAv<DelberegningFaktiskTilsynsutgift>(
            Grunnlagstype.DELBEREGNING_FAKTISK_UTGIFT,
            sluttberegning,
        ).firstOrNull {
            it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                (
                    sluttberegning.gjelderBarnReferanse != null &&
                        it.referanse.contains(
                            sluttberegning.gjelderBarnReferanse!!,
                        )
                    )
        }
        return faktiskUtgift?.innhold?.beregnetBeløp
    }

    fun List<GrunnlagDto>.finnSamværsfradragForPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null

        val samværsfradrag = finnOgKonverterGrunnlagSomErReferertAv<DelberegningSamværsfradrag>(
            Grunnlagstype.DELBEREGNING_SAMVÆRSFRADRAG,
            sluttberegning,
        ).firstOrNull {
            it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                (
                    sluttberegning.gjelderBarnReferanse != null &&
                        it.referanse.contains(
                            sluttberegning.gjelderBarnReferanse!!,
                        )
                    )
        }
        return samværsfradrag?.innhold?.beløp
    }

    fun List<GrunnlagDto>.finnNettoBarnetilleggForPeriode(
        grunnlagsreferanseListe: List<Grunnlagsreferanse>,
        referanseTilRolle: String?,
    ): BigDecimal? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val nettoBarnetillegg = finnOgKonverterGrunnlagSomErReferertAv<DelberegningNettoBarnetillegg>(
            Grunnlagstype.DELBEREGNING_NETTO_BARNETILLEGG,
            sluttberegning,
        ).firstOrNull {
            it.gjelderReferanse == referanseTilRolle &&
                (
                    it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse ||
                        (sluttberegning.gjelderBarnReferanse != null && it.referanse.contains(sluttberegning.gjelderBarnReferanse!!))

                    )
        }
        return nettoBarnetillegg?.innhold?.summertNettoBarnetillegg
    }

    fun List<GrunnlagDto>.finnSkyldnerBorMedAndreVoksneIPeriode(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val bostatusPeriode = finnOgKonverterGrunnlagSomErReferertAv<DelberegningVoksneIHustand>(
            Grunnlagstype.DELBEREGNING_VOKSNE_I_HUSSTAND,
            sluttberegning,
        ).firstOrNull()
        return bostatusPeriode?.innhold?.borMedAndreVoksne
    }

    fun List<GrunnlagDto>.finnSamværsklasseIPeriode(
        vedtakErAldersjustering: Boolean,
        vedtakFraBisys: Boolean,
        grunnlagsreferanseListe: List<Grunnlagsreferanse>,
    ): Samværsklasse? {
        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        if (vedtakErAldersjustering && !vedtakFraBisys) {
            val samværsperiode = finnOgKonverterGrunnlagSomErReferertAv<KopiSamværsperiodeGrunnlag>(
                Grunnlagstype.KOPI_SAMVÆRSPERIODE,
                sluttberegning,
            ).firstOrNull { it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse }
            return samværsperiode?.innhold?.samværsklasse
        } else {
            val samværsperiode = finnOgKonverterGrunnlagSomErReferertAv<SamværsperiodeGrunnlag>(
                Grunnlagstype.SAMVÆRSPERIODE,
                sluttberegning,
            ).firstOrNull { it.gjelderBarnReferanse == sluttberegning.gjelderBarnReferanse }
            return samværsperiode?.innhold?.samværsklasse
        }
    }

    fun List<GrunnlagDto>.finnInntekterRolle(
        grunnlagsreferanseListe: List<Grunnlagsreferanse>,
        referanseTilRolle: String?,
        grunnlagListe: List<GrunnlagDto>,
    ): List<Inntekt>? {
        val søknadsbarnReferanse = finnReferanseTilRolle(grunnlagListe, Grunnlagstype.PERSON_SØKNADSBARN)

        val sluttberegning = finnSluttberegningIReferanser(grunnlagsreferanseListe) ?: return null
        val inntekter = finnOgKonverterGrunnlagSomErReferertAv<InntektsrapporteringPeriode>(
            Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            sluttberegning,
        ).filter { it.innhold.valgt }
            .filter { it.gjelderReferanse == referanseTilRolle && (it.innhold.gjelderBarn == null || it.innhold.gjelderBarn == søknadsbarnReferanse) }
        return inntekter.map { inntekt ->
            Inntekt(
                type = inntekt.innhold.inntektsrapportering.name,
                beløp = inntekt.innhold.beløp,
                inntektstype = inntekt.innhold.inntektspostListe.firstOrNull()?.inntektstype?.name,
                gjelderKravhaver = finnIdentTilReferanse(grunnlagListe, inntekt.innhold.gjelderBarn),
            )
        }
    }

    fun finnReferanseTilRolle(grunnlagListe: List<GrunnlagDto>, grunnlagstype: Grunnlagstype) = grunnlagListe
        .firstOrNull { it.type == grunnlagstype }?.referanse

    fun finnIdentTilReferanse(grunnlagListe: List<GrunnlagDto>, referanse: String?) = grunnlagListe.hentPersonMedReferanseKonvertert(referanse)?.ident?.verdi

    fun finnReferanseTilIdent(grunnlagListe: List<GrunnlagDto>, ident: String) = grunnlagListe.hentPersonMedIdent(ident)?.referanse

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatistikkService::class.java)
    }
}

data class GrunnlagsdataForskudd(
    val barnetsAldersgruppe: String?,
    val antallBarnIEgenHusstand: Double?,
    val sivilstand: String?,
    val barnBorMedMottaker: Boolean?,
    val mottakerInntektListe: List<Inntekt>?,
    val kravhaverInntektListe: List<Inntekt>?,
)

data class GrunnlagsdataBidrag(
    val bidragsevne: BigDecimal?,
    val underholdskostnad: BigDecimal?,
    val skyldnersAndelUnderholdskostnad: BigDecimal?,
    val nettoTilsynsutgift: BigDecimal?,
    val faktiskUtgift: BigDecimal?,
    val samværsfradrag: BigDecimal?,
    val nettoBarnetilleggSkyldner: BigDecimal?,
    val nettoBarnetilleggMottaker: BigDecimal?,
    val skyldnerBorMedAndreVoksne: Boolean?,
    val samværsklasse: Samværsklasse?,
    val skyldnerInntektListe: List<Inntekt>?,
    val mottakerInntektListe: List<Inntekt>?,
    val kravhaverInntektListe: List<Inntekt>?,
)

data class GrunnlagsdataSærbidrag(
    val kategori: Særbidragskategori?, // "type": "SÆRBIDRAG_KATEGORI",
    val kravbeløp: BigDecimal?, // "type": "UTGIFTSPOSTER", kravbeløp
    val godkjentBeløp: BigDecimal?, // "type": "UTGIFTSPOSTER", godkjentBeløp
    val skyldnerInntektListe: List<Inntekt>?,
    val mottakerInntektListe: List<Inntekt>?,
    val kravhaverInntektListe: List<Inntekt>?,
)
