package no.nav.bidrag.beregn.barnebidrag.utils

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.barnebidrag.service.beregning.BeregnIndeksreguleringPrivatAvtaleService.delberegningIndeksreguleringPrivatAvtaleV2
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.ByggetBeløpshistorikk
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.omgjøringFeilet
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeløpshistorikkGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeløpshistorikkPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningIndeksreguleringPrivatAvtale
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtaleGrunnlagV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.erIndeksEllerAldersjustering
import no.nav.bidrag.transport.behandling.felles.grunnlag.erResultatEndringUnderGrense
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåFremmedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentAllePersoner
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.tilGrunnlagstype
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.erIndeksEllerAldersjustering
import no.nav.bidrag.transport.behandling.vedtak.response.erOrkestrertVedtak
import no.nav.bidrag.transport.behandling.vedtak.response.finnResultatFraAnnenVedtak
import no.nav.bidrag.transport.behandling.vedtak.response.finnStønadsendring
import java.time.LocalDateTime
import java.time.YearMonth

internal val vedtaksidBeregnetBeløpshistorikk = 1
internal val vedtaksidAutomatiskJobb = 2
internal val vedtaksidPrivatavtale = 3

class OmgjøringOrkestratorHelpersV2(private val vedtakService: VedtakService, private val identUtils: IdentUtils) {

    internal fun List<StønadPeriodeDto>.justerSistePeriodeTilÅBliLøpende() = mapIndexed { index, periode ->
        if (index == this.size - 1) {
            periode.copy(periode = periode.periode.copy(til = null))
        } else {
            periode
        }
    }

