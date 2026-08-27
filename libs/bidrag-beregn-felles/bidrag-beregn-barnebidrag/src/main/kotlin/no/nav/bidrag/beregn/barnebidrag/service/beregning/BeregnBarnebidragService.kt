package no.nav.bidrag.beregn.barnebidrag.service.beregning

import no.nav.bidrag.beregn.barnebidrag.bo.BeløpshistorikkPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BeregnEndeligBidragServiceRespons
import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrenseDelberegningPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrensePeriodeDelberegningPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.PrivatAvtaleIndeksregulertPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.PrivatAvtaleIndeksregulertPeriodeGrunnlagV2
import no.nav.bidrag.beregn.barnebidrag.bo.SluttberegningPeriodeGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SluttberegningPeriodeGrunnlagV2
import no.nav.bidrag.beregn.barnebidrag.mapper.AldersjusteringMapper.justerTilPeriodeHvisBarnetBlir18ÅrIBeregningsperioden
import no.nav.bidrag.beregn.barnebidrag.mapper.NettoTilsynsutgiftMapper
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.beregn.core.exception.BegrensetRevurderingLikEllerLavereEnnLøpendeBidragException
import no.nav.bidrag.beregn.core.exception.BegrensetRevurderingLøpendeForskuddManglerException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgOppfostringsbidragBeregningException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException
import no.nav.bidrag.beregn.core.service.BeregnService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.diverse.InntektBeløpstype
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.avrundetMedToDesimaler
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningResultatBarnV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatVedtakV2
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.felles.valider
import no.nav.bidrag.transport.behandling.felles.grunnlag.BeløpshistorikkGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningAndelAvBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragTilFordelingLøpendeBidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragTilFordelingPrivatAvtale
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningEndringSjekkGrense
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningEndringSjekkGrensePeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningFatteVedtak
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningIndeksreguleringPrivatAvtale
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningPrivatAvtale
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.InnholdMedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtaleGrunnlagV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidrag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.SøknadGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.VirkningstidspunktGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragspliktig
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåFremmedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGyldigeGrunnlagForBarn
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.personObjekt
import no.nav.bidrag.transport.felles.commonObjectmapper
import java.math.BigDecimal
import java.time.YearMonth

class BeregnBarnebidragService : BeregnService() {

    fun opprettAvslagResultat(mottattGrunnlag: BeregnGrunnlag): BeregnetBarnebidragResultat = BeregnetBarnebidragResultat(
        beregnetBarnebidragPeriodeListe =
        listOf(
            ResultatPeriode(
                grunnlagsreferanseListe = mottattGrunnlag.grunnlagListe.map { it.referanse },
                periode = mottattGrunnlag.periode,
                resultat = ResultatBeregning(
                    beløp = null,
                ),
            ),
        ),
        grunnlagListe = mottattGrunnlag.grunnlagListe,
    )

    fun beregnBarnebidragAlleSøknadsbarn(beregnGrunnlagListe: List<BeregnGrunnlag>): List<Pair<BidragsberegningResultatBarnV2, List<GrunnlagDto>>> {
        // Kaller beregning for ett og ett søknadsbarn
        val resultatBeregningAlleBarn = beregnGrunnlagListe.map { beregningBarn ->

            try {
                val beregningResultat =
                    beregnBarnebidrag(beregningBarn)

                BidragsberegningResultatBarnV2(
                    søknadsbarnreferanse = beregningBarn.søknadsbarnReferanse,
                    resultatVedtakListe = listOf(
                        ResultatVedtakV2(
                            periodeListe = beregningResultat.beregnetBarnebidragPeriodeListe,
                            delvedtak = false,
                            omgjøringsvedtak = false,
                            vedtakstype = Vedtakstype.ENDRING,
                        ),
                    ),
                ) to beregningResultat.grunnlagListe
            } catch (e: Exception) {
                BidragsberegningResultatBarnV2(
                    søknadsbarnreferanse = beregningBarn.søknadsbarnReferanse,
                    resultatVedtakListe = emptyList(),
                    beregningsfeil = e,
                ) to beregningBarn.grunnlagListe
            }
        }
        return resultatBeregningAlleBarn
    }

    // Komplett beregning av barnebidrag
    fun beregnBarnebidrag(mottattGrunnlag: BeregnGrunnlag): BeregnetBarnebidragResultat {
        secureLogger.debug { "Beregning av barnebidrag - følgende request mottatt: ${commonObjectmapper.writeValueAsString(mottattGrunnlag)}" }

        val virkningstidspunkt = mottattGrunnlag.grunnlagListe.filtrerOgKonverterBasertPåEgenReferanse<VirkningstidspunktGrunnlag>(
            Grunnlagstype.VIRKNINGSTIDSPUNKT,
        ).firstOrNull()

        if (virkningstidspunkt != null && virkningstidspunkt.innhold.avslag != null) {
            return BeregnetBarnebidragResultat(
                beregnetBarnebidragPeriodeListe =
                listOf(
                    ResultatPeriode(
                        grunnlagsreferanseListe = listOf(virkningstidspunkt.referanse),
                        periode = ÅrMånedsperiode(mottattGrunnlag.periode.fom, null),
                        resultat =
                        ResultatBeregning(
                            beløp = null,
                        ),
                    ),
                ),
                grunnlagListe = listOf(virkningstidspunkt.grunnlag as GrunnlagDto),
            )
        }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av barnebidrag: " + e.message)
        }

        // Sjekker om søknadsbarnet fyller 18 år i beregningsperioden
        val utvidetGrunnlagJustert = justerTilPeriodeHvisBarnetBlir18ÅrIBeregningsperioden(mottattGrunnlag)
        var utvidetGrunnlag = utvidetGrunnlagJustert.beregnGrunnlag
        val åpenSluttperiode = utvidetGrunnlagJustert.åpenSluttperiode

        // Kaller delberegninger
        val delberegningBidragsevneResultat = BeregnBidragsevneService.delberegningBidragsevne(utvidetGrunnlag, åpenSluttperiode)

        val delberegningNettoTilsynsutgiftResultat =
            BeregnNettoTilsynsutgiftService.delberegningNettoTilsynsutgift(utvidetGrunnlag, åpenSluttperiode)

        utvidetGrunnlag = utvidetGrunnlag.copy(
            grunnlagListe = (utvidetGrunnlag.grunnlagListe + delberegningNettoTilsynsutgiftResultat).distinctBy(GrunnlagDto::referanse),
        )
        val delberegningUnderholdskostnadResultat =
            BeregnUnderholdskostnadService.delberegningUnderholdskostnad(utvidetGrunnlag, åpenSluttperiode)

        utvidetGrunnlag = utvidetGrunnlag.copy(
            grunnlagListe = (utvidetGrunnlag.grunnlagListe + delberegningUnderholdskostnadResultat).distinctBy(GrunnlagDto::referanse),
        )
        val delberegningBpAndelUnderholdskostnadResultat =
            BeregnBpAndelUnderholdskostnadService.delberegningBpAndelUnderholdskostnad(utvidetGrunnlag, åpenSluttperiode)

        val delberegningSamværsfradragResultat = BeregnSamværsfradragService.delberegningSamværsfradrag(utvidetGrunnlag, åpenSluttperiode)

        utvidetGrunnlag = utvidetGrunnlag.copy(
            grunnlagListe = (
                utvidetGrunnlag.grunnlagListe + delberegningBidragsevneResultat + delberegningNettoTilsynsutgiftResultat +
                    delberegningUnderholdskostnadResultat + delberegningBpAndelUnderholdskostnadResultat + delberegningSamværsfradragResultat
                )
                .distinctBy(GrunnlagDto::referanse),
        )
        val delberegningEndeligBidragResultat = BeregnEndeligBidragService.delberegningEndeligBidrag(utvidetGrunnlag, åpenSluttperiode)

        val resultatPeriodeListe: List<ResultatPeriode>
        val beløpshistorikkGrunnlag = emptyList<GrunnlagDto>()
        var delberegningEndringSjekkGrensePeriodeResultat = emptyList<GrunnlagDto>()
        var delberegningEndringSjekkGrenseResultat = emptyList<GrunnlagDto>()
        var delberegningIndeksreguleringPrivatAvtaleResultat = emptyList<GrunnlagDto>()

        // Skal sjekke mot minimumsgrense for endring ("12%-regelen") hvis egetTiltak er false og det ikke er klageberegning
        if (mottattGrunnlag.skalSjekkeMotMinimumsgrenseForEndring()) {
            val sjekkMotMinimumsgrenseForEndringResultat = sjekkMotMinimumsgrenseForEndring(
                mottattGrunnlag = mottattGrunnlag,
                utvidetGrunnlagJustert = utvidetGrunnlagJustert,
                delberegningEndeligBidragResultat = delberegningEndeligBidragResultat,
                åpenSluttperiode = åpenSluttperiode,
            )
            resultatPeriodeListe = sjekkMotMinimumsgrenseForEndringResultat.resultatPeriodeListe
            delberegningEndringSjekkGrensePeriodeResultat = sjekkMotMinimumsgrenseForEndringResultat.delberegningEndringSjekkGrensePeriodeResultat
            delberegningEndringSjekkGrenseResultat = sjekkMotMinimumsgrenseForEndringResultat.delberegningEndringSjekkGrenseResultat
            delberegningIndeksreguleringPrivatAvtaleResultat =
                sjekkMotMinimumsgrenseForEndringResultat.delberegningIndeksreguleringPrivatAvtaleResultat
        } else {
            resultatPeriodeListe = lagResultatPerioder(delberegningEndeligBidragResultat.grunnlagListe)
        }

        // Slår sammen grunnlag fra alle delberegninger
        val foreløpigResultatGrunnlagListe = (
            delberegningBidragsevneResultat + delberegningNettoTilsynsutgiftResultat + delberegningUnderholdskostnadResultat +
                delberegningBpAndelUnderholdskostnadResultat + delberegningSamværsfradragResultat + delberegningEndeligBidragResultat.grunnlagListe +
                beløpshistorikkGrunnlag
            )
            .distinctBy { it.referanse }
            .sortedBy { it.referanse }

