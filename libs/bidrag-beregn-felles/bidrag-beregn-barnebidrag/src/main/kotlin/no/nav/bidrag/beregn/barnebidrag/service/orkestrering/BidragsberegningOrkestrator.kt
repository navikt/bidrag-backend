package no.nav.bidrag.beregn.barnebidrag.service.orkestrering

import com.fasterxml.jackson.databind.node.POJONode
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningSakConsumer
import no.nav.bidrag.beregn.core.exception.BidragsberegningFeiletTekniskException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgOppfostringsbidragBeregningException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgOppfostringsbidragException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgUfullstendigeGrunnlagException
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.beregning.Beregningstype
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.minOfNullable
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregningGrunnlagV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorResponseV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningResultatBarnV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatBeregning
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatVedtakV2
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumBidragTilFordeling
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtaleGrunnlagV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtalePeriodeGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutaPar
import no.nav.bidrag.transport.behandling.felles.grunnlag.ValutakursGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragspliktig
import no.nav.bidrag.transport.behandling.felles.grunnlag.erPerson
import no.nav.bidrag.transport.behandling.felles.grunnlag.erRevurderingsbarn
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGyldigeGrunnlagForBarn
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentAllePersoner
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.personObjekt
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.person.PersonStønad
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningResultat(
    val søknadsbarnreferanse: String,
    val beregnetBarnebidragResultat: BeregnetBarnebidragResultat? = null,
    val beregningsfeil: Exception? = null,
    val fatteVedtakAnbefalt: Boolean = false,
    val avvistRevurderingsbarn: Boolean = false,
)

data class BeregningsResultatOrkestrert(
    val resultatListe: List<BeregningResultat>,
    val grunnlagsliste: List<GrunnlagDto> = resultatListe.filter { it.beregnetBarnebidragResultat != null }.flatMap {
        it.beregnetBarnebidragResultat!!.grunnlagListe
    }.distinctBy { it.referanse },

)

const val BARNEBIDRAG_BEREGNING_GRUNNLAGSREFERANSE_SJEKK_EVNESPREKK_ETTER_FF_POSTFIX = "_2A"