    fun utførDelberegningPrivatAvtalePeriode(omgjøringsberegningGrunnlag: BeregnGrunnlag): List<GrunnlagDto> = if (omgjøringsberegningGrunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG)
            .none { it.gjelderBarnReferanse == omgjøringsberegningGrunnlag.søknadsbarnReferanse }
    ) {
        emptyList()
    } else {
        delberegningIndeksreguleringPrivatAvtaleV2(omgjøringsberegningGrunnlag, omgjøringsberegningGrunnlag.periode)
    }

    fun finnBeløpshistorikkFørOmgjøringsVedtak(
        vedtak: VedtakDto,
        stønad: Stønadsid,
        personobjekter: List<GrunnlagDto>? = null,
        omgjøringsberegningGrunnlag: BeregnGrunnlag,
        omgjortVedtakVirkningstidspunkt: YearMonth,
        gjelderParagraf35c: Boolean,
        skalInnkreves: Boolean,
    ): BeløpshistorikkGrunnlag {
        val delberegningIndeksreguleringPrivatAvtalePeriodeResultat = utførDelberegningPrivatAvtalePeriode(omgjøringsberegningGrunnlag)
        val beløpshistorikk = if (gjelderParagraf35c) {
            vedtakService.hentBeløpshistorikkTilGrunnlag(
                stønadsid = stønad,
                personer = personobjekter ?: vedtak.grunnlagListe.hentAllePersoner() as List<GrunnlagDto>,
                tidspunkt = LocalDateTime.now(),
            ).innholdTilObjekt<BeløpshistorikkGrunnlag>().let {
                it.copy(
                    beløpshistorikk = it.beløpshistorikk.filter {
                        if (it.vedtaksid == null) return@filter true
                        val resultatFraVedtak = vedtakService.hentVedtak(it.vedtaksid!!)?.let { v ->
                            val relatertVedtak = if (v.type == Vedtakstype.INNKREVING && v.erOrkestrertVedtak) {
                                val resultat = v.grunnlagListe.finnResultatFraAnnenVedtak(finnFørsteTreff = true)!!
                                vedtakService.hentVedtak(resultat.vedtaksid!!)!!
                            } else {
                                v
                            }
                            val periode =
                                relatertVedtak.finnStønadsendring(stønad)?.periodeListe?.find { p -> p.periode == it.periode } ?: return@filter false
                            relatertVedtak.grunnlagListe.finnResultatFraAnnenVedtak(periode.grunnlagReferanseListe)
                        }
                        it.vedtaksid != vedtak.vedtaksid && vedtak.vedtaksid != resultatFraVedtak?.vedtaksid
                    },
                )
            }
        } else {
            vedtak.finnBeløpshistorikkGrunnlag(stønad, identUtils)
                ?: vedtakService.hentBeløpshistorikkTilGrunnlag(
                    stønadsid = stønad,
                    personer = personobjekter ?: vedtak.grunnlagListe.hentAllePersoner() as List<GrunnlagDto>,
                    tidspunkt = vedtak.vedtakstidspunkt!!.minusSeconds(1),
                ).innholdTilObjekt<BeløpshistorikkGrunnlag>().let {
                    it.copy(
                        beløpshistorikk = it.beløpshistorikk.filter { it.vedtaksid != vedtak.vedtaksid },
                    )
                }
        }

        return if (delberegningIndeksreguleringPrivatAvtalePeriodeResultat.isNotEmpty()) {
            val søknadsbarn = delberegningIndeksreguleringPrivatAvtalePeriodeResultat.hentPersonMedIdent(stønad.kravhaver.verdi)!!
            val privatavtalePerioder = delberegningIndeksreguleringPrivatAvtalePeriodeResultat
                .filtrerOgKonverterBasertPåFremmedReferanse<DelberegningIndeksreguleringPrivatAvtale>(
                    Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE,
                    gjelderBarnReferanse = søknadsbarn.referanse,
                )

            val førstePeriodeFraBeløpshistorikk =
                beløpshistorikk.beløpshistorikk.minByOrNull { it.periode.fom }?.periode

            val periodeStartInnkreving = if (skalInnkreves) {
                førstePeriodeFraBeløpshistorikk?.fom?.let { minOf(it, omgjortVedtakVirkningstidspunkt) } ?: omgjortVedtakVirkningstidspunkt
            } else {
                null
            }

            // Bare ta med privat avtale perioder til første periode i historikken
            val privatAvtalePerioderFiltrert = privatavtalePerioder
                .filter { periodeStartInnkreving == null || it.innhold.periode.fom.isBefore(periodeStartInnkreving) }
                .map {
                    DelberegningIndeksreguleringPrivatAvtale(
                        periode = it.innhold.periode,
                        nesteIndeksreguleringsår = it.innhold.nesteIndeksreguleringsår,
                        indeksreguleringFaktor = it.innhold.indeksreguleringFaktor,
                        valutakode = it.innhold.valutakode,
                        indeksregulertBeløp = it.innhold.indeksregulertBeløp,
                    )
                }

            // Juster siste periode i privat avtale historikk slik at den slutter samme tidspunkt som neste periode starter
            // Dette inkluderer også klageberegningen da den er første periode etter
            val privatavtalePerioderJustert = privatAvtalePerioderFiltrert
                .mapIndexed { index, periode ->
                    val erSistePeriode = index == privatAvtalePerioderFiltrert.size - 1
                    val tilDato = if (erSistePeriode) periodeStartInnkreving else periode.periode.til
                    periode.copy(periode = ÅrMånedsperiode(fom = periode.periode.fom, til = tilDato))
                }

            beløpshistorikk.copy(
                nesteIndeksreguleringsår = maxOf(
                    beløpshistorikk.nesteIndeksreguleringsår ?: 0,
                    privatavtalePerioder.last().innhold.nesteIndeksreguleringsår?.toInt() ?: 0,
                ).takeIf { it != 0 },
                beløpshistorikk = beløpshistorikk.beløpshistorikk + privatavtalePerioderJustert.map {
                    BeløpshistorikkPeriode(
                        periode = it.periode,
                        beløp = it.indeksregulertBeløp,
                        valutakode = it.valutakode.toString(),
                        vedtaksid = null,
                    )
                },
            )
        } else {
            beløpshistorikk
        }
    }

    internal fun byggBeløpshistorikk(
        historikk: List<BeregnetBarnebidragResultat>,
        stønad: Stønadsid,
        førPeriode: YearMonth? = null,
        beløpshistorikkFørOmgjortVedtak: BeløpshistorikkGrunnlag,
    ): ByggetBeløpshistorikk {
        val personer = historikk.flatMap { it.grunnlagListe.hentAllePersoner() }.map { it.tilDto() }.toMutableList()

        val perioder = historikk.filter { it.beregnetBarnebidragPeriodeListe.isNotEmpty() }.sortedBy { it.beregnetFraDato }.flatMap {
            val grunnlagsliste = it.grunnlagListe
            it.beregnetBarnebidragPeriodeListe.map {
                val erResultatIngenEndring = grunnlagsliste.erResultatUnderGrense(it.grunnlagsreferanseListe)
                val resultatFraVedtak = grunnlagsliste.finnResultatFraAnnenVedtak(it.grunnlagsreferanseListe)
                val erIndeksreguleringEllerAldersjustering =
                    grunnlagsliste.finnSluttberegningIReferanser(it.grunnlagsreferanseListe)?.type?.erIndeksEllerAldersjustering == true
                StønadPeriodeDto(
                    periodeid = 1,
                    periode = it.periode,
                    resultatkode = when {
                        grunnlagsliste.finnSluttberegningIReferanser(
                            it.grunnlagsreferanseListe,
                        )?.type == Grunnlagstype.SLUTTBEREGNING_INDEKSREGULERING -> Resultatkode.INDEKSREGULERING.name

                        resultatFraVedtak?.vedtakstype == Vedtakstype.INDEKSREGULERING -> Resultatkode.INDEKSREGULERING.name

                        erResultatIngenEndring -> Resultatkode.INGEN_ENDRING_UNDER_GRENSE.name

                        else -> Resultatkode.KOSTNADSBEREGNET_BIDRAG.name
                    },
                    beløp = it.resultat.beløp,
                    stønadsid = 1,
                    valutakode = "NOK",
                    vedtaksid = if (resultatFraVedtak?.vedtaksid != null) {
                        resultatFraVedtak.vedtaksid!!
                    } else if (erIndeksreguleringEllerAldersjustering) {
                        vedtaksidAutomatiskJobb
                    } else {
                        vedtaksidBeregnetBeløpshistorikk
                    },
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = null,
                )
            }
        }.sortedBy { it.periode.fom }.sorterOgJusterPerioder2()
            .filter {
                førPeriode == null || it.periode.fom.isBefore(førPeriode) ||
                    // Ta med indeks eller aldersjusteringer som er på samme periode som førPeriode. Det er for at det skal bli riktig med 12% sjekken senere
                    (it.periode.fom == førPeriode && it.vedtaksid == vedtaksidAutomatiskJobb)
            }
            .justerSistePeriodeTilÅBliLøpende()

        val perioderFørFraBeløpshistorikk = beløpshistorikkFørOmgjortVedtak.beløpshistorikk
            .map {
                val vedtak = it.vedtaksid?.let { vedtakService.hentVedtak(it) }
                val erResultatIngenEndring = vedtak?.finnStønadsendring(stønad)
                    ?.periodeListe
                    ?.find { vp -> vp.periode.fom == it.periode.fom }
                    ?.let { periode -> vedtak.grunnlagListe.erResultatUnderGrense(periode.grunnlagReferanseListe) }
                    ?: false

                StønadPeriodeDto(
                    periodeid = 1,
                    periode = it.periode,
                    resultatkode = when {
                        vedtak?.type == Vedtakstype.INDEKSREGULERING -> Resultatkode.INDEKSREGULERING.name
                        erResultatIngenEndring -> Resultatkode.INGEN_ENDRING_UNDER_GRENSE.name
                        else -> Resultatkode.KOSTNADSBEREGNET_BIDRAG.name
                    },
                    beløp = it.beløp,
                    stønadsid = 1,
                    valutakode = "NOK",
                    vedtaksid = when {
                        vedtak != null && vedtak.type.erIndeksEllerAldersjustering -> vedtaksidAutomatiskJobb
                        else -> it.vedtaksid ?: vedtaksidPrivatavtale
                    },
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = null,
                )
            }

        val sistePeriode = perioder.maxByOrNull { it.periode.fom }
        val sisteRelevantePeriode = perioder.sortedBy { it.periode.fom }
            .lastOrNull { it.resultatkode != Resultatkode.INGEN_ENDRING_UNDER_GRENSE.name }
        val nesteIndeksår = when {
            // Betyr at siste periode var INGEN_ENDRING_UNDER_GRENSE
            sisteRelevantePeriode == null && perioder.isNotEmpty() ->
                sistePeriode?.periode?.til?.year ?: sistePeriode?.periode?.fom?.year ?: YearMonth.now().year

            // Neste år etter siste periode som ikke var INGEN_ENDRING_UNDER_GRENSE
            sisteRelevantePeriode != null -> sisteRelevantePeriode.periode.fom.year + 1

            // Ellers bruk neste indeksår fra eksisterende beløpshistorikk. Ingen endring har blitt gjort på beløpshistorikken
            else -> beløpshistorikkFørOmgjortVedtak.nesteIndeksreguleringsår ?: (YearMonth.now().year + 1)
        }
        val stønadDto = opprettStønad(stønad).copy(
            førsteIndeksreguleringsår = nesteIndeksår,
            nesteIndeksreguleringsår = nesteIndeksår,
            innkreving = Innkrevingstype.MED_INNKREVING,
            periodeListe = (perioderFørFraBeløpshistorikk + perioder).sorterOgJusterPerioder2(),
        )

        personer.hentPersonMedIdent(stønad.kravhaver.verdi) ?: personer.hentPersonForNyesteIdent(identUtils, stønad.kravhaver) ?: run {
            val grunnlag = opprettPersonGrunnlag(stønad.kravhaver, Rolletype.BARN)
            personer.add(grunnlag)
        }

        personer.hentPersonMedIdent(stønad.skyldner.verdi) ?: personer.hentPersonForNyesteIdent(identUtils, stønad.skyldner) ?: run {
            val grunnlag = opprettPersonGrunnlag(stønad.skyldner, Rolletype.BIDRAGSPLIKTIG)
            personer.add(grunnlag)
        }
        val grunnlagBeløpshistorikk = stønadDto.tilGrunnlag(personer.toMutableList(), stønad, identUtils)
        val grunnlagsliste = (listOf(grunnlagBeløpshistorikk) + personer).toSet().toList()

        return ByggetBeløpshistorikk(nesteIndeksår, grunnlagsliste, stønadDto, grunnlagBeløpshistorikk)
    }

    private fun erIndeksregulering(vedtaksId: Int?) = vedtaksId?.let { vedtakService.hentVedtak(it) }?.type == Vedtakstype.INDEKSREGULERING
    fun opprettPersonGrunnlag(ident: Personident, rolle: Rolletype): GrunnlagDto = GrunnlagDto(
        referanse = "person_${rolle.name}_${ident.verdi}",
        type = rolle.tilGrunnlagstype(),
        innhold = POJONode(
            Person(
                ident = ident,
                navn = null,
                fødselsdato = identUtils.hentFødselsdato(ident) ?: omgjøringFeilet("Fant ikke fødselsdato for person $ident med rolle $rolle"),
            ),
        ),
    )

    private fun List<StønadPeriodeDto>.sorterOgJusterPerioder2(): List<StønadPeriodeDto> {
        val sortert = sortedBy { it.periode.fom }

        return sortert.mapIndexed { indeks, resultatPeriode ->
            val nesteFom = sortert.getOrNull(indeks + 1)?.periode?.fom
            resultatPeriode.copy(
                periode = ÅrMånedsperiode(fom = resultatPeriode.periode.fom, til = nesteFom ?: resultatPeriode.periode.til),
            )
        }
    }

    private fun List<GrunnlagDto>.erResultatUnderGrense(grunnlagsreferanseListe: List<Grunnlagsreferanse>): Boolean {
        val søknadsbarn = finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<Person>(
            Grunnlagstype.PERSON_SØKNADSBARN,
            grunnlagsreferanseListe,
        ).firstOrNull() ?: run {
            val refererTil = grunnlagsreferanseListe.mapNotNull { gr -> find { it.referanse == gr }?.gjelderBarnReferanse }
            filtrerOgKonverterBasertPåEgenReferanse<Person>(
                Grunnlagstype.PERSON_SØKNADSBARN,
                referanse = refererTil.firstOrNull() ?: "",
            ).firstOrNull()
        }
        return søknadsbarn?.let { erResultatEndringUnderGrense(søknadsbarn.referanse) } ?: false
    }
}