        // Filtrerer bort grunnlag som ikke blir referert (dette vil skje f.eks. hvis barnet er selvforsørget og hvis barnet bor hos BP - da
        // regnes ikke alle delberegninger som relevante). Delberegninger for sjekk mot minimumsgrense for endring står i en særstilling ettersom de
        // ikke refereres noe sted, men likevel skal være med i resultatgrunnlaget om de finnes.
        val endeligResultatGrunnlagListe = (
            filtrerResultatGrunnlag(
                foreløpigResultatGrunnlagListe = foreløpigResultatGrunnlagListe,
                refererteReferanserListe = resultatPeriodeListe.flatMap { it.grunnlagsreferanseListe },
            ) + delberegningEndringSjekkGrenseResultat +
                delberegningEndringSjekkGrensePeriodeResultat +
                delberegningIndeksreguleringPrivatAvtaleResultat
            )
            .toMutableList()

        // Mapper ut grunnlag for Person-objekter som er brukt
        endeligResultatGrunnlagListe.addAll(
            mapPersonobjektGrunnlag(
                resultatGrunnlagListe = endeligResultatGrunnlagListe,
                personobjektGrunnlagListe = mottattGrunnlag.grunnlagListe,
            ),
        )

        val beregnetBarnebidragResultat = BeregnetBarnebidragResultat(
            beregnetBarnebidragPeriodeListe = justerPerioderForOpphørsdato(resultatPeriodeListe, mottattGrunnlag.opphørsdato),
            grunnlagListe = endeligResultatGrunnlagListe.distinctBy { it.referanse }.sortedBy { it.referanse },
        )

        // Kaster exception hvis det er utført begrenset revurdering og det er minst ett tilfelle hvor beregnet bidrag er lavere enn løpende bidrag
        // eller hvis løpende forskudd mangler i første beregningsperiode
        if (delberegningEndeligBidragResultat.skalKasteBegrensetRevurderingException) {
            if (delberegningEndeligBidragResultat.feilmelding.contains("løpende forskudd mangler")) {
                throw BegrensetRevurderingLøpendeForskuddManglerException(
                    melding = delberegningEndeligBidragResultat.feilmelding,
                    periodeListe = delberegningEndeligBidragResultat.perioderMedFeilListe,
                    data = beregnetBarnebidragResultat,
                )
            } else {
                throw BegrensetRevurderingLikEllerLavereEnnLøpendeBidragException(
                    melding = delberegningEndeligBidragResultat.feilmelding,
                    periodeListe = delberegningEndeligBidragResultat.perioderMedFeilListe,
                    data = beregnetBarnebidragResultat,
                )
            }
        }

