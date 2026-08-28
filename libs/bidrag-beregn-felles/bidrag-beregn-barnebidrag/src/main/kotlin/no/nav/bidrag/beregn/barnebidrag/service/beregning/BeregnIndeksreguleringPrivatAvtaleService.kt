package no.nav.bidrag.beregn.barnebidrag.service.beregning

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.barnebidrag.bo.IndeksreguleringPrivatAvtaleGrunnlag
import no.nav.bidrag.beregn.barnebidrag.mapper.BidragsevneMapper.finnReferanseTilRolle
import no.nav.bidrag.beregn.barnebidrag.mapper.BidragsevneMapper.mapSjablonSjablontall
import no.nav.bidrag.beregn.core.bo.SjablonSjablontallBeregningGrunnlag
import no.nav.bidrag.beregn.core.bo.SjablonSjablontallPeriodeGrunnlag
import no.nav.bidrag.beregn.core.service.BeregnService
import no.nav.bidrag.commons.service.sjablon.SjablonProvider
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.avrundetMedNullDesimaler
import no.nav.bidrag.domene.util.avrundetTilNærmesteTier
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningIndeksreguleringPrivatAvtale
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningPrivatAvtale
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningPrivatAvtalePeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtaleGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtaleGrunnlagV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtalePeriodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.opprettDelberegningreferanse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal object BeregnIndeksreguleringPrivatAvtaleService : BeregnService() {

    fun delberegningIndeksreguleringPrivatAvtale(grunnlag: BeregnGrunnlag, beregningsperiode: ÅrMånedsperiode? = null): List<GrunnlagDto> {
        val referanseTilBP = finnReferanseTilRolle(
            grunnlagListe = grunnlag.grunnlagListe,
            grunnlagstype = Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
        )

        val privatAvtale = grunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlag>(Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG)
            .filter { it.gjelderBarnReferanse == grunnlag.søknadsbarnReferanse }
            .map {
                PrivatAvtale(
                    referanse = it.referanse,
                    avtaleInngåttDato = it.innhold.avtaleInngåttDato,
                    skalIndeksreguleres = it.innhold.skalIndeksreguleres,
                )
            }.firstOrNull()

        val privatAvtalePeriodeListe = grunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtalePeriodeGrunnlag>(
                grunnlagType = Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG,
            )
            .filter { it.gjelderBarnReferanse == grunnlag.søknadsbarnReferanse }
            .map {
                PrivatAvtalePeriode(
                    referanse = it.referanse,
                    periode = it.innhold.periode,
                    beløp = it.innhold.beløp,
                    valutakode = Valutakode.NOK,
                )
            }.sortedBy { it.periode.fom }

        val beregnTil = grunnlag.periode.til
        val beregnFra = privatAvtalePeriodeListe.first().periode.fom
        val periode = ÅrMånedsperiode(
            fom = beregnFra,
            til = if (beregnTil != null && beregnTil > beregnFra) beregnTil else beregnFra.plusMonths(1),
        )

        val sjablonListe = mapSjablonSjablontallGrunnlag(
            periode = periode,
            sjablonListe = SjablonProvider.hentSjablontall(),
        ) { it.indeksregulering }

        val sjablonIndeksreguleringFaktorListe = mapSjablonSjablontall(sjablonListe)

        // Kast exception om privatAvtalePeriodeListe er tom
        if (privatAvtale == null || privatAvtalePeriodeListe.isEmpty()) {
            throw IllegalArgumentException("Ingen privat avtale eller perioder funnet")
        }

        // Lager liste over bruddperioder
        val (indeksregulerPeriode, beregningsperiodeListe) = lagBruddperiodeListe(
            privatAvtale = privatAvtale,
            privatAvtalePeriodeListe = privatAvtalePeriodeListe,
            beregningsperiode = periode,
        )

        val resultatliste = mutableListOf<DelberegningPrivatAvtalePeriode>()

        var beløpFraForrigeDelberegning: BigDecimal? = null

        beregningsperiodeListe.forEach {
            val grunnlagBeregning = lagIndeksreguleringBeregningGrunnlag(
                beregningsperiode = it.periode,
                periodeSkalIndeksreguleres = it.periodeSkalIndeksreguleres,
                referanseTilRolle = referanseTilBP,
                søknadsbarnReferanse = grunnlag.søknadsbarnReferanse,
                privatAvtale = privatAvtale,
                privatAvtalePeriodeListe = privatAvtalePeriodeListe,
                sjablonIndeksreguleringFaktorListe = sjablonIndeksreguleringFaktorListe,
                beløpFraForrigeDelberegning = beløpFraForrigeDelberegning,
            )

            val resultat = beregn(grunnlagBeregning)

            if (it.periodeSkalIndeksreguleres) {
                beløpFraForrigeDelberegning = resultat.beløp
            }

            resultatliste.add(resultat)
        }

        val referanseFom = beregningsperiode?.fom ?: grunnlag.periode.fom

        val delberegningPrivatAvtaleGrunnlag = listOf(
            GrunnlagDto(
                type = Grunnlagstype.DELBEREGNING_PRIVAT_AVTALE,
                referanse = opprettDelberegningreferanse(
                    type = Grunnlagstype.DELBEREGNING_PRIVAT_AVTALE,
                    periode = ÅrMånedsperiode(referanseFom, null),
                    søknadsbarnReferanse = grunnlag.søknadsbarnReferanse,
                    gjelderReferanse = referanseTilBP,
                ),
                innhold = POJONode(
                    DelberegningPrivatAvtale(
                        nesteIndeksreguleringsår = indeksregulerPeriode.year.toBigDecimal(),
                        perioder = resultatliste,
                    ),
                ),
                gjelderReferanse = referanseTilBP,
                gjelderBarnReferanse = grunnlag.søknadsbarnReferanse,
                grunnlagsreferanseListe = listOf(privatAvtale.referanse) + privatAvtalePeriodeListe.map { it.referanse },

            ),
        )

        // Mapper ut grunnlag som er brukt i beregningen (mottatte grunnlag og sjabloner)
        val resultatGrunnlagListe = mapDelberegningResultatGrunnlag(
            grunnlagReferanseListe =
            delberegningPrivatAvtaleGrunnlag.map { it.referanse } +
                privatAvtalePeriodeListe.map { it.referanse } +
                privatAvtale.referanse,
            mottattGrunnlag = grunnlag,
            sjablonGrunnlag = sjablonListe,
        ).toMutableList()

        // Mapper ut grunnlag for Person-objekter som er brukt
        resultatGrunnlagListe.addAll(
            mapPersonobjektGrunnlag(
                resultatGrunnlagListe = resultatGrunnlagListe + delberegningPrivatAvtaleGrunnlag,
                personobjektGrunnlagListe = grunnlag.grunnlagListe,
            ),
        )

        return (delberegningPrivatAvtaleGrunnlag + resultatGrunnlagListe)
            .distinctBy { it.referanse }.sortedBy { it.referanse }
    }

    private fun beregn(grunnlag: IndeksreguleringPrivatAvtaleGrunnlag): DelberegningPrivatAvtalePeriode {
        val delberegningResultat: DelberegningPrivatAvtalePeriode = if (grunnlag.periodeSkalIndeksreguleres) {
            val indeksreguleringFaktor = BigDecimal.valueOf(grunnlag.sjablonIndeksreguleringFaktor!!.verdi).divide(BigDecimal(100)).setScale(4)
            val beløp = grunnlag.beløpFraForrigeDelberegning ?: grunnlag.privatAvtalePeriode.beløp
            val indeksregulertBeløp = beløp.plus(beløp.multiply(indeksreguleringFaktor)).avrundetTilNærmesteTier
            DelberegningPrivatAvtalePeriode(
                periode = grunnlag.beregningsperiode,
                indeksreguleringFaktor = indeksreguleringFaktor,
                beløp = indeksregulertBeløp,
            )
        } else {
            DelberegningPrivatAvtalePeriode(
                periode = grunnlag.beregningsperiode,
                indeksreguleringFaktor = null,
                beløp = grunnlag.privatAvtalePeriode.beløp.avrundetMedNullDesimaler,

            )
        }
        return delberegningResultat
    }

    fun delberegningIndeksreguleringPrivatAvtaleV2(grunnlag: BeregnGrunnlag, beregningsperiode: ÅrMånedsperiode? = null): List<GrunnlagDto> {
        val referanseTilBP = finnReferanseTilRolle(
            grunnlagListe = grunnlag.grunnlagListe,
            grunnlagstype = Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
        )

        val privatAvtale = grunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG)
            .filter { it.gjelderBarnReferanse == grunnlag.søknadsbarnReferanse }
            .map {
                PrivatAvtale(
                    referanse = it.referanse,
                    avtaleInngåttDato = it.innhold.avtaleInngåttDato,
                    skalIndeksreguleres = it.innhold.skalIndeksreguleres,
                )
            }.firstOrNull()

        val privatAvtalePeriodeListe = grunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtalePeriodeGrunnlag>(
                grunnlagType = Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG,
            )
            .filter { it.gjelderBarnReferanse == grunnlag.søknadsbarnReferanse }
            .map {
                PrivatAvtalePeriode(
                    referanse = it.referanse,
                    periode = it.innhold.periode,
                    beløp = it.innhold.beløp,
                    valutakode = it.innhold.valutakode ?: Valutakode.NOK,
                )
            }.sortedBy { it.periode.fom }

        val beregnTil = if (beregningsperiode != null) beregningsperiode.til else grunnlag.periode.til
        val beregnFra = privatAvtalePeriodeListe.first().periode.fom
        val periode = ÅrMånedsperiode(
            fom = beregnFra,
            til = if (beregnTil != null && beregnTil > beregnFra) beregnTil else beregnFra.plusMonths(1),
        )

        val sjablonListe = mapSjablonSjablontallGrunnlag(
            periode = periode,
            sjablonListe = SjablonProvider.hentSjablontall(),
        ) { it.indeksregulering }

        val sjablonIndeksreguleringFaktorListe = mapSjablonSjablontall(sjablonListe)

        // Kast exception om privatAvtalePeriodeListe er tom
        if (privatAvtale == null || privatAvtalePeriodeListe.isEmpty()) {
            throw IllegalArgumentException("Ingen privat avtale eller perioder funnet")
        }

        // Lager liste over bruddperioder
        val beregningsperiodeListe = lagBruddperiodeListeV2(
            privatAvtale = privatAvtale,
            privatAvtalePeriodeListe = privatAvtalePeriodeListe,
            beregningsperiode = periode,
        )

        val resultatliste = mutableListOf<DelberegningIndeksreguleringPrivatAvtale>()

        var beløpFraForrigeDelberegning: BigDecimal? = null

        beregningsperiodeListe.forEach { periode ->
            val grunnlagBeregning = lagIndeksreguleringBeregningGrunnlag(
                beregningsperiode = periode.periode,
                periodeSkalIndeksreguleres = periode.periodeSkalIndeksreguleres,
                nesteIndeksreguleringsår = periode.nesteIndeksreguleringsår,
                referanseTilRolle = referanseTilBP,
                søknadsbarnReferanse = grunnlag.søknadsbarnReferanse,
                privatAvtale = privatAvtale,
                privatAvtalePeriodeListe = privatAvtalePeriodeListe,
                sjablonIndeksreguleringFaktorListe = sjablonIndeksreguleringFaktorListe,
                beløpFraForrigeDelberegning = beløpFraForrigeDelberegning,
            )

            var resultat: DelberegningIndeksreguleringPrivatAvtale

            if (grunnlagBeregning.periodeSkalIndeksreguleres) {
                val indeksreguleringFaktor = BigDecimal.valueOf(
                    grunnlagBeregning.sjablonIndeksreguleringFaktor!!.verdi,
                ).divide(BigDecimal(100)).setScale(4)
                val beløp = grunnlagBeregning.beløpFraForrigeDelberegning ?: grunnlagBeregning.privatAvtalePeriode.beløp
                val indeksregulertBeløp = beløp.plus(beløp.multiply(indeksreguleringFaktor)).avrundetTilNærmesteTier
                resultat =
                    DelberegningIndeksreguleringPrivatAvtale(
                        periode = grunnlagBeregning.beregningsperiode,
                        nesteIndeksreguleringsår = grunnlagBeregning.nesteIndeksreguleringsår?.toBigDecimal(),
                        indeksreguleringFaktor = indeksreguleringFaktor,
                        valutakode = grunnlagBeregning.privatAvtalePeriode.valutakode,
                        indeksregulertBeløp = indeksregulertBeløp,
                    )
                beløpFraForrigeDelberegning = indeksregulertBeløp
            } else {
                resultat =
                    DelberegningIndeksreguleringPrivatAvtale(
                        periode = grunnlagBeregning.beregningsperiode,
                        nesteIndeksreguleringsår = grunnlagBeregning.nesteIndeksreguleringsår?.toBigDecimal(),
                        indeksreguleringFaktor = null,
                        valutakode = grunnlagBeregning.privatAvtalePeriode.valutakode,
                        indeksregulertBeløp = grunnlagBeregning.privatAvtalePeriode.beløp.avrundetMedNullDesimaler,
                    )
            }

            resultatliste.add(resultat)
        }

        val delberegninger = mutableListOf<GrunnlagDto>()
        var forrigeReferanse: String? = null

        resultatliste.forEach { resultatperiode ->
            val referanse = opprettDelberegningreferanse(
                type = Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE,
                periode = ÅrMånedsperiode(resultatperiode.periode.fom, resultatperiode.periode.til),
                søknadsbarnReferanse = grunnlag.søknadsbarnReferanse,
                gjelderReferanse = referanseTilBP,
            )

            delberegninger.add(
                GrunnlagDto(
                    type = Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE,
                    referanse = referanse,
                    innhold = POJONode(
                        DelberegningIndeksreguleringPrivatAvtale(
                            periode = resultatperiode.periode,
                            nesteIndeksreguleringsår = resultatperiode.nesteIndeksreguleringsår,
                            indeksreguleringFaktor = resultatperiode.indeksreguleringFaktor,
                            valutakode = resultatperiode.valutakode,
                            indeksregulertBeløp = resultatperiode.indeksregulertBeløp,
                        ),
                    ),
                    gjelderReferanse = referanseTilBP,
                    gjelderBarnReferanse = grunnlag.søknadsbarnReferanse,
                    grunnlagsreferanseListe = listOfNotNull(
                        privatAvtale.referanse,
                        resultatperiode.periode.let { periode ->
                            privatAvtalePeriodeListe.first {
                                ÅrMånedsperiode(it.periode.fom, it.periode.til).inneholder(periode)
                            }.referanse
                        },
                        forrigeReferanse,
                    ),
                ),
            )
            forrigeReferanse = referanse
        }

        // Mapper ut grunnlag som er brukt i beregningen (mottatte grunnlag og sjabloner)
        val resultatGrunnlagListe = mapDelberegningResultatGrunnlag(
            grunnlagReferanseListe =
            delberegninger.map { it.referanse } +
                privatAvtalePeriodeListe.map { it.referanse } +
                privatAvtale.referanse,
            mottattGrunnlag = grunnlag,
            sjablonGrunnlag = sjablonListe,
        ).toMutableList()

        // Mapper ut grunnlag for Person-objekter som er brukt
        resultatGrunnlagListe.addAll(
            mapPersonobjektGrunnlag(
                resultatGrunnlagListe = resultatGrunnlagListe + delberegninger,
                personobjektGrunnlagListe = grunnlag.grunnlagListe,
            ),
        )

        return (delberegninger + resultatGrunnlagListe)
            .distinctBy { it.referanse }.sortedBy { it.referanse }
    }

    // Lager en liste over alle bruddperioder med indikator for indeksregulering
    private fun lagBruddperiodeListe(
        privatAvtale: PrivatAvtale,
        privatAvtalePeriodeListe: List<PrivatAvtalePeriode>,
        beregningsperiode: ÅrMånedsperiode,
    ): Pair<YearMonth, List<Beregningsperiode>> {
        var beregningsperiodeListe = mutableListOf<Beregningsperiode>()

        val beregnTil = beregningsperiode.til ?: YearMonth.now()
        var indeksregulerPeriode = maxOf(
            YearMonth.from(privatAvtale.avtaleInngåttDato),
            privatAvtalePeriodeListe.last().periode.fom,
        ).plusYears(1).withMonth(7)

        if (privatAvtale.skalIndeksreguleres && indeksregulerPeriode <= beregnTil && privatAvtalePeriodeListe.last().periode.til == null) {
            privatAvtalePeriodeListe.forEach {
                if (it.periode.fom.isBefore(indeksregulerPeriode)) {
                    beregningsperiodeListe.add(
                        Beregningsperiode(
                            periode = ÅrMånedsperiode(it.periode.fom, it.periode.fom),
                            periodeSkalIndeksreguleres = false,
                        ),
                    )
                }
            }

            while (indeksregulerPeriode < beregnTil) {
                if (indeksregulerPeriode < beregnTil || beregningsperiode.til == null) {
                    beregningsperiodeListe.add(
                        Beregningsperiode(
                            periode = ÅrMånedsperiode(indeksregulerPeriode, indeksregulerPeriode),
                            periodeSkalIndeksreguleres = true,
                        ),
                    )
                    indeksregulerPeriode = indeksregulerPeriode.plusYears(1)
                }
            }

            val periodeListe = beregningsperiodeListe.asSequence().map { it.periode }

            // Slår sammen og lager periodene som skal beregnes.
            val sammenslåttePerioder = lagBruddPeriodeListe(periodeListe, beregningsperiode).ifEmpty {
                listOf(ÅrMånedsperiode(beregningsperiodeListe.firstOrNull()?.periode?.fom ?: privatAvtalePeriodeListe.last().periode.fom, null))
            }
            // Til slutt legges det til en periode med åpen tildato
            val endeligListe = sammenslåttePerioder.last().til?.let { sammenslåttePerioder.plus(ÅrMånedsperiode(it, null)) } ?: sammenslåttePerioder

            beregningsperiodeListe = endeligListe.map {
                Beregningsperiode(
                    periode = it,
                    periodeSkalIndeksreguleres = beregningsperiodeListe.first { periode -> periode.periode.fom == it.fom }.periodeSkalIndeksreguleres,
                )
            }.toMutableList()
        } else {
            beregningsperiodeListe =
                privatAvtalePeriodeListe.asSequence().map { Beregningsperiode(periode = it.periode, periodeSkalIndeksreguleres = false) }.toList()
                    .toMutableList()
        }
        return indeksregulerPeriode to beregningsperiodeListe.sortedBy { it.periode.fom }
    }

    // Lager en liste over alle bruddperioder med indeksår og indikator for indeksregulering
    private fun lagBruddperiodeListeV2(
        privatAvtale: PrivatAvtale,
        privatAvtalePeriodeListe: List<PrivatAvtalePeriode>,
        beregningsperiode: ÅrMånedsperiode,
    ): List<BeregningsperiodeV2> {
        var beregningsperiodeListe = mutableListOf<BeregningsperiodeV2>()

        val beregnTil = beregningsperiode.til ?: YearMonth.now()
        var indeksregulerPeriode = maxOf(
            YearMonth.from(privatAvtale.avtaleInngåttDato),
            privatAvtalePeriodeListe.last().periode.fom,
        ).plusYears(1).withMonth(7)

        if (privatAvtale.skalIndeksreguleres && indeksregulerPeriode <= beregnTil && privatAvtalePeriodeListe.last().periode.til == null) {
            privatAvtalePeriodeListe.forEach {
                if (it.periode.fom.isBefore(indeksregulerPeriode)) {
                    beregningsperiodeListe.add(
                        BeregningsperiodeV2(
                            periode = ÅrMånedsperiode(it.periode.fom, it.periode.fom),
                            periodeSkalIndeksreguleres = false,
                            nesteIndeksreguleringsår = if (it.periode.til == null) indeksregulerPeriode.year else null,
                        ),
                    )
                }
            }

            // Lager nye perioder mellom siste ordinære periode og beregnTil, og legger til indeksreguleringsår for hver periode
            while (indeksregulerPeriode < beregnTil) {
                if (indeksregulerPeriode < beregnTil || beregningsperiode.til == null) {
                    beregningsperiodeListe.add(
                        BeregningsperiodeV2(
                            periode = ÅrMånedsperiode(indeksregulerPeriode, indeksregulerPeriode),
                            periodeSkalIndeksreguleres = true,
                            nesteIndeksreguleringsår = indeksregulerPeriode.plusYears(1).year,
                        ),
                    )
                    indeksregulerPeriode = indeksregulerPeriode.plusYears(1)
                }
            }

            val periodeListe = beregningsperiodeListe.asSequence().map { it.periode }

            // Slår sammen og lager periodene som skal beregnes.
            val sammenslåttePerioder = lagBruddPeriodeListe(periodeListe, beregningsperiode).ifEmpty {
                listOf(ÅrMånedsperiode(beregningsperiodeListe.firstOrNull()?.periode?.fom ?: privatAvtalePeriodeListe.last().periode.fom, null))
            }
            // Til slutt legges det til en periode med åpen tildato
            val endeligListe = sammenslåttePerioder.last().til?.let { sammenslåttePerioder.plus(ÅrMånedsperiode(it, null)) } ?: sammenslåttePerioder

            beregningsperiodeListe = endeligListe.map {
                BeregningsperiodeV2(
                    periode = it,
                    periodeSkalIndeksreguleres = beregningsperiodeListe.first { periode -> periode.periode.fom == it.fom }.periodeSkalIndeksreguleres,
                    nesteIndeksreguleringsår = beregningsperiodeListe.first { periode -> periode.periode.fom == it.fom }.nesteIndeksreguleringsår,
                )
            }.toMutableList()
        } else {
            beregningsperiodeListe = privatAvtalePeriodeListe.map {
                // Siste (åpne) periode skal ha indeksreguleringsår
                val indeksreguleringsår = if (it.periode.til == null && privatAvtale.skalIndeksreguleres) indeksregulerPeriode.year else null
                BeregningsperiodeV2(periode = it.periode, periodeSkalIndeksreguleres = false, nesteIndeksreguleringsår = indeksreguleringsår)
            }.toMutableList()
        }
        return beregningsperiodeListe.sortedBy { it.periode.fom }
    }

    // Lager grunnlag for indeksregulering som ligger innenfor bruddPeriode
    private fun lagIndeksreguleringBeregningGrunnlag(
        beregningsperiode: ÅrMånedsperiode,
        periodeSkalIndeksreguleres: Boolean,
        nesteIndeksreguleringsår: Int? = null,
        referanseTilRolle: Grunnlagsreferanse,
        søknadsbarnReferanse: Grunnlagsreferanse,
        privatAvtale: PrivatAvtale,
        privatAvtalePeriodeListe: List<PrivatAvtalePeriode>,
        sjablonIndeksreguleringFaktorListe: List<SjablonSjablontallPeriodeGrunnlag>,
        beløpFraForrigeDelberegning: BigDecimal?,
    ): IndeksreguleringPrivatAvtaleGrunnlag {
        val privatAvtalePeriode = privatAvtalePeriodeListe
            .firstOrNull { ÅrMånedsperiode(it.periode.fom, it.periode.til).inneholder(beregningsperiode) }
            ?.let {
                PrivatAvtalePeriode(
                    referanse = it.referanse,
                    periode = it.periode,
                    beløp = it.beløp,
                    valutakode = it.valutakode,
                )
            }
            ?: throw IllegalArgumentException("Grunnlag privat avtale periode ikke funnet for periode $beregningsperiode")

        val sjablonIndeksreguleringFaktor = if (periodeSkalIndeksreguleres) {
            sjablonIndeksreguleringFaktorListe
                .firstOrNull { beregningsperiode.inneholder(it.sjablonSjablontallPeriode.periode) }
                .also { if (it == null) secureLogger.info { "Sjablon indeksreguleringsfaktor ikke funnet for periode $beregningsperiode" } }
                ?.let {
                    SjablonSjablontallBeregningGrunnlag(
                        referanse = it.referanse,
                        type = it.sjablonSjablontallPeriode.sjablon.navn,
                        verdi = it.sjablonSjablontallPeriode.verdi.toDouble(),
                    )
                }
        } else {
            null
        }

        return IndeksreguleringPrivatAvtaleGrunnlag(
            beregningsperiode = beregningsperiode,
            // Hvis sjablon ikke ble funnet skal perioden likevel ikke indeksreguleres. Dette for å unngå feil med beregning frem i tid uten
            // at sjablon har blitt lagt inn.
            periodeSkalIndeksreguleres = sjablonIndeksreguleringFaktor != null,
            nesteIndeksreguleringsår = nesteIndeksreguleringsår,
            referanseTilRolle = referanseTilRolle,
            søknadsbarnReferanse = søknadsbarnReferanse,
            privatAvtalePeriode = privatAvtalePeriode,
            sjablonIndeksreguleringFaktor = sjablonIndeksreguleringFaktor,
            beløpFraForrigeDelberegning = beløpFraForrigeDelberegning,
            referanseliste = listOfNotNull(
                privatAvtale.referanse,
                privatAvtalePeriode.referanse,
                sjablonIndeksreguleringFaktor?.referanse,
            ),
        )
    }
}

data class Beregningsperiode(val periode: ÅrMånedsperiode, val periodeSkalIndeksreguleres: Boolean)
data class BeregningsperiodeV2(val periode: ÅrMånedsperiode, val periodeSkalIndeksreguleres: Boolean, val nesteIndeksreguleringsår: Int? = null)

data class PrivatAvtale(val referanse: String, val avtaleInngåttDato: LocalDate, val skalIndeksreguleres: Boolean)

data class PrivatAvtalePeriode(val referanse: String, val periode: ÅrMånedsperiode, val beløp: BigDecimal, val valutakode: Valutakode)