@Service
@Import(BeregnBarnebidragApi::class, OmgjøringOrkestrator::class, OmgjøringOrkestratorV2::class)
class BidragsberegningOrkestrator(
    private val barnebidragApi: BeregnBarnebidragApi,
    private val omgjøringOrkestrator: OmgjøringOrkestrator,
    private val omgjøringOrkestratorV2: OmgjøringOrkestratorV2,
    private val hentLøpendeBidragService: HentLøpendeBidragService,
    private val personConsumer: BeregningPersonConsumer,
    private val sakConsumer: BeregningSakConsumer,
) {

    fun utførBidragsberegningV3(request: BidragsberegningOrkestratorRequestV2): BidragsberegningOrkestratorResponseV2 = when (request.beregningstype) {
        Beregningstype.BIDRAG -> {
            secureLogger.debug { "Utfører bidragsberegning for request: ${commonObjectmapper.writeValueAsString(request)}" }

            // Sjekk om det skal gis direkte avslag for alle barn
            if (request.erDirekteAvslag) {
                // Kaller beregning for ett og ett søknadsbarn
                val respons = utforBeregningDirekteAvslag(request)
                secureLogger.debug { "Direkte avslag, respons fra beregning: $${commonObjectmapper.writeValueAsString(respons)}" }

                BidragsberegningOrkestratorResponseV2(
                    grunnlagListe = respons.flatMap { it.second }.distinct(),
                    resultat = respons.map { it.first },
                )
            } else {
                val resultat = orkestrerBeregning(request)
                BidragsberegningOrkestratorResponseV2(
                    grunnlagListe = resultat.grunnlagsliste,
                    resultat =
                    resultat.resultatListe.map { bergningResultat ->
                        BidragsberegningResultatBarnV2(
                            søknadsbarnreferanse = bergningResultat.søknadsbarnreferanse,
                            fatteVedtakAnbefalt = bergningResultat.fatteVedtakAnbefalt,
                            avvistRevurderingsbarn = bergningResultat.avvistRevurderingsbarn,
                            beregningsfeil = bergningResultat.beregningsfeil,
                            resultatVedtakListe = bergningResultat.beregnetBarnebidragResultat?.let {
                                listOf(
                                    ResultatVedtakV2(
                                        periodeListe = it.beregnetBarnebidragPeriodeListe,
                                        vedtakstype = Vedtakstype.ENDRING,
                                    ),
                                )
                            } ?: listOf(
                                ResultatVedtakV2(
                                    periodeListe = emptyList(),
                                    vedtakstype = Vedtakstype.ENDRING,
                                ),
                            ),

                        )
                    },
                )
            }
        }

        Beregningstype.OMGJØRING -> {
            secureLogger.debug { "Utfører omgjøringsberegning for request: $request" }
            val respons = orkestrerBeregning(request, true)
            BidragsberegningOrkestratorResponseV2(

                grunnlagListe = respons.grunnlagsliste,
                resultat = respons.resultatListe.map {
                    BidragsberegningResultatBarnV2(
                        søknadsbarnreferanse = it.søknadsbarnreferanse,
                        beregningsfeil = it.beregningsfeil,
                        avvistRevurderingsbarn = it.avvistRevurderingsbarn,
                        fatteVedtakAnbefalt = it.fatteVedtakAnbefalt,
                        resultatVedtakListe = if (it.beregnetBarnebidragResultat == null) {
                            emptyList()
                        } else {
                            listOf(
                                ResultatVedtakV2(
                                    periodeListe = it.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe,
                                    delvedtak = false,
                                    omgjøringsvedtak = true,
                                    vedtakstype = Vedtakstype.KLAGE,
                                ),
                            )
                        },
                    )
                },
            )
        }

        Beregningstype.OMGJØRING_ENDELIG -> {
            secureLogger.debug { "Utfører omgjøringsberegning for request: $request" }
            val klageberegningResultat = orkestrerBeregning(request, true)
            val respons = klageberegningResultat.resultatListe.map { resultat ->
                if (resultat.beregningsfeil != null) {
                    BidragsberegningResultatBarnV2(
                        søknadsbarnreferanse = resultat.søknadsbarnreferanse,
                        resultatVedtakListe = emptyList(),
                        beregningsfeil = resultat.beregningsfeil,
                    ) to request.grunnlagsliste
                } else {
                    val barnRequest = request.beregningBarn.find {
                        it.søknadsbarnreferanse == resultat.søknadsbarnreferanse
                    }!!
                    val erRevurderingsbarn = klageberegningResultat.grunnlagsliste.hentPersonMedReferanse(
                        barnRequest.søknadsbarnreferanse,
                    )!!.erRevurderingsbarn
                    val skalFatteVedtak = if (erRevurderingsbarn) {
                        val overstyrtFatteVedtak = barnRequest.omgjøringOrkestratorGrunnlag?.skalFatteVedtakForRevurderingsbarn != null
                        if (resultat.avvistRevurderingsbarn) {
                            false
                        } else if (overstyrtFatteVedtak) {
                            barnRequest.omgjøringOrkestratorGrunnlag?.skalFatteVedtakForRevurderingsbarn == true
                        } else {
                            resultat.fatteVedtakAnbefalt
                        }
                    } else {
                        true
                    }
                    val requestBarn = request.beregningBarn.find { bb -> bb.søknadsbarnreferanse == resultat.søknadsbarnreferanse }
                    val endeligKlageberegningResultat = omgjøringOrkestratorV2.utførOmgjøringEndelig(
                        omgjøringResultat = resultat.beregnetBarnebidragResultat!!,
                        omgjøringGrunnlagInput = requestBarn!!.tilBeregnGrunnlagV1Klage(request.grunnlagsliste),
                        omgjøringOrkestratorGrunnlag =
                        requestBarn.omgjøringOrkestratorGrunnlag ?: throw IllegalArgumentException("klageOrkestratorGrunnlag må være angitt"),
                        skalFatteVedtak = skalFatteVedtak,
                    )
                    BidragsberegningResultatBarnV2(
                        resultat.søknadsbarnreferanse,
                        avvistRevurderingsbarn = resultat.avvistRevurderingsbarn,
                        fatteVedtakAnbefalt = resultat.fatteVedtakAnbefalt,
                        resultatVedtakListe = endeligKlageberegningResultat.map {
                            ResultatVedtakV2(
                                periodeListe = it.resultat.beregnetBarnebidragPeriodeListe,
                                delvedtak = it.delvedtak,
                                grunnlagslisteDelvedtak = if (it.delvedtak) it.resultat.grunnlagListe else emptyList(),
                                omgjøringsvedtak = it.omgjøringsvedtak,
                                beregnet = it.beregnet,
                                vedtakstype = it.vedtakstype,
                            )
                        },
                    ) to run {
                        val grunnlagslisteOmgjøring = endeligKlageberegningResultat.flatMap {
                            if (!it.delvedtak && !it.omgjøringsvedtak) {
                                it.resultat.grunnlagListe
                            } else {
                                emptyList()
                            }
                        }
                        // Legg til grunnlag fra beregningen som kan inneholde vurdering av FF
                        val grunnlagslisteAlle =
                            grunnlagslisteOmgjøring + resultat.beregnetBarnebidragResultat.grunnlagListe
                        grunnlagslisteAlle.distinctBy { it.referanse }
                    }
                }
            }.toList()
            secureLogger.debug { "Resultat av bidragsberegning: $respons" }

            val resultatGrunnlagsliste = respons.flatMap { it.second }
            val grunnlagslisteFraRunde2A = klageberegningResultat.grunnlagsliste.filter {
                it.referanse.endsWith(BARNEBIDRAG_BEREGNING_GRUNNLAGSREFERANSE_SJEKK_EVNESPREKK_ETTER_FF_POSTFIX)
            }
            val grunnlagslisteJustert =
                leggTilDelberegningReferansePåSluttberegninger(
                    grunnlagListe = resultatGrunnlagsliste,
                    delberegningGrunnlagFraRunde2A = grunnlagslisteFraRunde2A,
                )
            val referertGrunnlagslisteFraRunde2A = klageberegningResultat.grunnlagsliste.filter { grunnlag ->
                !resultatGrunnlagsliste.map { it.referanse }.contains(grunnlag.referanse)
            }

            BidragsberegningOrkestratorResponseV2(
                grunnlagListe = (grunnlagslisteJustert + grunnlagslisteFraRunde2A + referertGrunnlagslisteFraRunde2A).distinctBy { it.referanse },
                resultat = respons.map { it.first },
            )
        }
    }

    // Orkestrering av beregningen
    //
    // Runde 1: Hvis det bare er søknadsbarn som er del av opprinnelig behandling i lista over søknadsbarn kjøres beregningen som normalt. Det
    // innebærer at det ikke er utløst forholdsmessig fordeling med innhenting av nye grunnlag for revurderingsbarn (= "runde 1" av beregningen).
    //
    // Hvis det finnes søknadsbarn i requesten som ikke er del av opprinnelig behandling (aka revurderingsbarn) må beregningen (potensielt)
    // gjøres i 2 runder. Det er da innhentet nye grunnlag for revurderingsbarn etter runde 1 og disse sendes inn som søknadsbarn.
    // Runde 2A: Revurderingsbarn filtreres bort fra søknadsbarn-lista og det vil da innhentes løpende bidrag for disse (som i runde 1)
    // Runde 2B: Hvis det ikke er full evne i runde 2A kjøres beregningen med nye grunnlag for revurderingsbarn (løpende bidrag som overlapper
    //           vil da bli filtrert bort i HentLøpendeBidragService)
    private fun orkestrerBeregning(
        request: BidragsberegningOrkestratorRequestV2,
        beregnForOmgjøring: Boolean = false,
    ): BeregningsResultatOrkestrert {
        val søknadsbarnSomErDelAvOpprinneligBehandlingListe = request.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
            .filter { it.innhold.delAvOpprinneligBehandling }
            .map { it.referanse }

        val søknadsbarnSomIkkeErDelAvOpprinneligBehandlingListe = request.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
            .filter { !it.innhold.delAvOpprinneligBehandling }
            .map { it.referanse }

        return if (søknadsbarnSomIkkeErDelAvOpprinneligBehandlingListe.isEmpty()) {
            // Alle søknadsbarn er del av opprinnelig behandling, ingen orkestrering nødvendig (runde 1)
            try {
                val resultat = utførBeregningOgFeilhåndtering(
                    request = request,
                    beregnForOmgjøring = beregnForOmgjøring,
                )
                BeregningsResultatOrkestrert(
                    resultat.map {
                        BeregningResultat(
                            søknadsbarnreferanse = it.søknadsbarnreferanse,
                            beregnetBarnebidragResultat = it.beregnetBarnebidragResultat,
                            fatteVedtakAnbefalt = it.fatteVedtakAnbefalt,
                        )
                    },
                )
            } catch (e: Exception) {
                håndterBeregningsfeil(e = e, request = request)
            }
        } else {
            val filtrertSøknadsbarnListe = request.beregningBarn
                .filter { it.søknadsbarnreferanse in søknadsbarnSomErDelAvOpprinneligBehandlingListe }
            // Filtrerer bort søknadsbarn som ikke er del av opprinnelig behandling (revurderingsbarn) og kaller beregning (runde 2A)
            try {
                val resultat = utførBeregningOgFeilhåndtering(
                    request = request.copy(
                        beregningBarn = filtrertSøknadsbarnListe,
                    ),
                    beregnForOmgjøring = beregnForOmgjøring,
                )
                val grunnlagsliste = resultat.flatMap {
                    it.beregnetBarnebidragResultat.grunnlagListe
                }.distinctBy { it.referanse }.toMutableSet()
                val avvisteRevurderingsbarn = hentAvvisteRevurderingsbarn(resultat = resultat, grunnlagsliste = grunnlagsliste, request = request)
                BeregningsResultatOrkestrert(
                    (resultat + avvisteRevurderingsbarn).map {
                        BeregningResultat(
                            søknadsbarnreferanse = it.søknadsbarnreferanse,
                            beregnetBarnebidragResultat = it.beregnetBarnebidragResultat,
                            fatteVedtakAnbefalt = it.fatteVedtakAnbefalt,
                            avvistRevurderingsbarn = it.avvistRevurderingsbarn,
                        )
                    },
                    grunnlagsliste = grunnlagsliste.toList(),
                )
            } catch (e: IkkeFullBidragsevneOgUfullstendigeGrunnlagException) {
                secureLogger.debug(e) {
                    "Beregning (runde 2) med løpende bidrag for revurderingsbarn medfører evnesprekk. Utfører ny beregning med nye grunnlag."
                }
                // Kaller beregning med nye grunnlag for søknadsbarn som ikke er del av opprinnelig behandling (revurderingsbarn) (runde 2B).
                // Tar i tillegg med utvalgte grunnlag fra runde 2A som brukes i delberegning fatte vedtak.
                try {
                    val resultat = utførBeregningOgFeilhåndtering(
                        request = request,
                        grunnlagOpprinneligBeregningListe = filtrerGrunnlagsreferanserFraOpprinneligBeregning(e.data.grunnlagListe),
                        beregnForOmgjøring = beregnForOmgjøring,
                    )
                    val resultatGrunnlagsliste = resultat.flatMap {
                        it.beregnetBarnebidragResultat.grunnlagListe
                    }.distinctBy { it.referanse }
                    val grunnlagslisteFraRunde2A = filtrerDelberegningGrunnlagFraEvnesprekk(e.data.grunnlagListe)
                        .filter { grunnlag ->
                            grunnlag.referanse !in resultatGrunnlagsliste.map { it.referanse }
                        }
                    val grunnlagslisteJustert =
                        leggTilDelberegningReferansePåSluttberegninger(
                            grunnlagListe = resultatGrunnlagsliste,
                            delberegningGrunnlagFraRunde2A = grunnlagslisteFraRunde2A,
                        )
                    val grunnlagsliste = (grunnlagslisteFraRunde2A + grunnlagslisteJustert).toMutableSet()

                    val avvisteRevurderingsbarn = hentAvvisteRevurderingsbarn(resultat = resultat, grunnlagsliste = grunnlagsliste, request = request)
                    val resultatAlle = resultat + avvisteRevurderingsbarn
                    BeregningsResultatOrkestrert(
                        resultatListe = resultatAlle.map {
                            BeregningResultat(
                                søknadsbarnreferanse = it.søknadsbarnreferanse,
                                beregnetBarnebidragResultat = it.beregnetBarnebidragResultat,
                                fatteVedtakAnbefalt = it.fatteVedtakAnbefalt,
                                avvistRevurderingsbarn = it.avvistRevurderingsbarn,
                            )
                        },
                        grunnlagsliste = grunnlagsliste.toList(),
                    )
                } catch (e2: Exception) {
                    håndterBeregningsfeil(e = e2, request = request)
                }
            } catch (e: Exception) {
                håndterBeregningsfeil(e = e, request = request)
            }
        }
    }

    private fun håndterBeregningsfeil(e: Exception, request: BidragsberegningOrkestratorRequestV2): BeregningsResultatOrkestrert {
        if (e is IkkeFullBidragsevneOgUfullstendigeGrunnlagException || e is IkkeFullBidragsevneOgOppfostringsbidragException) {
            throw e
        }
        secureLogger.debug(e) { "Feil ved beregning for flere barn i bidragsberegningorkestrator" }
        return BeregningsResultatOrkestrert(
            request.beregningBarn.map {
                BeregningResultat(
                    søknadsbarnreferanse = it.søknadsbarnreferanse,
                    fatteVedtakAnbefalt = false,
                    beregnetBarnebidragResultat = BeregnetBarnebidragResultat(),
                    beregningsfeil = e,
                )
            },

        )
    }

    private fun hentAvvisteRevurderingsbarn(
        resultat: List<BeregnetBarnebidragResultatV2>,
        grunnlagsliste: MutableSet<GrunnlagDto>,
        request: BidragsberegningOrkestratorRequestV2,
    ): List<BeregnetBarnebidragResultatV2> {
        val beregningenInneholderRevurderingsbarn =
            resultat.any {
                val personobjekt = grunnlagsliste.hentPersonMedReferanse(it.søknadsbarnreferanse) ?: return@any false
                !personobjekt.personObjekt.delAvOpprinneligBehandling
            }
        if (beregningenInneholderRevurderingsbarn) return emptyList()

        val personobjekterRevurderingsbarn = request.grunnlagsliste.hentAllePersoner()
            .filter { it.type == Grunnlagstype.PERSON_SØKNADSBARN }
            .filter { !it.personObjekt.delAvOpprinneligBehandling }

        grunnlagsliste.addAll(personobjekterRevurderingsbarn as List<GrunnlagDto>)
        return personobjekterRevurderingsbarn.map {
            BeregnetBarnebidragResultatV2(
                søknadsbarnreferanse = it.referanse,
                beregnetBarnebidragResultat = BeregnetBarnebidragResultat(),
                avvistRevurderingsbarn = true,
            )
        }
    }

    private fun utforBeregningDirekteAvslag(
        request: BidragsberegningOrkestratorRequestV2,
    ): List<Pair<BidragsberegningResultatBarnV2, List<GrunnlagDto>>> = request.beregningBarn.map { beregningBarn ->
        try {
            val beregningResultat =
                barnebidragApi.opprettAvslag(
                    beregnGrunnlag = beregningBarn.tilBeregnGrunnlagV1(
                        request.grunnlagsliste,
                    ).leggTilÅpenSluttperiodeHvisDirekteAvslagBeregning(),
                )
            BidragsberegningResultatBarnV2(
                søknadsbarnreferanse = beregningBarn.søknadsbarnreferanse,
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
                søknadsbarnreferanse = beregningBarn.søknadsbarnreferanse,
                resultatVedtakListe = emptyList(),
                beregningsfeil = e,
            ) to request.grunnlagsliste
        }
    }

    // TODO: Ikke gjør endringer her. Lag en utførBidragsberegningV3 som tar hensyn til FF. utførBidragsberegningV3 skal ha samme api (input og output) som utførBidragsberegningV2
    @Deprecated("Bruk utførBidragsberegningV3 som tar hensyn til FF")
    fun utførBidragsberegningV2(request: BidragsberegningOrkestratorRequestV2): BidragsberegningOrkestratorResponseV2 {
        when (request.beregningstype) {
            Beregningstype.BIDRAG -> {
                secureLogger.debug { "Utfører bidragsberegning for request: $${commonObjectmapper.writeValueAsString(request)}" }
                val respons = request.beregningBarn.map {
                    try {
                        val beregningResultat = if (request.erDirekteAvslag) {
                            barnebidragApi.opprettAvslag(
                                it.tilBeregnGrunnlagV1(request.grunnlagsliste).leggTilÅpenSluttperiodeHvisDirekteAvslagBeregning(),
                            )
                        } else {
                            barnebidragApi.beregn(
                                beregnGrunnlag = it.tilBeregnGrunnlagV1(request.grunnlagsliste),
                            )
                        }
                        BidragsberegningResultatBarnV2(
                            it.søknadsbarnreferanse,
                            listOf(
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
                            it.søknadsbarnreferanse,
                            emptyList(),
                            beregningsfeil = e,
                        ) to request.grunnlagsliste
                    }
                }

                secureLogger.debug { "Resultat av bidragsberegning: $respons" }
                return BidragsberegningOrkestratorResponseV2(
                    respons.flatMap { it.second },
                    respons.map { it.first },
                )
            }

            Beregningstype.OMGJØRING -> {
                secureLogger.debug { "Utfører omgjøringsberegning for request: $request" }
                val respons = request.beregningBarn.map {
                    try {
                        val klageberegningResultat = if (request.erDirekteAvslag) {
                            // Avslagsperiode skal alltid være løpende hvis det ikke kommer noe periode etter opphøret (feks ved etterfølgende vedtak i orkestrering)
                            barnebidragApi.opprettAvslag(
                                it.tilBeregnGrunnlagV1(request.grunnlagsliste).leggTilÅpenSluttperiodeHvisDirekteAvslagBeregning(),
                            )
                        } else {
                            barnebidragApi.beregn(
                                beregnGrunnlag = it.tilBeregnGrunnlagV1(request.grunnlagsliste),
                            )
                        }
                        BidragsberegningResultatBarnV2(
                            it.søknadsbarnreferanse,
                            listOf(
                                ResultatVedtakV2(
                                    periodeListe = klageberegningResultat.beregnetBarnebidragPeriodeListe,
                                    delvedtak = false,
                                    omgjøringsvedtak = true,
                                    vedtakstype = Vedtakstype.KLAGE,
                                ),
                            ),
                        ) to klageberegningResultat.grunnlagListe
                    } catch (e: Exception) {
                        BidragsberegningResultatBarnV2(
                            it.søknadsbarnreferanse,
                            emptyList(),
                            beregningsfeil = e,
                        ) to request.grunnlagsliste
                    }
                }
                secureLogger.debug { "Resultat av omgjøringsberegning: $${commonObjectmapper.writeValueAsString(respons)}" }
                return BidragsberegningOrkestratorResponseV2(
                    respons.flatMap { it.second },
                    respons.map { it.first },
                )
            }

            Beregningstype.OMGJØRING_ENDELIG -> {
                secureLogger.debug { "Utfører endelig omgjøringsberegning for request: $request" }
                val respons = request.beregningBarn.map {
                    try {
                        val klageberegningResultat = if (request.erDirekteAvslag) {
                            barnebidragApi.opprettAvslag(
                                it.tilBeregnGrunnlagV1(request.grunnlagsliste).leggTilÅpenSluttperiodeHvisDirekteAvslagBeregning(),
                            )
                        } else {
                            barnebidragApi.beregn(
                                beregnGrunnlag = it.tilBeregnGrunnlagV1(request.grunnlagsliste),
                            )
                        }
                        val endeligKlageberegningResultat = omgjøringOrkestrator.utførOmgjøringEndelig(
                            omgjøringResultat = klageberegningResultat,
                            omgjøringGrunnlagInput = it.tilBeregnGrunnlagV1(request.grunnlagsliste),
                            omgjøringOrkestratorGrunnlag =
                            it.omgjøringOrkestratorGrunnlag ?: throw IllegalArgumentException("klageOrkestratorGrunnlag må være angitt"),
                        )
                        BidragsberegningResultatBarnV2(
                            it.søknadsbarnreferanse,
                            endeligKlageberegningResultat.map { resultatVedtak ->
                                ResultatVedtakV2(
                                    periodeListe = resultatVedtak.resultat.beregnetBarnebidragPeriodeListe,
                                    delvedtak = resultatVedtak.delvedtak,
                                    grunnlagslisteDelvedtak = if (resultatVedtak.delvedtak) resultatVedtak.resultat.grunnlagListe else emptyList(),
                                    omgjøringsvedtak = resultatVedtak.omgjøringsvedtak,
                                    beregnet = resultatVedtak.beregnet,
                                    vedtakstype = resultatVedtak.vedtakstype,
                                )
                            },
                        ) to
                            endeligKlageberegningResultat.flatMap { resultatVedtak ->
                                if (!resultatVedtak.delvedtak && !resultatVedtak.omgjøringsvedtak) {
                                    resultatVedtak.resultat.grunnlagListe
                                } else {
                                    emptyList()
                                }
                            }
                    } catch (ex: Exception) {
                        BidragsberegningResultatBarnV2(
                            it.søknadsbarnreferanse,
                            emptyList(),
                            beregningsfeil = ex,
                        ) to request.grunnlagsliste
                    }
                }
                secureLogger.debug { "Resultat av endelig klageberegning: $respons" }
                return BidragsberegningOrkestratorResponseV2(
                    respons.flatMap { it.second },
                    respons.map { it.first },
                )
            }
        }
    }

    private fun utførBeregningOgFeilhåndtering(
        request: BidragsberegningOrkestratorRequestV2,
        grunnlagOpprinneligBeregningListe: List<GrunnlagDto> = emptyList(),
        beregnForOmgjøring: Boolean = false,
    ): List<BeregnetBarnebidragResultatV2> {
        // Kaller beregning for alle barn samlet

        return try {
            var løpendeBidragOgBeregningerGrunnlag: List<BeregnGrunnlag> = emptyList()
            if (request.skalHensyntaLøpendeBidrag) {
                løpendeBidragOgBeregningerGrunnlag = hentGrunnlagForLøpendeBidrag(request)
            }
            val grunnlagSøknadsbarnListe = request.tilListeBeregnGrunnlagV1()
            val totalBeregningsperiode = finnTotalBeregningsperiode(request)
            val privatAvtaleGrunnlag = finnPrivatAvtaleGrunnlag(
                grunnlagSøknadsbarnListe = grunnlagSøknadsbarnListe,
                request = request,
                totalBeregningsperiode = totalBeregningsperiode,
            )
            val grunnlagValutakursListe = byggValutakursListe()

            val beregningResultatListe = barnebidragApi.beregnV2(
                beregningsperiode = totalBeregningsperiode,
                grunnlagSøknadsbarnListe = grunnlagSøknadsbarnListe,
                grunnlagLøpendeBidragListe = løpendeBidragOgBeregningerGrunnlag,
                grunnlagPrivatAvtaleListe = privatAvtaleGrunnlag,
                grunnlagValutakursListe = grunnlagValutakursListe,
                grunnlagOpprinneligBeregningListe = grunnlagOpprinneligBeregningListe,
            )

            secureLogger.debug { "Resultat av bidragsberegning: $beregningResultatListe" }

            beregningResultatListe.map { resultat ->
                val beregningInput = request.beregningBarn.find { it.søknadsbarnreferanse == resultat.søknadsbarnreferanse }!!

                // Hvis virkning er samme som opphørsdato så betyr det at saksbehandler har valgt direkte avslag fra virkningstidspunkt bildet.
                // Virkningtidspunkt blir da samme som opphørsdat
                if (beregningInput.virkningstidspunkt == beregningInput.opphørsdato) {
                    val endeligResultat = resultat.beregnetBarnebidragResultat
                    val periodeListe = endeligResultat.beregnetBarnebidragPeriodeListe
                    val sistePeriode = periodeListe.maxByOrNull { it.periode.fom }
                    val periodeOpphør =
                        sistePeriode?.periode?.til ?: sistePeriode?.periode?.fom ?: beregningInput.virkningstidspunkt
                    resultat.copy(
                        beregnetBarnebidragResultat = resultat.beregnetBarnebidragResultat.copy(
                            // Legger til grunnlagsliste personer som er nødvendig for videre behandling i omgjøringsorkestrator
                            grunnlagListe = request.grunnlagsliste.filter { it.erPerson() },
                            beregnetBarnebidragPeriodeListe = periodeListe.ifEmpty {
                                listOf(
                                    ResultatPeriode(
                                        periode =
                                        ÅrMånedsperiode(
                                            periodeOpphør,
                                            null,
                                        ),
                                        ResultatBeregning(null),
                                        resultat.beregnetBarnebidragResultat.grunnlagListe.filter { it.erPerson() }.map { it.referanse },
                                    ),
                                )
                            },
                        ),
                    )
                } else {
                    resultat
                }
            }
        } catch (e: IkkeFullBidragsevneOgOppfostringsbidragBeregningException) {
            val errorMessage = e.message
                ?: "Det finnes perioder med evnesprekk og løpende bidrag med oppfostringsbidrag. Disse må håndteres manuelt før vedtak kan fattes."
            secureLogger.debug(e) { errorMessage }
            throw IkkeFullBidragsevneOgOppfostringsbidragException(
                melding = errorMessage,
                data = mapBeregningExceptionToResponse(e.data, beregnForOmgjøring),
            )
        } catch (e: IkkeFullBidragsevneOgUfullstendigeGrunnlagBeregningException) {
            val errorMessage = e.message
                ?: "Det finnes perioder med evnesprekk. Nye grunnlag må hentes inn for løpende bidrag før vedtak kan fattes."
            secureLogger.debug(e) { errorMessage }
            throw IkkeFullBidragsevneOgUfullstendigeGrunnlagException(
                melding = errorMessage,
                data = mapBeregningExceptionToResponse(e.data, beregnForOmgjøring),
            )
        } catch (e: Exception) {
            secureLogger.debug(e) { "Feil ved beregning for flere barn i bidragsberegningorkestrator" }
            throw BidragsberegningFeiletTekniskException(
                "Det skjedde en teknisk feil under beregning av bidrag. ${e.message}",
                BidragsberegningOrkestratorResponseV2(
                    emptyList(),
                    request.beregningBarn.map {
                        BidragsberegningResultatBarnV2(
                            søknadsbarnreferanse = it.søknadsbarnreferanse,
                        )
                    },
                ),
            )
        }
    }

    private fun BeregnGrunnlag.leggTilÅpenSluttperiodeHvisDirekteAvslagBeregning() = copy(
        periode = periode.copy(
            til = null,
        ),
    )

    /**
     * Trekker ut delberegningsgrunnlag fra evnesprekk-unntaket (runde 2A) som skal hektes på
     * den påfølgende beregningen med nye grunnlag for revurderingsbarna (runde 2B). Disse grunnlagene
     * inneholder sum-bidrag-til-fordeling beregnet med de opprinnelige løpende bidragene og må slås sammen
     * med resultatet fra runde 2B slik at sluttberegningene får riktige referanser. Lager nye referanser
     * for objekter fra runde 2A for å unngå duplikater mellom objekter som tilhører 2A og 2B.
     */
    private fun filtrerDelberegningGrunnlagFraEvnesprekk(grunnlagListe: List<GrunnlagDto>): List<GrunnlagDto> {
        val filtrertGrunnlagsliste = grunnlagListe.filter {
            it.type.name.startsWith("DELBEREGNING") ||
                it.type.name.startsWith("SLUTTBEREGNING") ||
                it.type == Grunnlagstype.LØPENDE_BIDRAG_PERIODE
        }

        val referanser = filtrertGrunnlagsliste.map { it.referanse }.toSet()
        val grunnlagslisteReferert = filtrertGrunnlagsliste.flatMap { traverserGrunnlagRekursivt(grunnlagListe, it) }
            .filter { grunnlag -> !filtrertGrunnlagsliste.map { it.referanse }.contains(grunnlag.referanse) }

        val grunnlagFraEvnesprekkKonvertert = filtrertGrunnlagsliste.map { grunnlag ->
            grunnlag.copy(
                referanse = "${grunnlag.referanse}$BARNEBIDRAG_BEREGNING_GRUNNLAGSREFERANSE_SJEKK_EVNESPREKK_ETTER_FF_POSTFIX",
                grunnlagsreferanseListe = grunnlag.grunnlagsreferanseListe.map { referanse ->
                    if (referanser.any { referanse.contains(it) }) {
                        "${referanse}$BARNEBIDRAG_BEREGNING_GRUNNLAGSREFERANSE_SJEKK_EVNESPREKK_ETTER_FF_POSTFIX"
                    } else {
                        referanse
                    }
                },
            )
        }
        return grunnlagslisteReferert + grunnlagFraEvnesprekkKonvertert
    }

    // Rekursiv funksjon som finner alle grunnlagsreferanser som refereres til fra toppnivået og nedover
    private fun traverserGrunnlagRekursivt(
        grunnlagListe: List<GrunnlagDto>,
        startGrunnlag: GrunnlagDto,
        innsamledeReferanser: MutableSet<GrunnlagDto> = mutableSetOf(),
    ): Set<GrunnlagDto> {
        // Unngå evig loop ved sirkulære referanser
        if (!innsamledeReferanser.add(startGrunnlag)) return innsamledeReferanser

        // Finn refererte grunnlagselementer og prosesser dem rekursivt
        startGrunnlag.grunnlagsreferanseListe
            .mapNotNull { ref -> grunnlagListe.find { it.referanse == ref } }
            .forEach {
                traverserGrunnlagRekursivt(
                    grunnlagListe = grunnlagListe,
                    startGrunnlag = it,
                    innsamledeReferanser = innsamledeReferanser,
                )
            }

        return innsamledeReferanser
    }

    /**
     * Kobler hver sluttberegning i runde 2B til den tilhørende [DelberegningSumBidragTilFordeling]
     * fra runde 2A ved å legge til dens referanse i [GrunnlagDto.grunnlagsreferanseListe].
     * Oppslagsnøkkelen er perioden — kun delberegninger med overlappende periode ifht sluttberegningen kobles på.
     */
    private fun leggTilDelberegningReferansePåSluttberegninger(
        grunnlagListe: List<GrunnlagDto>,
        delberegningGrunnlagFraRunde2A: List<GrunnlagDto>,
    ): List<GrunnlagDto> {
        val sumBidragPerPeriode = delberegningGrunnlagFraRunde2A
            .filter { it.type == Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING }
            .associateBy { it.innholdTilObjekt<DelberegningSumBidragTilFordeling>().periode }

        return grunnlagListe.map { grunnlag ->
            if (grunnlag.type == Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG) {
                val periode = grunnlag.innholdTilObjekt<SluttberegningBarnebidragV2>().periode
                val sumBidragReferanser = sumBidragPerPeriode
                    .filter { periode.overlapperMed(it.key) }
                    .values
                    .map { it.referanse }

                if (sumBidragReferanser.isNotEmpty()) {
                    val oppdatertGrunnlag = grunnlag.copy(
                        grunnlagsreferanseListe = (grunnlag.grunnlagsreferanseListe + sumBidragReferanser).toSet().toList(),
                    )

                    oppdatertGrunnlag
                } else {
                    grunnlag
                }
            } else {
                grunnlag
            }
        }
    }

    private fun BeregningGrunnlagV2.tilBeregnGrunnlagV1Klage(grunnlagListe: List<GrunnlagDto>) = BeregnGrunnlag(
        periode = ÅrMånedsperiode(minOfNullable(virkningstidspunkt, beregningsperiode.til)!!, beregningsperiode.til),
        opphørsdato = opphørsdato,
        stønadstype = stønadstype,
        søknadsbarnReferanse = søknadsbarnreferanse,
        grunnlagListe = grunnlagListe,
    )

    private fun BeregningGrunnlagV2.tilBeregnGrunnlagV1(grunnlagListe: List<GrunnlagDto>) = BeregnGrunnlag(
        periode = beregningsperiode,
        opphørsdato = opphørsdato,
        stønadstype = stønadstype,
        søknadsbarnReferanse = søknadsbarnreferanse,
        grunnlagListe = grunnlagListe,
    )

    // Filtrerer og konverterer grunnlag for hvert søknadsbarn
    internal fun BidragsberegningOrkestratorRequestV2.tilListeBeregnGrunnlagV1(): List<BeregnGrunnlag> = beregningBarn.map { beregningBarn ->
        val bidragspliktigRef =
            this.grunnlagsliste.bidragspliktig?.referanse ?: throw IllegalArgumentException("Finner ikke bidragspliktig i grunnlagsliste")
        val søknadsbarnRef = beregningBarn.søknadsbarnreferanse
        val bidragsmottakerRef = finnBidragsmottakerForBarn(søknadsbarnRef)
        val gyldigeGrunnlagForBarn = this.grunnlagsliste.finnGyldigeGrunnlagForBarn(
            bmRef = bidragsmottakerRef,
            bpRef = bidragspliktigRef,
            barnRef = søknadsbarnRef,
        )
        beregningBarn.tilBeregnGrunnlagV1(gyldigeGrunnlagForBarn)
    }

    // Henter alle søknadsbarn og deres referanser og personidenter fra grunnlagslista
    private fun hentAlleSøknadsbarn(beregningBarnListe: List<BeregningGrunnlagV2>, grunnlagsliste: List<GrunnlagDto>): List<PersonStønad> = beregningBarnListe.mapNotNull { beregningsbarn ->
        val barnGrunnlag =
            grunnlagsliste.filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
                .firstOrNull { it.referanse == beregningsbarn.søknadsbarnreferanse }
                ?: throw IllegalArgumentException(
                    "Fant ikke PERSON_SØKNADSBARN-grunnlag for barn med referanse ${beregningsbarn.søknadsbarnreferanse}",
                )
        barnGrunnlag.innhold.ident?.let { PersonStønad(ident = it.verdi, stønadstype = beregningsbarn.stønadstype) }
    }

    private fun BidragsberegningOrkestratorRequestV2.finnBidragsmottakerForBarn(søknadsbarnreferanse: String): String = grunnlagsliste
        .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
        .firstOrNull { it.referanse == søknadsbarnreferanse }
        ?.innhold?.bidragsmottaker
        ?: throw IllegalArgumentException("Fant ikke bidragsmottaker for barn med referanse $søknadsbarnreferanse")

    private fun finnTotalBeregningsperiode(request: BidragsberegningOrkestratorRequestV2): ÅrMånedsperiode {
        val fom = request.beregningBarn.minOf { it.beregningsperiode.fom }
        val til = request.beregningBarn.maxOf { it.beregningsperiode.til ?: throw IllegalArgumentException("beregningsperiode.til mangler") }
        return ÅrMånedsperiode(fom, til)
    }

    // Filtrerer ut grunnlag som er relevante for privat avtale og legger disse i en egen liste
    internal fun finnPrivatAvtaleGrunnlag(
        grunnlagSøknadsbarnListe: List<BeregnGrunnlag>,
        request: BidragsberegningOrkestratorRequestV2,
        totalBeregningsperiode: ÅrMånedsperiode,
    ): List<BeregnGrunnlag> {
        val søknadsbarnGrunnlagPrivatAvtaleListe = finnPrivatAvtaleGrunnlagForSøknadsbarn(
            grunnlagSøknadsbarnListe = grunnlagSøknadsbarnListe,
            totalBeregningsperiode = totalBeregningsperiode,
        )

        val andreBarnGrunnlagPrivatAvtaleListe = finnPrivatAvtaleGrunnlagForAndreBarn(
            request = request,
            totalBeregningsperiode = totalBeregningsperiode,
        )

        return søknadsbarnGrunnlagPrivatAvtaleListe + andreBarnGrunnlagPrivatAvtaleListe
    }

    // Mapper ut privat avtale-objekter for søknadsbarn
    private fun finnPrivatAvtaleGrunnlagForSøknadsbarn(
        grunnlagSøknadsbarnListe: List<BeregnGrunnlag>,
        totalBeregningsperiode: ÅrMånedsperiode,
    ): List<BeregnGrunnlag> {
        val gyldigePrivatAvtaleGrunnlag = setOf(
            Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG,
            Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG,
        )

        return grunnlagSøknadsbarnListe.mapNotNull { grunnlagSøknadsbarn ->
            val filtrertGrunnlagListe =
                grunnlagSøknadsbarn.grunnlagListe.filter { it.type in gyldigePrivatAvtaleGrunnlag }.toMutableList()

            if (filtrertGrunnlagListe.none { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }) {
                null
            } else {
                leggTilPersonObjekterIGrunnlagslisteForPrivatAvtale(
                    filtrertGrunnlagListe = filtrertGrunnlagListe,
                    alleGrunnlag = grunnlagSøknadsbarn.grunnlagListe,
                    barnReferanse = grunnlagSøknadsbarn.søknadsbarnReferanse,
                )

                BeregnGrunnlag(
                    periode = ÅrMånedsperiode(
                        fom = totalBeregningsperiode.fom,
                        til = grunnlagSøknadsbarn.periode.fom,
                    ),
                    opphørsdato = grunnlagSøknadsbarn.opphørsdato,
                    stønadstype = grunnlagSøknadsbarn.stønadstype,
                    søknadsbarnReferanse = grunnlagSøknadsbarn.søknadsbarnReferanse,
                    grunnlagListe = filtrertGrunnlagListe,
                )
            }
        }
    }

    // Mapper ut privat avtale-objekter for barn som ikke er søknadsbarn
    private fun finnPrivatAvtaleGrunnlagForAndreBarn(
        request: BidragsberegningOrkestratorRequestV2,
        totalBeregningsperiode: ÅrMånedsperiode,
    ): List<BeregnGrunnlag> {
        val søknadsbarnListe = request.beregningBarn.map { it.søknadsbarnreferanse }
        val andreBarnMedPrivatAvtale = request.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(
                Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG,
            )
            .filter { it.gjelderBarnReferanse !in søknadsbarnListe }
            .map {
                PrivatAvtaleAndreBarn(
                    gjelderBarnReferanse = it.gjelderBarnReferanse
                        ?: throw IllegalArgumentException("gjelderBarnReferanse må angis på privat avtale"),
                    stønadstype = it.innhold.stønadstype,
                )
            }

        return andreBarnMedPrivatAvtale.mapNotNull { barn ->
            byggPrivatAvtaleBeregnGrunnlagForAndreBarn(
                barn = barn,
                request = request,
                totalBeregningsperiode = totalBeregningsperiode,
            )
        }
    }

    private fun byggPrivatAvtaleBeregnGrunnlagForAndreBarn(
        barn: PrivatAvtaleAndreBarn,
        request: BidragsberegningOrkestratorRequestV2,
        totalBeregningsperiode: ÅrMånedsperiode,
    ): BeregnGrunnlag? {
        // Mapper ut PRIVAT_AVTALE_GRUNNLAG filtrert på stønadstype (skal bare være 1 forekomst)
        val privatAvtaleGrunnlagReferanse = request.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(
                Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG,
            )
            .filter { it.gjelderBarnReferanse == barn.gjelderBarnReferanse && it.innhold.stønadstype == barn.stønadstype }
            .map { it.referanse }
            .first()

        val filtrertGrunnlagListe = request.grunnlagsliste
            .filter { it.referanse == privatAvtaleGrunnlagReferanse }
            .toMutableList()

        // Mapper ut PRIVAT_AVTALE_PERIODE_GRUNNLAG (skal ligge i grunnlagsreferanselista til PRIVAT_AVTALE_GRUNNLAG)
        filtrertGrunnlagListe.addAll(
            request.grunnlagsliste.filter {
                it.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG &&
                    it.gjelderBarnReferanse == barn.gjelderBarnReferanse &&
                    it.referanse in filtrertGrunnlagListe.first().grunnlagsreferanseListe
            },
        )

        // Legger til personobjekter
        leggTilPersonObjekterIGrunnlagslisteForPrivatAvtale(
            filtrertGrunnlagListe = filtrertGrunnlagListe,
            alleGrunnlag = request.grunnlagsliste,
            barnReferanse = barn.gjelderBarnReferanse,
        )

        // Lager BeregnGrunnlag med relevante grunnlag for privat avtale
        return if (filtrertGrunnlagListe.none { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }) {
            null
        } else {
            BeregnGrunnlag(
                periode = beregnPeriodeForPrivatAvtale(
                    filtrertGrunnlagListe = filtrertGrunnlagListe,
                    totalBeregningsperiode = totalBeregningsperiode,
                ),
                opphørsdato = null,
                stønadstype = barn.stønadstype,
                søknadsbarnReferanse = barn.gjelderBarnReferanse,
                grunnlagListe = filtrertGrunnlagListe,
            )
        }
    }

    private fun leggTilPersonObjekterIGrunnlagslisteForPrivatAvtale(
        filtrertGrunnlagListe: MutableList<GrunnlagDto>,
        alleGrunnlag: List<GrunnlagDto>,
        barnReferanse: String? = null,
    ) {
        // Mapper ut barn person-objekt
        if (barnReferanse != null) {
            filtrertGrunnlagListe.addAll(alleGrunnlag.filter { it.referanse == barnReferanse })
        } else {
            filtrertGrunnlagListe.addAll(
                alleGrunnlag.filter { grunnlag ->
                    grunnlag.referanse in filtrertGrunnlagListe.map { it.gjelderBarnReferanse }
                },
            )
        }

        // Mapper ut bidragsmottaker person-objekt
        val grunnlagstype =
            if (barnReferanse == null) {
                Grunnlagstype.PERSON_SØKNADSBARN
            } else {
                alleGrunnlag.filter { it.referanse == barnReferanse }.map { it.type }.first()
            }
        val bidragsmottakerReferanse = filtrertGrunnlagListe.filtrerOgKonverterBasertPåEgenReferanse<Person>(grunnlagstype)
            .firstOrNull()?.innhold?.bidragsmottaker ?: ""
        filtrertGrunnlagListe.addAll(alleGrunnlag.filter { it.referanse == bidragsmottakerReferanse })

        // Mapper ut bidragspliktig person-objekt
        filtrertGrunnlagListe.addAll(alleGrunnlag.filter { it.type == Grunnlagstype.PERSON_BIDRAGSPLIKTIG })
    }

    // Finner periode det skal beregnes for
    private fun beregnPeriodeForPrivatAvtale(filtrertGrunnlagListe: List<GrunnlagDto>, totalBeregningsperiode: ÅrMånedsperiode): ÅrMånedsperiode {
        val privatAvtalePerioder = filtrertGrunnlagListe.filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtalePeriodeGrunnlag>(
            Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG,
        )

        // Finner laveste verdi av fom-periode for den private avtalen
        val startPrivatAvtale = privatAvtalePerioder.minOfOrNull { it.innhold.periode.fom } ?: totalBeregningsperiode.fom
        // Finner høyeste verdi av til-periode for den private avtalen
        val sluttPrivatAvtale = if (privatAvtalePerioder.any { it.innhold.periode.til == null }) {
            null
        } else {
            privatAvtalePerioder.mapNotNull { it.innhold.periode.til }.maxOrNull()
        }

        // Setter fom-periode for BeregnGrunnlag
        val fomPeriode = maxOf(totalBeregningsperiode.fom, startPrivatAvtale)
        // Setter til-periode for BeregnGrunnlag
        val tilPeriode = when {
            sluttPrivatAvtale != null && totalBeregningsperiode.til != null -> minOf(sluttPrivatAvtale, totalBeregningsperiode.til!!)
            else -> sluttPrivatAvtale ?: totalBeregningsperiode.til
        }

        return ÅrMånedsperiode(fom = fomPeriode, til = tilPeriode)
    }

    private fun hentGrunnlagForLøpendeBidrag(request: BidragsberegningOrkestratorRequestV2): List<BeregnGrunnlag> {
        val søknadsbarnListe = hentAlleSøknadsbarn(request.beregningBarn, request.grunnlagsliste)
        val bidragspliktig = request.grunnlagsliste.bidragspliktig!!

        val beregningsperiode = ÅrMånedsperiode(
            fom = request.beregningBarn.minOf { it.beregningsperiode.fom },
            til = request.beregningBarn.mapNotNull { it.beregningsperiode.til }.maxOrNull(),
        )

        // Henter grunnlag for løpende bidrag
        val løpendeBidragOgBeregninger = hentLøpendeBidragService.hentLøpendeBidragForBehandling(
            bpIdent = Personident(bidragspliktig.personIdent!!),
            beregningsperiode = beregningsperiode,
            request = request,
        )

        val løpendeBarnFødselsdatoMap = mutableMapOf<Personident, LocalDate?>()

        løpendeBidragOgBeregninger.løpendeBidragListe
            .filterNot { lb ->
                PersonStønad(
                    ident = lb.kravhaver.verdi,
                    stønadstype = lb.type,
                ) in søknadsbarnListe
            }
            .forEach { løpendeBidrag ->
                try {
                    val fødselsdato = personConsumer.hentFødselsdatoForPerson(løpendeBidrag.kravhaver)
                    løpendeBarnFødselsdatoMap[løpendeBidrag.kravhaver] = fødselsdato
                } catch (e: RestClientResponseException) {
                    if (e.statusCode == HttpStatus.FORBIDDEN) {
                        // Hvis SB ikke har tilgang til person så bør ikke det feile i løpende bidrag sjekken.
                        // Fødselsdato er ikke viktig å lagre for barn som ikke er del av beregningen så ignorerer
                        return@forEach
                    }
                    throw e
                }
            }

        // Sjekker om sak for innhentede løpende bidrag er utlandssaker og lager en liste med disse
        val utlandssakerListe = løpendeBidragOgBeregninger.løpendeBidragListe
            .distinctBy { it.sak }
            .mapNotNull {
                val sak = sakConsumer.hentSak(it.sak.toString())
                if (sak.kategori == Sakskategori.UTLAND) it.sak.toString() else null
            }

        return løpendeBidragOgBeregninger.tilBeregnGrunnlag(
            bpReferanse = bidragspliktig.referanse,
            søknadsbarnListe = søknadsbarnListe,
            løpendeBarnFødselsdatoMap = løpendeBarnFødselsdatoMap,
            personConsumer = personConsumer,
            request = request,
            utlandssakerListe = utlandssakerListe,
        )
    }

    private fun byggValutakursListe(): List<GrunnlagDto> = listOf(
        GrunnlagDto(
            referanse = "innhentet_valutakurs_grunnlag",
            type = Grunnlagstype.VALUTAKURS_GRUNNLAG,
            innhold = POJONode(hentValutakurser()),
        ),
    )

    // TODO Skal hente valutakurser fra ekstern tjeneste i bidrag-grunnlag
    private fun hentValutakurser() = ValutakursGrunnlag(
        listOf(
            ValutaPar(valutakode1 = Valutakode.NOK, valutakode2 = Valutakode.SEK, valutakurs = BigDecimal.valueOf(0.92)),
            ValutaPar(valutakode1 = Valutakode.SEK, valutakode2 = Valutakode.NOK, valutakurs = BigDecimal.valueOf(1.09)),
            ValutaPar(valutakode1 = Valutakode.NOK, valutakode2 = Valutakode.DKK, valutakurs = BigDecimal.valueOf(0.65)),
            ValutaPar(valutakode1 = Valutakode.DKK, valutakode2 = Valutakode.NOK, valutakurs = BigDecimal.valueOf(1.53)),
            ValutaPar(valutakode1 = Valutakode.NOK, valutakode2 = Valutakode.EUR, valutakurs = BigDecimal.valueOf(0.087)),
            ValutaPar(valutakode1 = Valutakode.EUR, valutakode2 = Valutakode.NOK, valutakurs = BigDecimal.valueOf(11.45)),
            ValutaPar(valutakode1 = Valutakode.NOK, valutakode2 = Valutakode.GBP, valutakurs = BigDecimal.valueOf(0.076)),
            ValutaPar(valutakode1 = Valutakode.GBP, valutakode2 = Valutakode.NOK, valutakurs = BigDecimal.valueOf(13.21)),
            ValutaPar(valutakode1 = Valutakode.NOK, valutakode2 = Valutakode.USD, valutakurs = BigDecimal.valueOf(0.10)),
            ValutaPar(valutakode1 = Valutakode.USD, valutakode2 = Valutakode.NOK, valutakurs = BigDecimal.valueOf(9.60)),
        ),
    )

    private fun mapBeregningExceptionToResponse(
        data: List<BeregnetBarnebidragResultatV2>,
        beregnForOmgjøring: Boolean,
    ): BidragsberegningOrkestratorResponseV2 = BidragsberegningOrkestratorResponseV2(
        grunnlagListe = data
            .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
            .distinctBy { it.referanse },
        resultat = data.map {
            BidragsberegningResultatBarnV2(
                søknadsbarnreferanse = it.søknadsbarnreferanse,
                resultatVedtakListe = listOf(
                    ResultatVedtakV2(
                        periodeListe = it.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe,
                        delvedtak = false,
                        omgjøringsvedtak = beregnForOmgjøring,
                        vedtakstype = if (beregnForOmgjøring) Vedtakstype.KLAGE else Vedtakstype.ENDRING,
                    ),
                ),
            )
        },
    )

    // Sjekker at en periode overlapper med en annen periode (intersect)
    private fun ÅrMånedsperiode.overlapperMed(annenPeriode: ÅrMånedsperiode): Boolean = (
        (annenPeriode.til == null || fom.isBefore(annenPeriode.til)) &&
            (til == null || til!!.isAfter(annenPeriode.fom))
        )

    // Filtrerer ut grunnlagsreferanser fra opprinnelig beregning (2A) (basert på løpende bidrag for revurderingsbarn) som skal brukes i endelig
    // beregning (2B) (basert på nye grunnlag for revurderingsbarn). Tar utgangspunkt i DELBEREGNING_ANDEL_AV_BIDRAGSEVNE, som skal brukes ifbm.
    // sjekk av om det skal fattes vedtak for revurderingsbarn. Tar også med delberegningsgrunnlag og løpende bidrag som refereres fra
    // DELBEREGNING_ANDEL_AV_BIDRAGSEVNE. I tillegg gjøres det en liten justering på referansene for å hindre at det blir samme referanse på grunnlag
    // som tilhører 2A og 2B.
    private fun filtrerGrunnlagsreferanserFraOpprinneligBeregning(grunnlagListe: List<GrunnlagDto>): List<GrunnlagDto> {
        // Finner alle grunnlag som er av type DELBEREGNING_ANDEL_AV_BIDRAGSEVNE, som er utgangspunktet for å finne alle refererte grunnlag
        val grunnlagListeFiltrert = grunnlagListe
            .filter { it.type == Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE }

        // Finner alle grunnlag som refereres fra DELBEREGNING_ANDEL_AV_BIDRAGSEVNE, som er av type DELBEREGNINGxxx eller LØPENDE_BIDRAG_PERIODE
        val refererteGrunnlag = grunnlagListeFiltrert
            .flatMap { traverserGrunnlagRekursivt(grunnlagListe, it) }
            .filter { it.type.toString().startsWith("DELBEREGNING") || it.type == Grunnlagstype.LØPENDE_BIDRAG_PERIODE }
            .distinctBy { it.referanse }

        val referanserSomSkalJusteres = refererteGrunnlag.map { it.referanse }.toSet()
        val referanserSomSkalJusteresJustert = referanserSomSkalJusteres.associateWith {
            "$it$BARNEBIDRAG_BEREGNING_GRUNNLAGSREFERANSE_SJEKK_EVNESPREKK_ETTER_FF_POSTFIX"
        }

        // Lager nye referanser for objekter fra runde 2A for å unngå duplikater mellom objekter som tilhører 2A og 2B
        return refererteGrunnlag.map { grunnlag ->
            grunnlag.copy(
                referanse = referanserSomSkalJusteresJustert[grunnlag.referanse] ?: grunnlag.referanse,
                grunnlagsreferanseListe = grunnlag.grunnlagsreferanseListe.map { ref ->
                    referanserSomSkalJusteresJustert[ref] ?: ref
                },
            )
        }
    }

    data class PrivatAvtaleAndreBarn(val gjelderBarnReferanse: String, val stønadstype: Stønadstype)
}