        secureLogger.debug {
            "Beregning av barnebidrag - følgende respons returnert: ${
                commonObjectmapper.writeValueAsString(
                    beregnetBarnebidragResultat,
                )
            }"
        }
        return beregnetBarnebidragResultat
    }

    // Komplett beregning av barnebidrag
    fun beregnBarnebidragV2(
        beregningsperiode: ÅrMånedsperiode,
        grunnlagSøknadsbarnListe: List<BeregnGrunnlag>,
        grunnlagLøpendeBidragListe: List<BeregnGrunnlag> = emptyList(),
        grunnlagPrivatAvtaleListe: List<BeregnGrunnlag> = emptyList(),
        grunnlagValutakursListe: List<GrunnlagDto> = emptyList(),
        grunnlagOpprinneligBeregningListe: List<GrunnlagDto> = emptyList(),
    ): List<BeregnetBarnebidragResultatV2> {
        secureLogger.debug {
            "Beregning av barnebidrag - for periode ${beregningsperiode.fom}-${beregningsperiode.til}. " +
                "GrunnlagSøknadsbarnListe: ${commonObjectmapper.writeValueAsString(grunnlagSøknadsbarnListe)}. " +
                "GrunnlagLøpendeBidragListe: ${commonObjectmapper.writeValueAsString(grunnlagLøpendeBidragListe)}. " +
                "GrunnlagPrivatAvtaleListe: ${commonObjectmapper.writeValueAsString(grunnlagPrivatAvtaleListe)}. " +
                "GrunnlagValutakursListe: ${commonObjectmapper.writeValueAsString(grunnlagValutakursListe)}." +
                "GrunnlagOpprinneligBeregningListe: ${commonObjectmapper.writeValueAsString(grunnlagOpprinneligBeregningListe)}."
        }

        val utvidetGrunnlagSøknadsbarnListe = grunnlagSøknadsbarnListe.map { beregningBarn ->
            val virkningstidspunkt = beregningBarn.grunnlagListe.filtrerOgKonverterBasertPåEgenReferanse<VirkningstidspunktGrunnlag>(
                Grunnlagstype.VIRKNINGSTIDSPUNKT,
            ).firstOrNull()

            // Brukes ifbm opphør / direkte avslag?
            // TODO Sjekke om denne koden er i bruk eller kan fjernes
            if (virkningstidspunkt != null && virkningstidspunkt.innhold.avslag != null) {
                BeregnetBarnebidragResultatV2(
                    søknadsbarnreferanse = beregningBarn.søknadsbarnReferanse,
                    beregnetBarnebidragResultat = BeregnetBarnebidragResultat(
                        beregnetBarnebidragPeriodeListe =
                        listOf(
                            ResultatPeriode(
                                grunnlagsreferanseListe = listOf(virkningstidspunkt.referanse),
                                periode = ÅrMånedsperiode(beregningBarn.periode.fom, null),
                                resultat =
                                ResultatBeregning(
                                    beløp = null,
                                ),
                            ),
                        ),
                        grunnlagListe = listOf(virkningstidspunkt.grunnlag as GrunnlagDto),
                    ),
                )
            }

            // Kontroll av inputdata
            beregningBarn.valider()

            // Sjekker om søknadsbarnet fyller 18 år i beregningsperioden
            val utvidetGrunnlag = justerTilPeriodeHvisBarnetBlir18ÅrIBeregningsperioden(beregningBarn)
                // Sjekker om barnet er del av opprinnelig behandling (eller er del av en revurderingssøknad)
                .sjekkOmBarnetErDelAvOpprinneligBehandling()
                // Legger til virkning-fra-periode. Vil bli en egen bruddperiode hvis den avviker fra beregningsperiode fra.
                .leggTilVirkningFraPeriode(virkningstidspunkt)

            // Kaller alle standard delberegninger for søknadsbarn
            utvidetGrunnlag.utførAlleStandardDelberegningerForSøknadsbarn()
        }

        // Beregner nytt samværsfradrag for løpende bidrag
        val utvidetGrunnlagLøpendeBidragListe = grunnlagLøpendeBidragListe.map { utvidGrunnlagLøpendeBidrag(it) }

        // Utfører indeksregulering og beregner nytt samværsfradrag for privat avtale
        val utvidetGrunnlagPrivatAvtaleListe = grunnlagPrivatAvtaleListe.map { utvidGrunnlagPrivatAvtale(it, beregningsperiode) }

        // Endelig bidrag delberegninger: Kaller en samlemetode for endelig beregning. Denne tar alle barn som input.
        val endeligBidragBeregningListe = BeregnEndeligBidragServiceV2.delberegningEndeligBidrag(
            beregningsperiode = beregningsperiode,
            grunnlagSøknadsbarnListe = utvidetGrunnlagSøknadsbarnListe,
            grunnlagLøpendeBidragListe = utvidetGrunnlagLøpendeBidragListe,
            grunnlagPrivatAvtaleListe = utvidetGrunnlagPrivatAvtaleListe,
            grunnlagValutakurs = grunnlagValutakursListe.firstOrNull(),
        )

        // Sjekker om det finnes perioder hvor det ikke er full evne for noen av søknadsbarna og det samtidig finnes løpende bidrag (norske bidrag)
        // eller privat avtale (norske bidrag). I så fall må saksbehandler varsle andre BM'er og det må innhentes fullstendige grunnlag i de andre
        // løpende sakene. Hvis dette inntreffer settes det et flagg her, og det kastes en exception etter at beregningen er fullført, som håndteres
        // videre av den som kaller beregningen (f.eks. orkestratoren). Det forutsettes at grunnlagLøpendeBidragListe og grunnlagPrivatAvtaleListe er
        // tom hvis vi har alle grunnlag vi trenger for å beregne FF.
        val ikkeFullBidragsevneOgUfullstendigeGrunnlag =
            (
                endeligBidragBeregningListe.harDelberegningBidragTilFordelingLøpendeBidragMedNorskeBidrag() ||
                    endeligBidragBeregningListe.harDelberegningBidragTilFordelingPrivatAvtaleMedNorskeBidrag()
                ) &&
                endeligBidragBeregningListe.harPerioderMedEvnesprekk()

        // Sjekker om det finnes perioder hvor det ikke er full evne for noen av søknadsbarna og det samtidig finnes ett eller flere
        // oppfostringsbidrag. Dette skal resultere i exception og videre manuell behandling.
        val ikkeFullBidragsevneOgOppfostringsbidrag =
            (endeligBidragBeregningListe.inneholderOppfostringsbidrag() && endeligBidragBeregningListe.harPerioderMedEvnesprekk())

        val beregnetBarnebidragResultatListe = endeligBidragBeregningListe.map { beregningBarn ->

            // Sjekker om det skal fattes vedtak
            val delberegningFatteVedtak = BeregnFatteVedtakService.delberegningFatteVedtak(
                beregnGrunnlagGjeldendeBarn = beregningBarn,
                beregnGrunnlagAlleBarnListe = endeligBidragBeregningListe,
                grunnlagOpprinneligBeregningListe = grunnlagOpprinneligBeregningListe,
                sjekkEvneMotPeriode = beregningsperiode.til ?: YearMonth.now(),
            )

            // Sjekk mot minimumsgrense for endring ("12%-regelen")
            val sjekkMotMinimumsgrenseForEndringResultat =
                beregningBarn.minimumsgrenseForEndring(
                    ikkeFullBidragsevneOgUfullstendigeGrunnlag = ikkeFullBidragsevneOgUfullstendigeGrunnlag,
                )

            // Bygger endelig grunnlagsliste
            val endeligResultatGrunnlagListe = byggEndeligResultatGrunnlagListe(
                grunnlagListe = beregningBarn.beregnGrunnlag.grunnlagListe,
                resultatPeriodeListe = sjekkMotMinimumsgrenseForEndringResultat.resultatPeriodeListe,
                ekstraGrunnlag = sjekkMotMinimumsgrenseForEndringResultat.delberegningEndringSjekkGrenseResultat +
                    sjekkMotMinimumsgrenseForEndringResultat.delberegningEndringSjekkGrensePeriodeResultat +
                    sjekkMotMinimumsgrenseForEndringResultat.delberegningIndeksreguleringPrivatAvtaleResultat +
                    delberegningFatteVedtak,
            )

            // Bygger resultat
            val beregnetBarnebidragResultat = BeregnetBarnebidragResultat(
                beregnetBarnebidragPeriodeListe = justerPerioderForOpphørsdato(
                    periodeliste = sjekkMotMinimumsgrenseForEndringResultat.resultatPeriodeListe,
                    opphørsdato = beregningBarn.beregnGrunnlag.opphørsdato,
                ),
                grunnlagListe = endeligResultatGrunnlagListe.distinctBy { it.referanse }.sortedBy { it.referanse },
            )

            // Kaster exception hvis det er utført begrenset revurdering og det er minst ett tilfelle hvor beregnet bidrag er lavere enn løpende bidrag
            // eller hvis løpende forskudd mangler i første beregningsperiode
            // TODO Logikk for begrenset revurdering er pt ikke i bruk i ny løsning (avventer avklaringer). Hvis den skal tas i bruk må logikken
            // TODO under her aktiveres. Det må også legges inn logikk i BeregnEndeligBidragServiceV2 tilsvarende BeregnEndeligBidragService.
            // TODO Bidraget skal IKKE sjekkes for reduksjon til forskuddssats hvis forholdsmessig fordeling slår ut.
//                if (delberegningEndeligBidragResultat.skalKasteBegrensetRevurderingException) {
//                    if (delberegningEndeligBidragResultat.feilmelding.contains("løpende forskudd mangler")) {
//                        throw BegrensetRevurderingLøpendeForskuddManglerException(
//                            melding = delberegningEndeligBidragResultat.feilmelding,
//                            periodeListe = delberegningEndeligBidragResultat.perioderMedFeilListe,
//                            data = beregnetBarnebidragResultat,
//                        )
//                    } else {
//                        throw BegrensetRevurderingLikEllerLavereEnnLøpendeBidragException(
//                            melding = delberegningEndeligBidragResultat.feilmelding,
//                            periodeListe = delberegningEndeligBidragResultat.perioderMedFeilListe,
//                            data = beregnetBarnebidragResultat,
//                        )
//                    }
//                }

            // Hvis virkning-fra-periode er senere enn beregningsperiode.fom, vil det legges ut tomme resultatperioder for perioder før
            // virkning fra dato, men alle resultatgrunnlag tas med (det skal bare fattes vedtak for perioder etter virkning-fra-periode).
            val beregnetBarnebidragPeriodeListe = filtrerOgJusterPerioderForVedtak(
                virkningFraPeriode = beregningBarn.virkningFraPeriode,
                beregningsperiodeFom = beregningsperiode.fom,
                beregnetBarnebidragPeriodeListe = beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe,
            )

            // Hvis det ikke skal fattes vedtak, leveres periodeliste, men med en indikator i responsen på at det ikke anbefales å fatte vedtak.
            // Hvis det er revurderingsbarn og alle søknadsbarn har avslag i siste periode, leveres det tom periodeliste (skal da ikke være mulig å
            // fatte vedtak).
            val fatteVedtakResultat = delberegningFatteVedtak
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningFatteVedtak>(
                    Grunnlagstype.DELBEREGNING_FATTE_VEDTAK,
                )
                .map { it.innhold.fatteVedtakResultat }
                .first()

            BeregnetBarnebidragResultatV2(
                beregningBarn.beregnGrunnlag.søknadsbarnReferanse,
                BeregnetBarnebidragResultat(
                    beregnetBarnebidragPeriodeListe =
                    if (!(fatteVedtakResultat.erRevurderingsbarn && fatteVedtakResultat.ingenOverlappendePerioderMedSøknadsbarn)) {
                        beregnetBarnebidragPeriodeListe
                    } else {
                        emptyList()
                    },
                    grunnlagListe = beregnetBarnebidragResultat.grunnlagListe,
                ),
                fatteVedtakAnbefalt = fatteVedtakResultat.skalFatteVedtak,
                avvistRevurderingsbarn = fatteVedtakResultat.ingenOverlappendePerioderMedSøknadsbarn && fatteVedtakResultat.erRevurderingsbarn,
            )
        }

        // Kaster exception hvis det finnes perioder hvor det ikke er full evne og det finnes løpende bidrag som inneholder oppfostringsbidrag
        if (ikkeFullBidragsevneOgOppfostringsbidrag) {
            secureLogger.debug {
                "Beregning av barnebidrag - ikkeFullBidragsevneOgOppfostringsbidrag exception med følgende respons: " +
                    commonObjectmapper.writeValueAsString(beregnetBarnebidragResultatListe)
            }
            throw IkkeFullBidragsevneOgOppfostringsbidragBeregningException(
                melding = "Det finnes perioder med evnesprekk og løpende bidrag med oppfostringsbidrag. Disse må håndteres manuelt før vedtak kan " +
                    "fattes.",
                data = beregnetBarnebidragResultatListe,
            )
        }

        // Kaster exception hvis det finnes perioder hvor det ikke er full evne og det finnes løpende bidrag eller privat avtale med ufullstendige
        // grunnlag
        if (ikkeFullBidragsevneOgUfullstendigeGrunnlag) {
            secureLogger.debug {
                "Beregning av barnebidrag - ikkeFullBidragsevneOgUfullstendigeGrunnlag exception med følgende respons: " +
                    commonObjectmapper.writeValueAsString(beregnetBarnebidragResultatListe)
            }
            throw IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException(
                melding = "Det finnes perioder med evnesprekk. Nye grunnlag må hentes inn for løpende bidrag og/eller privat avtale før vedtak kan " +
                    "fattes.",
                data = beregnetBarnebidragResultatListe,
            )
        }

        secureLogger.debug {
            "Beregning av barnebidrag - følgende respons returnert: ${
                commonObjectmapper.writeValueAsString(
                    beregnetBarnebidragResultatListe,
                )
            }"
        }
        return beregnetBarnebidragResultatListe
    }

    private fun List<BeregnGrunnlagJustert>.harPerioderMedEvnesprekk(): Boolean = this
        .flatMap { it.beregnGrunnlag.grunnlagListe }
        .filtrerOgKonverterBasertPåEgenReferanse<DelberegningAndelAvBidragsevne>(
            Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE,
        )
        .any { !it.innhold.harBPFullEvne }

    private fun List<BeregnGrunnlagJustert>.harDelberegningBidragTilFordelingPrivatAvtaleMedNorskeBidrag(): Boolean = this
        .flatMap { it.beregnGrunnlag.grunnlagListe }
        .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragTilFordelingPrivatAvtale>(
            Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE,
        )
        .any { it.innhold.erNorskBidrag }

    private fun List<BeregnGrunnlagJustert>.harDelberegningBidragTilFordelingLøpendeBidragMedNorskeBidrag(): Boolean = this
        .flatMap { it.beregnGrunnlag.grunnlagListe }
        .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragTilFordelingLøpendeBidrag>(
            Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG,
        )
        .any { it.innhold.erNorskBidrag }

    private fun List<BeregnGrunnlagJustert>.inneholderOppfostringsbidrag(): Boolean = this
        .flatMap { it.beregnGrunnlag.grunnlagListe }
        .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragTilFordelingLøpendeBidrag>(
            Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING_LØPENDE_BIDRAG,
        )
        .any { it.innhold.erOppfostringsbidrag }

    // Sjekker om barnetillegg eksisterer for en gitt rolle
    private fun barnetilleggEksisterer(grunnlag: BeregnGrunnlag, referanse: String): Boolean = grunnlag.grunnlagListe
        .filtrerOgKonverterBasertPåFremmedReferanse<InntektsrapporteringPeriode>(
            grunnlagType = Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
            referanse = referanse,
        )
        .filter { it.innhold.inntektsrapportering == Inntektsrapportering.BARNETILLEGG }
        .any { it.innhold.gjelderBarn == grunnlag.søknadsbarnReferanse }

    private fun justerPerioderForOpphørsdato(periodeliste: List<ResultatPeriode>, opphørsdato: YearMonth?): List<ResultatPeriode> {
        if (opphørsdato == null) return periodeliste
        if (periodeliste.isEmpty()) return emptyList()

        val filtrertePerioder = periodeliste.filter { it.periode.fom.isBefore(opphørsdato) }
        if (filtrertePerioder.isEmpty()) return emptyList()

        val sistePeriode = filtrertePerioder.maxByOrNull { it.periode.fom }!!
        val sistePeriodeIndeks = filtrertePerioder.indexOf(sistePeriode)

        // Juster siste periodeTil for opphørsdato. Skal ikke ta hensyn til opphørsdato hvis beløp = null ("avslag")
        return filtrertePerioder.mapIndexed { indeks, periode ->
            if ((indeks == sistePeriodeIndeks) && (periode.resultat.beløp != null)) {
                periode.copy(periode = periode.periode.copy(til = opphørsdato))
            } else {
                periode
            }
        }
    }

    // Utvider grunnlag for løpende bidrag med samværsfradrag
    private fun utvidGrunnlagLøpendeBidrag(beregningBarn: BeregnGrunnlag): BeregnGrunnlagJustert {
        // Kontroll av inputdata
        beregningBarn.valider()

        // Sjekker om søknadsbarnet fyller 18 år i beregningsperioden
        var utvidetGrunnlag = justerTilPeriodeHvisBarnetBlir18ÅrIBeregningsperioden(beregningBarn)

        // Kaller delberegninger

        // Samværsfradrag
        val delberegningSamværsfradragResultat = BeregnSamværsfradragService.delberegningSamværsfradrag(
            mottattGrunnlag = utvidetGrunnlag.beregnGrunnlag,
            åpenSluttperiode = utvidetGrunnlag.åpenSluttperiode,
            erLøpendeBidrag = true,
            erPrivatAvtale = false,
        )
        utvidetGrunnlag = utvidetGrunnlag.utvidMedNyeGrunnlag(delberegningSamværsfradragResultat)

        return utvidetGrunnlag
    }

    // Utvider grunnlag for privat avtale med indeksregulering og samværsfradrag
    private fun utvidGrunnlagPrivatAvtale(beregningBarn: BeregnGrunnlag, beregningsperiode: ÅrMånedsperiode): BeregnGrunnlagJustert {
        // Kontroll av inputdata
        beregningBarn.valider()

        // Sjekker om søknadsbarnet fyller 18 år i beregningsperioden
        var utvidetGrunnlag = justerTilPeriodeHvisBarnetBlir18ÅrIBeregningsperioden(beregningBarn)

        // Kaller delberegninger

        // Indeksregulering av privat avtale
        val delberegningIndeksreguleringPrivatAvtaleResultat =
            BeregnIndeksreguleringPrivatAvtaleService.delberegningIndeksreguleringPrivatAvtaleV2(
                grunnlag = beregningBarn,
                beregningsperiode = beregningsperiode,
            )
        utvidetGrunnlag = utvidetGrunnlag.utvidMedNyeGrunnlag(delberegningIndeksreguleringPrivatAvtaleResultat)

        // Samværsfradrag
        val delberegningSamværsfradragResultat = BeregnSamværsfradragService.delberegningSamværsfradrag(
            mottattGrunnlag = utvidetGrunnlag.beregnGrunnlag,
            åpenSluttperiode = false,
            erLøpendeBidrag = false,
            erPrivatAvtale = true,
        )
        utvidetGrunnlag = utvidetGrunnlag.utvidMedNyeGrunnlag(delberegningSamværsfradragResultat)

        return utvidetGrunnlag
    }

    // Filtrerer og justerer resultatperioder basert på om vedtak skal fattes og virkning-fra-periode
    private fun filtrerOgJusterPerioderForVedtak(
        virkningFraPeriode: YearMonth?,
        beregningsperiodeFom: YearMonth,
        beregnetBarnebidragPeriodeListe: List<ResultatPeriode>,
    ): List<ResultatPeriode> = if (virkningFraPeriode != null && virkningFraPeriode.isAfter(beregningsperiodeFom)) {
        beregnetBarnebidragPeriodeListe
            .map { periode ->
                if (
                    periode.periode.fom.isBefore(virkningFraPeriode) &&
                    (periode.periode.til == null || periode.periode.til!!.isAfter(virkningFraPeriode))
                ) {
                    // Juster fom til virkningFraPeriode ved overlapp
                    periode.copy(periode = periode.periode.copy(fom = virkningFraPeriode))
                } else {
                    periode
                }
            }
            .filterNot {
                it.periode.fom.isBefore(virkningFraPeriode) &&
                    (it.periode.til == null || !it.periode.til!!.isAfter(virkningFraPeriode))
            }
    } else {
        beregnetBarnebidragPeriodeListe
    }

    // Beregning av bidragsevne
    fun beregnBidragsevne(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug { "Beregning av bidragsevne - følgende request mottatt: ${commonObjectmapper.writeValueAsString(mottattGrunnlag)}" }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av bidragsevne: " + e.message)
        }

        // Kaller delberegninger
        val delberegningBidragsevneResultat = BeregnBidragsevneService.delberegningBidragsevne(mottattGrunnlag)

        return delberegningBidragsevneResultat
    }

    // Bygger endelig grunnlagsliste
    private fun byggEndeligResultatGrunnlagListe(
        grunnlagListe: List<GrunnlagDto>,
        resultatPeriodeListe: List<ResultatPeriode>,
        ekstraGrunnlag: List<GrunnlagDto> = emptyList(),
    ): MutableList<GrunnlagDto> {
        // Slår sammen grunnlag fra alle delberegninger
        val foreløpigResultatGrunnlagListe = grunnlagListe
            .distinctBy { it.referanse }
            .sortedBy { it.referanse }

        // Filtrerer bort grunnlag som ikke blir referert (dette vil skje f.eks. hvis barnet er selvforsørget og hvis barnet bor hos BP - da regnes
        // ikke alle delberegninger som relevante). Delberegninger for sjekk mot minimumsgrense for endring og fatting av vedtak står i en
        // særstilling ettersom de ikke refereres noe sted, men likevel skal være med i resultatgrunnlaget om de finnes.
        val endeligResultatGrunnlagListe = (
            filtrerResultatGrunnlag(
                foreløpigResultatGrunnlagListe = foreløpigResultatGrunnlagListe,
                refererteReferanserListe = resultatPeriodeListe.flatMap { it.grunnlagsreferanseListe },
            ) + ekstraGrunnlag
            ).toMutableList()

        // Mapper ut grunnlag for Person-objekter som er brukt
        endeligResultatGrunnlagListe.addAll(
            mapPersonobjektGrunnlag(
                resultatGrunnlagListe = endeligResultatGrunnlagListe,
                personobjektGrunnlagListe = grunnlagListe,
            ),
        )

        return endeligResultatGrunnlagListe
    }

    // Beregning av netto tilsynsutgift
    fun beregnNettoTilsynsutgift(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug {
            "Beregning av netto tilsynsutgift - følgende request mottatt: ${commonObjectmapper.writeValueAsString(mottattGrunnlag)}"
        }
        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av netto tilsynsutgift: " + e.message)
        }

        // Kaller delberegninger
        val delberegningNettoTilsynsutgiftResultat = BeregnNettoTilsynsutgiftService.delberegningNettoTilsynsutgift(mottattGrunnlag)
        return delberegningNettoTilsynsutgiftResultat
    }

    // Beregning av underholdskostnad
    fun beregnUnderholdskostnad(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug { "Beregning av underholdskostnad - følgende request mottatt: ${commonObjectmapper.writeValueAsString(mottattGrunnlag)}" }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av underholdskostnad: " + e.message)
        }

        val delberegningUnderholdskostnadResultat = BeregnUnderholdskostnadService.delberegningUnderholdskostnad(mottattGrunnlag)

        secureLogger.debug {
            "Beregning av underholdskostnad - følgende respons returnert: ${
                commonObjectmapper.writeValueAsString(
                    delberegningUnderholdskostnadResultat,
                )
            }"
        }
        return delberegningUnderholdskostnadResultat
    }

    // Beregning av først netto tilsynsutgift så underholdskostnad
    fun beregnNettoTilsynsutgiftOgUnderholdskostnad(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug {
            "Beregning av netto tilsynsutgift og så underholdskostnad - følgende request mottatt: ${
                commonObjectmapper.writeValueAsString(
                    mottattGrunnlag,
                )
            }"
        }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av underholdskostnad: " + e.message)
        }

        val bidragspliktigRef =
            mottattGrunnlag.grunnlagListe.bidragspliktig?.referanse
                ?: throw IllegalArgumentException("Finner ikke bidragspliktig i grunnlagsliste")
        val søknadsbarnRef = mottattGrunnlag.søknadsbarnReferanse
        val bidragsmottakerRef = mottattGrunnlag.grunnlagListe.finnBidragsmottakerForBarn(søknadsbarnRef)

        // Filtrerer vekk grunnlag som ikke gjelder det aktuelle barnet (ettersom denne metoden kalles direkte fra bidrag-behandling og for å
        // gjøre det likt som i totalberegningen - som går via BidragsberegningOrkestrator)
        val gyldigeGrunnlagForBarn = mottattGrunnlag.grunnlagListe.finnGyldigeGrunnlagForBarn(
            bmRef = bidragsmottakerRef,
            bpRef = bidragspliktigRef,
            barnRef = søknadsbarnRef,
        )
        var utvidetGrunnlag = mottattGrunnlag.copy(grunnlagListe = gyldigeGrunnlagForBarn)

        // Sjekker om søknadsbarnet fyller 18 år i beregningsperioden (gjøres her fordi dette er en metode som vil bli kalt direkte fra
        // bidrag-behandling)
        val utvidetGrunnlagJustert = justerTilPeriodeHvisBarnetBlir18ÅrIBeregningsperioden(utvidetGrunnlag)
        utvidetGrunnlag = utvidetGrunnlagJustert.beregnGrunnlag
        val åpenSluttperiode = utvidetGrunnlagJustert.åpenSluttperiode

        val delberegningNettoTilsynsutgiftResultat = BeregnNettoTilsynsutgiftService.delberegningNettoTilsynsutgift(
            mottattGrunnlag = utvidetGrunnlag,
            åpenSluttperiode = åpenSluttperiode,
        )

        val delberegningUnderholdskostnadResultat = BeregnUnderholdskostnadService.delberegningUnderholdskostnad(
            mottattGrunnlag = BeregnGrunnlag(
                periode = utvidetGrunnlag.periode,
                stønadstype = utvidetGrunnlag.stønadstype,
                søknadsbarnReferanse = utvidetGrunnlag.søknadsbarnReferanse,
                grunnlagListe = (utvidetGrunnlag.grunnlagListe + delberegningNettoTilsynsutgiftResultat).distinctBy { it.referanse },
            ),
            åpenSluttperiode = åpenSluttperiode,
        )

        return (delberegningNettoTilsynsutgiftResultat + delberegningUnderholdskostnadResultat).distinctBy { it.referanse }
    }

    // Beregning av BP's andel av underholdskostnad
    fun beregnBpAndelUnderholdskostnad(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug {
            "Beregning av BP's andel av underholdskostnad - følgende request mottatt: ${
                commonObjectmapper.writeValueAsString(
                    mottattGrunnlag,
                )
            }"
        }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av BP's andel av underholdskostnad: " + e.message)
        }

        // Kaller delberegninger
        val delberegningBpAndelUnderholdskostnadResultat =
            BeregnBpAndelUnderholdskostnadService.delberegningBpAndelUnderholdskostnad(mottattGrunnlag)

        return delberegningBpAndelUnderholdskostnadResultat
    }

    // Beregning av netto barnetillegg. Kan gjelde både BM og BP.
    fun beregnNettoBarnetillegg(mottattGrunnlag: BeregnGrunnlag, rolle: Grunnlagstype): List<GrunnlagDto> {
        secureLogger.debug { "Beregning av netto barnetillegg - følgende request mottatt: ${commonObjectmapper.writeValueAsString(mottattGrunnlag)}" }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av netto barnetillegg: " + e.message)
        }

        // Kaller delberegninger
        val delberegningNettoBarnetilleggResultat = BeregnNettoBarnetilleggService.delberegningNettoBarnetillegg(mottattGrunnlag, rolle)

        return delberegningNettoBarnetilleggResultat
    }

    // Beregning av samværsfradrag
    fun beregnSamværsfradrag(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug { "Beregning av samværsfradrag - følgende request mottatt: ${commonObjectmapper.writeValueAsString(mottattGrunnlag)}" }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av samværsfradrag: " + e.message)
        }

        // Kaller delberegninger
        val delberegningSamværsfradragResultat = BeregnSamværsfradragService.delberegningSamværsfradrag(mottattGrunnlag)

        return delberegningSamværsfradragResultat
    }

    // Beregning av endelig bidrag (sluttberegning)
    fun beregnEndeligBidrag(mottattGrunnlag: BeregnGrunnlag): BeregnEndeligBidragServiceRespons {
        secureLogger.debug {
            "Beregning av endelig bidrag (sluttberegning) - følgende request mottatt: ${
                commonObjectmapper.writeValueAsString(
                    mottattGrunnlag,
                )
            }"
        }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av endelig bidrag (sluttberegning): " + e.message)
        }

        // Kaller delberegninger
        val delberegningEndeligBidragResultat = BeregnEndeligBidragService.delberegningEndeligBidrag(mottattGrunnlag)

        return delberegningEndeligBidragResultat
    }

    // Beregning av om endelig bidrag (sluttberegning) er under eller over grense ("12%"-regelen) ifht løpende bidrag (per periode)
    fun beregnEndringSjekkGrensePeriode(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug {
            "Beregning av om endring i bidrag er over eller under grense (periode) - følgende request mottatt: " +
                commonObjectmapper.writeValueAsString(mottattGrunnlag)
        }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av endring sjekk grense (periode): " + e.message)
        }

        // Kaller delberegninger
        val delberegningEndringSjekkGrensePeriodeResultat =
            BeregnEndringSjekkGrensePeriodeService.delberegningEndringSjekkGrensePeriode(mottattGrunnlag)

        return delberegningEndringSjekkGrensePeriodeResultat
    }

    // Beregning av om endelig bidrag (sluttberegning) er under eller over grense ("12%"-regelen) ifht løpende bidrag (totalt)
    fun beregnEndringSjekkGrense(mottattGrunnlag: BeregnGrunnlag): List<GrunnlagDto> {
        secureLogger.debug {
            "Beregning av om endring i bidrag er over eller under grense - følgende request mottatt: ${
                commonObjectmapper.writeValueAsString(
                    mottattGrunnlag,
                )
            }"
        }

        // Kontroll av inputdata
        try {
            mottattGrunnlag.valider()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Ugyldig input ved beregning av endring sjekk grense: " + e.message)
        }

        // Kaller delberegninger
        val delberegningEndringSjekkGrenseResultat = BeregnEndringSjekkGrenseService.delberegningEndringSjekkGrense(mottattGrunnlag)

        return delberegningEndringSjekkGrenseResultat
    }

    // Sjekk mot minimumsgrense for endring ("12%-regelen")
    private fun sjekkMotMinimumsgrenseForEndring(
        mottattGrunnlag: BeregnGrunnlag,
        utvidetGrunnlagJustert: BeregnGrunnlagJustert,
        delberegningEndeligBidragResultat: BeregnEndeligBidragServiceRespons,
        åpenSluttperiode: Boolean,
    ): SjekkMotMinimumsgrenseForEndringResultat {
        val er18ÅrsBidrag = mottattGrunnlag.stønadstype == Stønadstype.BIDRAG18AAR

        // Filtrerer ut beløpshistorikk. Hvis det er 18-års-bidrag benyttes egen beløpshistorikk.
        val beløpshistorikkGrunnlag = if (er18ÅrsBidrag) {
            filtrerBeløpshistorikk18ÅrGrunnlag(mottattGrunnlag)
        } else {
            filtrerBeløpshistorikkGrunnlag(mottattGrunnlag)
        }

        // Kaller delberegning for indeksregulering av privat avtale
        val delberegningIndeksreguleringPrivatAvtalePeriodeResultat = utførDelberegningPrivatAvtalePeriode(
            beregnGrunnlag = mottattGrunnlag,
            beregningsperiode = mottattGrunnlag.periode,
        )

        // Kaller delberegning for å sjekke om endring i bidrag er over grense (pr periode)
        var grunnlagTilEndringSjekkGrense = utvidetGrunnlagJustert.beregnGrunnlag.copy(
            grunnlagListe =
            delberegningIndeksreguleringPrivatAvtalePeriodeResultat + beløpshistorikkGrunnlag + delberegningEndeligBidragResultat.grunnlagListe,
        )
        val delberegningEndringSjekkGrensePeriodeResultat =
            BeregnEndringSjekkGrensePeriodeService.delberegningEndringSjekkGrensePeriode(
                mottattGrunnlag = grunnlagTilEndringSjekkGrense,
                åpenSluttperiode = åpenSluttperiode,
            )

        // Kaller delberegning for å sjekke om endring i bidrag er over grense (totalt)
        grunnlagTilEndringSjekkGrense = grunnlagTilEndringSjekkGrense.copy(
            grunnlagListe = (grunnlagTilEndringSjekkGrense.grunnlagListe + delberegningEndringSjekkGrensePeriodeResultat),
        )
        val delberegningEndringSjekkGrenseResultat = BeregnEndringSjekkGrenseService.delberegningEndringSjekkGrense(
            mottattGrunnlag = grunnlagTilEndringSjekkGrense,
            åpenSluttperiode = åpenSluttperiode,
        )

        val beregnetBidragErOverMinimumsgrenseForEndring = erOverMinimumsgrenseForEndring(delberegningEndringSjekkGrenseResultat)
        val grunnlagstype = if (er18ÅrsBidrag) Grunnlagstype.BELØPSHISTORIKK_BIDRAG_18_ÅR else Grunnlagstype.BELØPSHISTORIKK_BIDRAG

        val resultatPeriodeListe = lagResultatPerioder(
            delberegningEndeligBidragPeriodeResultat = delberegningEndeligBidragResultat.grunnlagListe,
            beregnetBidragErOverMinimumsgrenseForEndring = beregnetBidragErOverMinimumsgrenseForEndring,
            beløpshistorikkGrunnlag = beløpshistorikkGrunnlag,
            beløpshistorikkGrunnlagstype = grunnlagstype,
            delberegningEndringSjekkGrensePeriodeResultat = delberegningEndringSjekkGrensePeriodeResultat,
            delberegningIndeksreguleringPrivatAvtalePeriodeResultat = delberegningIndeksreguleringPrivatAvtalePeriodeResultat,
        )

        return SjekkMotMinimumsgrenseForEndringResultat(
            resultatPeriodeListe = resultatPeriodeListe,
            delberegningEndringSjekkGrensePeriodeResultat = delberegningEndringSjekkGrensePeriodeResultat,
            delberegningEndringSjekkGrenseResultat = delberegningEndringSjekkGrenseResultat,
            delberegningIndeksreguleringPrivatAvtaleResultat = delberegningIndeksreguleringPrivatAvtalePeriodeResultat,
        )
    }

    // Sjekk mot minimumsgrense for endring ("12%-regelen")
    private fun BeregnGrunnlagJustert.sjekkMotMinimumsgrenseForEndringV2(): SjekkMotMinimumsgrenseForEndringResultat {
        val er18ÅrsBidrag = beregnGrunnlag.stønadstype == Stønadstype.BIDRAG18AAR
        val grunnlagstype = if (er18ÅrsBidrag) Grunnlagstype.BELØPSHISTORIKK_BIDRAG_18_ÅR else Grunnlagstype.BELØPSHISTORIKK_BIDRAG

        // Filtrerer ut beløpshistorikk. Hvis det er 18-års-bidrag benyttes egen beløpshistorikk.
        val beløpshistorikkGrunnlag = beregnGrunnlag.filtrerBeløpshistorikkGrunnlag(er18ÅrsBidrag)

        var utvidetGrunnlag = utvidMedNyeGrunnlag(beløpshistorikkGrunnlag)

        // Sjekker om sluttberegningen har åpen sluttperiode og bruker den videre (se spesiallogikk i
        // BeregnEndeligBidragServiceV2.sluttberegningBarnebidrag)
        val erÅpenSluttperiodeFraSluttberegning = utvidetGrunnlag.beregnGrunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)
            .maxByOrNull { it.innhold.periode.fom }
            ?.innhold?.periode?.til == null

        // Kaller delberegning for å sjekke om endring i bidrag er over grense (pr periode)
        val delberegningEndringSjekkGrensePeriodeResultat =
            BeregnEndringSjekkGrensePeriodeService.delberegningEndringSjekkGrensePeriodeV2(
                mottattGrunnlag = utvidetGrunnlag.beregnGrunnlag,
                åpenSluttperiode = erÅpenSluttperiodeFraSluttberegning,
                virkningFraPeriode = utvidetGrunnlag.virkningFraPeriode,
            )
        utvidetGrunnlag = utvidetGrunnlag.utvidMedNyeGrunnlag(delberegningEndringSjekkGrensePeriodeResultat)

        // Kaller delberegning for å sjekke om endring i bidrag er over grense (totalt)
        val delberegningEndringSjekkGrenseResultat = BeregnEndringSjekkGrenseService.delberegningEndringSjekkGrense(
            mottattGrunnlag = utvidetGrunnlag.beregnGrunnlag,
            åpenSluttperiode = erÅpenSluttperiodeFraSluttberegning,
        )

        // Henter resultat av delberegning for indeksregulering av privat avtale
        val delberegningIndeksreguleringPrivatAvtalePeriodeResultat = beregnGrunnlag.hentIndeksreguleringPrivatAvtaleGrunnlag()

        val resultatPeriodeListe = lagResultatPerioderV2(
            sluttberegningPeriodeResultat = beregnGrunnlag.grunnlagListe,
            beregnetBidragErOverMinimumsgrenseForEndring = erOverMinimumsgrenseForEndring(delberegningEndringSjekkGrenseResultat),
            beløpshistorikkGrunnlag = beløpshistorikkGrunnlag,
            beløpshistorikkGrunnlagstype = grunnlagstype,
            delberegningEndringSjekkGrensePeriodeResultat = delberegningEndringSjekkGrensePeriodeResultat,
            delberegningIndeksreguleringPrivatAvtalePeriodeResultat = delberegningIndeksreguleringPrivatAvtalePeriodeResultat,
            delberegningEndringSjekkGrenseResultat = delberegningEndringSjekkGrenseResultat,
        )

        return SjekkMotMinimumsgrenseForEndringResultat(
            resultatPeriodeListe = resultatPeriodeListe,
            delberegningEndringSjekkGrensePeriodeResultat = delberegningEndringSjekkGrensePeriodeResultat,
            delberegningEndringSjekkGrenseResultat = delberegningEndringSjekkGrenseResultat,
            delberegningIndeksreguleringPrivatAvtaleResultat = delberegningIndeksreguleringPrivatAvtalePeriodeResultat,
        )
    }

    // Sjekk mot minimumsgrense for endring ("12%-regelen") skal bare utføres hvis det ikke er eget tiltak og det ikke er klage
    private fun BeregnGrunnlag.skalSjekkeMotMinimumsgrenseForEndring(): Boolean = grunnlagListe
        .filtrerOgKonverterBasertPåEgenReferanse<SøknadGrunnlag>(Grunnlagstype.SØKNAD)
        .map { !it.innhold.egetTiltak && it.innhold.klageMottattDato == null }
        .firstOrNull() ?: true

    // Skal sjekke mot minimumsgrense for endring ("12%-regelen") hvis egetTiltak er false og det ikke er klageberegning.
    // Gjør ikke 12%-sjekk hvis det finnes perioder hvor det ikke er full evne og ufullstendige grunnlag.
    private fun BeregnGrunnlagJustert.minimumsgrenseForEndring(
        ikkeFullBidragsevneOgUfullstendigeGrunnlag: Boolean,
    ): SjekkMotMinimumsgrenseForEndringResultat = if (beregnGrunnlag.skalSjekkeMotMinimumsgrenseForEndring() && !ikkeFullBidragsevneOgUfullstendigeGrunnlag) {
        sjekkMotMinimumsgrenseForEndringV2()
    } else {
        SjekkMotMinimumsgrenseForEndringResultat(
            resultatPeriodeListe = lagResultatPerioderV2(beregnGrunnlag.grunnlagListe),
            delberegningEndringSjekkGrensePeriodeResultat = emptyList(),
            delberegningEndringSjekkGrenseResultat = emptyList(),
            delberegningIndeksreguleringPrivatAvtaleResultat = emptyList(),
        )
    }

    private fun BeregnGrunnlag.filtrerBeløpshistorikkGrunnlag(er18ÅrsBidrag: Boolean): List<GrunnlagDto> {
        val grunnlagstype = if (er18ÅrsBidrag) Grunnlagstype.BELØPSHISTORIKK_BIDRAG_18_ÅR else Grunnlagstype.BELØPSHISTORIKK_BIDRAG
        return grunnlagListe.filter { it.type == grunnlagstype }
    }

    private fun BeregnGrunnlag.hentIndeksreguleringPrivatAvtaleGrunnlag(): List<GrunnlagDto> = grunnlagListe
        .filter {
            it.type == Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE &&
                it.gjelderBarnReferanse == søknadsbarnReferanse
        }

    private fun filtrerBeløpshistorikkGrunnlag(beregnGrunnlag: BeregnGrunnlag): List<GrunnlagDto> = beregnGrunnlag.grunnlagListe.filter { it.type == Grunnlagstype.BELØPSHISTORIKK_BIDRAG }

    private fun filtrerBeløpshistorikk18ÅrGrunnlag(beregnGrunnlag: BeregnGrunnlag): List<GrunnlagDto> = beregnGrunnlag.grunnlagListe.filter { it.type == Grunnlagstype.BELØPSHISTORIKK_BIDRAG_18_ÅR }

    private fun utførDelberegningPrivatAvtalePeriode(beregnGrunnlag: BeregnGrunnlag, beregningsperiode: ÅrMånedsperiode): List<GrunnlagDto> = if (beregnGrunnlag.grunnlagListe
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG)
            .none { it.gjelderBarnReferanse == beregnGrunnlag.søknadsbarnReferanse }
    ) {
        emptyList()
    } else {
        BeregnIndeksreguleringPrivatAvtaleService.delberegningIndeksreguleringPrivatAvtale(
            grunnlag = beregnGrunnlag,
            beregningsperiode = beregningsperiode,
        )
    }

    // Standardlogikk for å lage resultatperioder
    private fun lagResultatPerioder(delberegningEndeligBidragResultat: List<GrunnlagDto>): List<ResultatPeriode> = delberegningEndeligBidragResultat
        .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidrag>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)
        .map {
            ResultatPeriode(
                periode = it.innhold.periode,
                resultat = ResultatBeregning(
                    beløp = it.innhold.resultatBeløp,
                ),
                grunnlagsreferanseListe = listOf(it.referanse),
            )
        }

    // Standardlogikk for å lage resultatperioder
    private fun lagResultatPerioderV2(grunnlagListe: List<GrunnlagDto>): List<ResultatPeriode> = grunnlagListe
        .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)
        .map {
            ResultatPeriode(
                periode = it.innhold.periode,
                resultat = ResultatBeregning(
                    beløp = it.innhold.resultatBeløp,
                ),
                grunnlagsreferanseListe = listOf(it.referanse),
            )
        }

    // Lager resultatperioder basert på beløpshistorikk hvis beregnet bidrag ikke er over minimumsgrense for endring
    private fun lagResultatPerioder(
        delberegningEndeligBidragPeriodeResultat: List<GrunnlagDto>,
        beregnetBidragErOverMinimumsgrenseForEndring: Boolean,
        beløpshistorikkGrunnlag: List<GrunnlagDto>,
        beløpshistorikkGrunnlagstype: Grunnlagstype,
        delberegningEndringSjekkGrensePeriodeResultat: List<GrunnlagDto>,
        delberegningIndeksreguleringPrivatAvtalePeriodeResultat: List<GrunnlagDto>,
    ): List<ResultatPeriode> {
        // Henter beløpshistorikk (det finnes kun en forekomst, som dekker hele perioden)
        val beløpshistorikkPeriodeGrunnlag = beløpshistorikkGrunnlag
            .filtrerOgKonverterBasertPåEgenReferanse<BeløpshistorikkGrunnlag>(beløpshistorikkGrunnlagstype)
            .map {
                BeløpshistorikkPeriodeGrunnlag(
                    referanse = it.referanse,
                    beløpshistorikkPeriode = it.innhold,
                )
            }
            .firstOrNull()

        // Henter resultat av sluttberegning
        val sluttberegningPeriodeGrunnlagListe = delberegningEndeligBidragPeriodeResultat
            .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidrag>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)
            .map {
                SluttberegningPeriodeGrunnlag(
                    referanse = it.referanse,
                    sluttberegningPeriode = it.innhold,
                )
            }

        // Henter resultat av delberegning endring-sjekk-grense-periode
        val delberegningEndringSjekkGrensePeriodeGrunnlagListe = delberegningEndringSjekkGrensePeriodeResultat
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE)
            .map {
                EndringSjekkGrensePeriodeDelberegningPeriodeGrunnlag(
                    referanse = it.referanse,
                    endringSjekkGrensePeriodePeriode = it.innhold,
                    referanseListe = it.grunnlag.grunnlagsreferanseListe,
                )
            }

        // Henter resultat av delberegning privat-avtale-periode
        val delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe = delberegningIndeksreguleringPrivatAvtalePeriodeResultat
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningPrivatAvtale>(Grunnlagstype.DELBEREGNING_PRIVAT_AVTALE)
            .firstOrNull()?.let { dpa ->
                dpa.innhold.perioder.map {
                    PrivatAvtaleIndeksregulertPeriodeGrunnlag(
                        referanse = dpa.referanse,
                        privatAvtaleIndeksregulertPeriode = it,
                    )
                }
            } ?: emptyList()

        // Bruker grunnlagslisten fra delberegning endring-sjekk-grense-periode som utgangspunkt for å lage resultatperioder
        return delberegningEndringSjekkGrensePeriodeGrunnlagListe
            .map {
                ResultatPeriode(
                    periode = it.endringSjekkGrensePeriodePeriode.periode,
                    resultat = ResultatBeregning(
                        beløp = hentEndeligBeløp(
                            beregnetBidragErOverMinimumsgrenseForEndring = beregnetBidragErOverMinimumsgrenseForEndring,
                            referanseListe = it.referanseListe,
                            periode = it.endringSjekkGrensePeriodePeriode.periode,
                            sluttberegningPeriodeGrunnlagListe = sluttberegningPeriodeGrunnlagListe,
                            beløpshistorikkPeriodeGrunnlag = beløpshistorikkPeriodeGrunnlag,
                            delberegningIndeksregPrivatAvtalePeriodeGrunnlagListe = delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe,
                        ),
                    ),
                    grunnlagsreferanseListe = lagReferanseliste(
                        referanseListe = it.referanseListe,
                        periode = it.endringSjekkGrensePeriodePeriode.periode,
                        beløpshistorikkPeriodeGrunnlag = beløpshistorikkPeriodeGrunnlag,
                        delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe = delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe,
                    ),
                )
            }
    }

    // Hvis beregnet bidrag er over minimumsgrense for endring skal beløp hentes fra sluttberegning (matcher på referanse); hvis ikke skal beløp
    // hentes fra privat avtale (matcher på referanse) eller beløpshistorikk (matcher på periode)
    private fun hentEndeligBeløp(
        beregnetBidragErOverMinimumsgrenseForEndring: Boolean,
        referanseListe: List<String>,
        periode: ÅrMånedsperiode,
        sluttberegningPeriodeGrunnlagListe: List<SluttberegningPeriodeGrunnlag>,
        beløpshistorikkPeriodeGrunnlag: BeløpshistorikkPeriodeGrunnlag?,
        delberegningIndeksregPrivatAvtalePeriodeGrunnlagListe: List<PrivatAvtaleIndeksregulertPeriodeGrunnlag>,
    ): BigDecimal? {
        val privatAvtaleBeløp = delberegningIndeksregPrivatAvtalePeriodeGrunnlagListe
            .filter {
                (periode.til == null || it.privatAvtaleIndeksregulertPeriode.periode.fom < periode.til) &&
                    (it.privatAvtaleIndeksregulertPeriode.periode.til == null || it.privatAvtaleIndeksregulertPeriode.periode.til!! > periode.fom)
            }
            .map { it.privatAvtaleIndeksregulertPeriode.beløp }
            .firstOrNull()
        val beløpshistorikkBeløp = beløpshistorikkPeriodeGrunnlag?.beløpshistorikkPeriode?.beløpshistorikk
            ?.filter { (periode.til == null || it.periode.fom < periode.til) && (it.periode.til == null || it.periode.til!! > periode.fom) }
            ?.map { it.beløp }
            ?.firstOrNull()
        return if (beregnetBidragErOverMinimumsgrenseForEndring) {
            sluttberegningPeriodeGrunnlagListe
                .filter { it.referanse in referanseListe }
                .map { it.sluttberegningPeriode.resultatBeløp }
                .firstOrNull()
        } else {
            beløpshistorikkBeløp ?: privatAvtaleBeløp
        }
    }

    // Fjerner referanser som skal legges ut i resultatperioden i enkelte tilfeller (privat avtale og sjablon)
    private fun lagReferanseliste(
        referanseListe: List<String>,
        periode: ÅrMånedsperiode,
        beløpshistorikkPeriodeGrunnlag: BeløpshistorikkPeriodeGrunnlag?,
        delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe: List<PrivatAvtaleIndeksregulertPeriodeGrunnlag>,
    ): List<String> {
        val privatAvtaleReferanse = delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe
            .filter { it.referanse in referanseListe }
            .map { it.referanse }
            .firstOrNull() ?: ""
        val sjablonReferanse = referanseListe
            .firstOrNull { it.startsWith("sjablon") || it.startsWith("Sjablon") || it.startsWith("SJABLON") } ?: ""
        val beløpshistorikkReferanse = beløpshistorikkPeriodeGrunnlag?.beløpshistorikkPeriode?.beløpshistorikk
            ?.filter { (periode.til == null || it.periode.fom < periode.til) && (it.periode.til == null || it.periode.til!! > periode.fom) }
            ?.map { beløpshistorikkPeriodeGrunnlag.referanse }
            ?.firstOrNull() ?: ""
        // beløpshistorikkReferanse trumfer privatAvtaleReferanse hvis begge finnes
        return if (privatAvtaleReferanse.isNotEmpty() && beløpshistorikkReferanse.isNotEmpty()) {
            referanseListe.minus(privatAvtaleReferanse).minus(sjablonReferanse)
        } else {
            referanseListe.minus(sjablonReferanse)
        }
    }

    // Rekursiv funksjon som traverserer gjennom alle grunnlag fra toppnivået og nedover og filtrerer bort alle grunnlag som ikke blir referert
    private fun filtrerResultatGrunnlag(
        foreløpigResultatGrunnlagListe: List<GrunnlagDto>,
        refererteReferanserListe: List<String>,
        referanserAlleredeLagtTil: MutableSet<String> = mutableSetOf(),
    ): List<GrunnlagDto> {
        // Stopper hvis det ikke finnes flere refererte referanser
        if (refererteReferanserListe.isEmpty()) {
            return emptyList()
        }

        // Filtrer ut grunnlag som er referert og som ikke allerede er lagt til
        val endeligResultatGrunnlagListe = foreløpigResultatGrunnlagListe
            .filter { it.referanse in refererteReferanserListe && it.referanse !in referanserAlleredeLagtTil }

        // Henter ut referanser til neste nivå
        val nesteNivåReferanseListe = endeligResultatGrunnlagListe.flatMap { it.grunnlagsreferanseListe }

        // Legger til referanser som allerede er lagt til
        referanserAlleredeLagtTil.addAll(endeligResultatGrunnlagListe.map { it.referanse })

        // Gjør rekursivt kall og returnerer det endelige resultatet til slutt
        return endeligResultatGrunnlagListe + filtrerResultatGrunnlag(
            foreløpigResultatGrunnlagListe = foreløpigResultatGrunnlagListe,
            refererteReferanserListe = nesteNivåReferanseListe,
            referanserAlleredeLagtTil = referanserAlleredeLagtTil,
        )
    }

    fun beregnMånedsbeløpFaktiskUtgift(faktiskUtgift: BigDecimal, kostpenger: BigDecimal = BigDecimal.ZERO): BigDecimal = NettoTilsynsutgiftMapper.beregnMånedsbeløpFaktiskUtgift(faktiskUtgift, kostpenger).avrundetMedToDesimaler

    fun beregnMånedsbeløpTilleggsstønad(tilleggsstønad: BigDecimal, beløpstype: InntektBeløpstype): BigDecimal = NettoTilsynsutgiftMapper.beregnMånedsbeløpTilleggsstønad(tilleggsstønad, beløpstype).avrundetMedToDesimaler

    // Sjekker om søknadsbarnet er del av opprinnelig behandling eller om det er del av en revurderingssøknad som er utløst pga. FF og innhenting
    // av nye grunnlag for løpende bidrag.
    private fun BeregnGrunnlagJustert.sjekkOmBarnetErDelAvOpprinneligBehandling(): BeregnGrunnlagJustert {
        val søknadsbarn = beregnGrunnlag.grunnlagListe
            .hentPersonMedReferanse(beregnGrunnlag.søknadsbarnReferanse)
            ?.personObjekt

        return this.copy(
            erDelAvOpprinneligBehandling = søknadsbarn?.delAvOpprinneligBehandling ?: true,
        )
    }

    // Finner virkning fra periode. Vil bli en egen bruddperiode hvis den avviker fra beregningsperiode fra.
    private fun BeregnGrunnlagJustert.leggTilVirkningFraPeriode(
        virkningstidspunkt: InnholdMedReferanse<VirkningstidspunktGrunnlag>?,
    ): BeregnGrunnlagJustert {
        val virkningFraPeriode = virkningstidspunkt?.innhold?.virkningstidspunkt
            ?.let(YearMonth::from)
            ?: this.beregnGrunnlag.periode.fom

        return this.copy(
            virkningFraPeriode = virkningFraPeriode,
        )
    }

    private fun List<GrunnlagDto>.finnBidragsmottakerForBarn(søknadsbarnreferanse: String): String = this
        .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
        .firstOrNull { it.referanse == søknadsbarnreferanse }
        ?.innhold?.bidragsmottaker
        ?: throw IllegalArgumentException("Fant ikke bidragsmottaker for barn med referanse $søknadsbarnreferanse")

    // Utfører alle standard delberegninger for et søknadsbarn
    private fun BeregnGrunnlagJustert.utførAlleStandardDelberegningerForSøknadsbarn(): BeregnGrunnlagJustert {
        var grunnlag = this

        // Bidragsevne
        grunnlag = grunnlag.utvidMedNyeGrunnlag(
            BeregnBidragsevneService.delberegningBidragsevne(
                mottattGrunnlag = grunnlag.beregnGrunnlag,
                åpenSluttperiode = grunnlag.åpenSluttperiode,
                virkningFraPeriode = grunnlag.virkningFraPeriode,
            ),
        )

        // Netto tilsynsutgift
        grunnlag = grunnlag.utvidMedNyeGrunnlag(
            BeregnNettoTilsynsutgiftService.delberegningNettoTilsynsutgift(
                mottattGrunnlag = grunnlag.beregnGrunnlag,
                åpenSluttperiode = grunnlag.åpenSluttperiode,
                virkningFraPeriode = grunnlag.virkningFraPeriode,
            ),
        )

        // Underholdskostnad
        grunnlag = grunnlag.utvidMedNyeGrunnlag(
            BeregnUnderholdskostnadService.delberegningUnderholdskostnad(
                mottattGrunnlag = grunnlag.beregnGrunnlag,
                åpenSluttperiode = grunnlag.åpenSluttperiode,
                virkningFraPeriode = grunnlag.virkningFraPeriode,
            ),
        )

        // BP andel underholdskostnad
        grunnlag = grunnlag.utvidMedNyeGrunnlag(
            BeregnBpAndelUnderholdskostnadService.delberegningBpAndelUnderholdskostnad(
                mottattGrunnlag = grunnlag.beregnGrunnlag,
                åpenSluttperiode = grunnlag.åpenSluttperiode,
                virkningFraPeriode = grunnlag.virkningFraPeriode,
            ),
        )

        // Samværsfradrag
        grunnlag = grunnlag.utvidMedNyeGrunnlag(
            BeregnSamværsfradragService.delberegningSamværsfradrag(
                mottattGrunnlag = grunnlag.beregnGrunnlag,
                åpenSluttperiode = grunnlag.åpenSluttperiode,
                virkningFraPeriode = grunnlag.virkningFraPeriode,
                erLøpendeBidrag = false,
                erPrivatAvtale = false,
            ),
        )

        // Barnetillegg BP og BM
        grunnlag = grunnlag.utførBarnetilleggDelberegning(Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
        grunnlag = grunnlag.utførBarnetilleggDelberegning(Grunnlagstype.PERSON_BIDRAGSMOTTAKER)

        return grunnlag
    }

    // Utfører barnetillegg-delberegning (netto barnetillegg) for en gitt rolle hvis barnetillegg eksisterer
    private fun BeregnGrunnlagJustert.utførBarnetilleggDelberegning(rolle: Grunnlagstype): BeregnGrunnlagJustert {
        val rolleReferanse = when (rolle) {
            Grunnlagstype.PERSON_BIDRAGSPLIKTIG -> beregnGrunnlag.grunnlagListe.bidragspliktig?.referanse
            Grunnlagstype.PERSON_BIDRAGSMOTTAKER -> beregnGrunnlag.grunnlagListe.bidragsmottaker?.referanse
            else -> null
        } ?: return this

        if (!barnetilleggEksisterer(beregnGrunnlag, rolleReferanse)) {
            return this
        }

        // Netto barnetillegg
        val oppdatertGrunnlag = this.utvidMedNyeGrunnlag(
            BeregnNettoBarnetilleggService.delberegningNettoBarnetillegg(
                mottattGrunnlag = beregnGrunnlag,
                rolle = rolle,
                åpenSluttperiode = true,
                virkningFraPeriode = virkningFraPeriode,
            ),
        )

        return oppdatertGrunnlag
    }

    companion object {

        // Hvis beregnet bidrag er over minimumsgrense for endring skal beløp hentes fra sluttberegning (matcher på referanse); hvis ikke skal beløp
        // hentes fra privat avtale (matcher på referanse) eller beløpshistorikk (matcher på periode)
        fun hentEndeligBeløpV2(
            beregnetBidragErOverMinimumsgrenseForEndring: Boolean,
            referanseListe: List<String>,
            periode: ÅrMånedsperiode,
            sluttberegningPeriodeGrunnlagListe: List<SluttberegningPeriodeGrunnlagV2>,
            beløpshistorikkPeriodeGrunnlag: BeløpshistorikkPeriodeGrunnlag?,
            delberegningIndeksregPrivatAvtalePeriodeGrunnlagListe: List<PrivatAvtaleIndeksregulertPeriodeGrunnlagV2>,
            delberegningEndringSjekkGrenseGrunnlagListe: List<EndringSjekkGrenseDelberegningPeriodeGrunnlag>,
        ): BigDecimal? {
            val privatAvtaleBeløp = delberegningIndeksregPrivatAvtalePeriodeGrunnlagListe
                .filter {
                    (periode.til == null || it.privatAvtaleIndeksregulertPeriode.periode.fom < periode.til) &&
                        (it.privatAvtaleIndeksregulertPeriode.periode.til == null || it.privatAvtaleIndeksregulertPeriode.periode.til!! > periode.fom)
                }
                .map { it.privatAvtaleIndeksregulertPeriode.indeksregulertBeløp }
                .firstOrNull()
            val beløpshistorikkBeløp = beløpshistorikkPeriodeGrunnlag?.beløpshistorikkPeriode?.beløpshistorikk
                ?.filter { (periode.til == null || it.periode.fom < periode.til) && (it.periode.til == null || it.periode.til!! > periode.fom) }
                ?.map { it.beløp }
                ?.firstOrNull()
            // Hvis ny 12%-regel kan det være flere perioder. Kan derfor ikke bruke beregnetBidragErOverMinimumsgrenseForEndring direkte
            val endeligBeregnetBidragErOverMinimumsgrenseForEndring =
                if (BarnebidragUnleashFeatures.BIDRAG_BEREGNING_FRA_FØRSTE_PERIODE_OVER_TOLV_PROSENT.isEnabled) {
                    delberegningEndringSjekkGrenseGrunnlagListe
                        .filter {
                            (periode.til == null || it.endringSjekkGrensePeriode.periode.fom < periode.til) &&
                                (
                                    it.endringSjekkGrensePeriode.periode.til == null ||
                                        it.endringSjekkGrensePeriode.periode.til!! > periode.fom
                                    )
                        }
                        .map { it.endringSjekkGrensePeriode.endringErOverGrense }
                        .first()
                } else {
                    beregnetBidragErOverMinimumsgrenseForEndring
                }
            return if (endeligBeregnetBidragErOverMinimumsgrenseForEndring) {
                sluttberegningPeriodeGrunnlagListe
                    .filter { it.referanse in referanseListe }
                    .map { it.sluttberegningPeriode.resultatBeløp }
                    .firstOrNull()
            } else {
                beløpshistorikkBeløp ?: privatAvtaleBeløp
            }
        }

        // Fjerner referanser som skal legges ut i resultatperioden i enkelte tilfeller (privat avtale og sjablon)
        fun lagReferanselisteV2(
            referanseListe: List<String>,
            periode: ÅrMånedsperiode,
            beløpshistorikkPeriodeGrunnlag: BeløpshistorikkPeriodeGrunnlag?,
            delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe: List<PrivatAvtaleIndeksregulertPeriodeGrunnlagV2>,
        ): List<String> {
            val privatAvtaleReferanse = delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe
                .filter { it.referanse in referanseListe }
                .map { it.referanse }
                .firstOrNull() ?: ""
            val sjablonReferanse = referanseListe
                .firstOrNull { it.startsWith("sjablon") || it.startsWith("Sjablon") || it.startsWith("SJABLON") } ?: ""
            val beløpshistorikkReferanse = beløpshistorikkPeriodeGrunnlag?.beløpshistorikkPeriode?.beløpshistorikk
                ?.filter { (periode.til == null || it.periode.fom < periode.til) && (it.periode.til == null || it.periode.til!! > periode.fom) }
                ?.map { beløpshistorikkPeriodeGrunnlag.referanse }
                ?.firstOrNull() ?: ""
            // beløpshistorikkReferanse trumfer privatAvtaleReferanse hvis begge finnes
            return if (privatAvtaleReferanse.isNotEmpty() && beløpshistorikkReferanse.isNotEmpty()) {
                referanseListe.minus(privatAvtaleReferanse).minus(sjablonReferanse)
            } else {
                referanseListe.minus(sjablonReferanse)
            }
        }

        // Lager resultatperioder basert på beløpshistorikk hvis beregnet bidrag ikke er over minimumsgrense for endring
        fun lagResultatPerioderV2(
            sluttberegningPeriodeResultat: List<GrunnlagDto>,
            beregnetBidragErOverMinimumsgrenseForEndring: Boolean,
            beløpshistorikkGrunnlag: List<GrunnlagDto>,
            beløpshistorikkGrunnlagstype: Grunnlagstype,
            delberegningEndringSjekkGrensePeriodeResultat: List<GrunnlagDto>,
            delberegningIndeksreguleringPrivatAvtalePeriodeResultat: List<GrunnlagDto>,
            delberegningEndringSjekkGrenseResultat: List<GrunnlagDto>,
        ): List<ResultatPeriode> {
            // Henter beløpshistorikk (det finnes kun en forekomst, som dekker hele perioden)
            val beløpshistorikkPeriodeGrunnlag = beløpshistorikkGrunnlag
                .filtrerOgKonverterBasertPåEgenReferanse<BeløpshistorikkGrunnlag>(beløpshistorikkGrunnlagstype)
                .map {
                    BeløpshistorikkPeriodeGrunnlag(
                        referanse = it.referanse,
                        beløpshistorikkPeriode = it.innhold,
                    )
                }
                .firstOrNull()

            // Henter resultat av sluttberegning
            val sluttberegningPeriodeGrunnlagListe = sluttberegningPeriodeResultat
                .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)
                .map {
                    SluttberegningPeriodeGrunnlagV2(
                        referanse = it.referanse,
                        sluttberegningPeriode = it.innhold,
                    )
                }

            // Henter resultat av delberegning endring-sjekk-grense-periode
            val delberegningEndringSjekkGrensePeriodeGrunnlagListe = delberegningEndringSjekkGrensePeriodeResultat
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrensePeriode>(
                    Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
                )
                .map {
                    EndringSjekkGrensePeriodeDelberegningPeriodeGrunnlag(
                        referanse = it.referanse,
                        endringSjekkGrensePeriodePeriode = it.innhold,
                        referanseListe = it.grunnlag.grunnlagsreferanseListe,
                    )
                }

            // Henter resultat av delberegning endring-sjekk-grense
            // Trengs bare hvis ny 12%-regel
            val delberegningEndringSjekkGrenseGrunnlagListe = delberegningEndringSjekkGrenseResultat
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningEndringSjekkGrense>(
                    Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE,
                )
                .map {
                    EndringSjekkGrenseDelberegningPeriodeGrunnlag(
                        referanse = it.referanse,
                        endringSjekkGrensePeriode = it.innhold,
                        referanseListe = it.grunnlag.grunnlagsreferanseListe,
                    )
                }

            // Henter resultat av delberegning privat-avtale-periode
            val delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe = delberegningIndeksreguleringPrivatAvtalePeriodeResultat
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningIndeksreguleringPrivatAvtale>(
                    Grunnlagstype.DELBEREGNING_INDEKSREGULERING_PRIVAT_AVTALE,
                )
                .map {
                    PrivatAvtaleIndeksregulertPeriodeGrunnlagV2(
                        referanse = it.referanse,
                        privatAvtaleIndeksregulertPeriode = it.innhold,
                    )
                }

            // Bruker grunnlagslisten fra delberegning endring-sjekk-grense-periode som utgangspunkt for å lage resultatperioder
            return delberegningEndringSjekkGrensePeriodeGrunnlagListe
                .map {
                    ResultatPeriode(
                        periode = it.endringSjekkGrensePeriodePeriode.periode,
                        resultat = ResultatBeregning(
                            beløp = hentEndeligBeløpV2(
                                beregnetBidragErOverMinimumsgrenseForEndring = beregnetBidragErOverMinimumsgrenseForEndring,
                                referanseListe = it.referanseListe,
                                periode = it.endringSjekkGrensePeriodePeriode.periode,
                                sluttberegningPeriodeGrunnlagListe = sluttberegningPeriodeGrunnlagListe,
                                beløpshistorikkPeriodeGrunnlag = beløpshistorikkPeriodeGrunnlag,
                                delberegningIndeksregPrivatAvtalePeriodeGrunnlagListe = delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe,
                                delberegningEndringSjekkGrenseGrunnlagListe = delberegningEndringSjekkGrenseGrunnlagListe,
                            ),
                        ),
                        grunnlagsreferanseListe = lagReferanselisteV2(
                            referanseListe = it.referanseListe,
                            periode = it.endringSjekkGrensePeriodePeriode.periode,
                            beløpshistorikkPeriodeGrunnlag = beløpshistorikkPeriodeGrunnlag,
                            delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe =
                            delberegningIndeksreguleringPrivatAvtalePeriodeGrunnlagListe,
                        ),
                    )
                }
        }
    }

    data class SjekkMotMinimumsgrenseForEndringResultat(
        val resultatPeriodeListe: List<ResultatPeriode>,
        val delberegningEndringSjekkGrensePeriodeResultat: List<GrunnlagDto>,
        val delberegningEndringSjekkGrenseResultat: List<GrunnlagDto>,
        val delberegningIndeksreguleringPrivatAvtaleResultat: List<GrunnlagDto>,
    )
}
