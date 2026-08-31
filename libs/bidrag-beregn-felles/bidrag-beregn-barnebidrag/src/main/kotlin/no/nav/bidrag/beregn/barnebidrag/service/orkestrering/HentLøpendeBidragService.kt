package no.nav.bidrag.beregn.barnebidrag.service.orkestrering

import com.fasterxml.jackson.databind.node.POJONode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.annotation.Timed
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.vedtak.Vedtaksfiltrering
import no.nav.bidrag.commons.security.SikkerhetsKontekst
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.BehandlingsrefKilde
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.domene.util.avrundetTilNærmesteTier
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidrag
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.LøpendeBidragPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragspliktig
import no.nav.bidrag.transport.behandling.felles.grunnlag.byggSluttberegningBarnebidragDetaljer
import no.nav.bidrag.transport.behandling.felles.grunnlag.erPerson
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSamværsklasse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnSluttberegningIReferanser
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.tilPersonreferanse
import no.nav.bidrag.transport.behandling.vedtak.request.HentManuelleVedtakRequest
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakForStønad
import no.nav.bidrag.transport.felles.toCompactString
import no.nav.bidrag.transport.person.PersonStønad
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientResponseException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.collections.set

private val log = KotlinLogging.logger {}

@Service
@Import(Vedtaksfiltrering::class)
class HentLøpendeBidragService(private val vedtakService: VedtakService) {
    @Timed
    fun hentLøpendeBidragForBehandling(
        bpIdent: Personident,
        beregningsperiode: ÅrMånedsperiode,
        request: BidragsberegningOrkestratorRequestV2,
    ): LøpendeBidragOgBeregninger {
        try {
            // Finner personidenter for søknadsbarn som har full beregningsperiode (fra tidligste virkningstidspunkt i requesten),
            // og som det derfor ikke trengs å hentes løpende bidrag for.
            val barnMedFullBeregningsperiodeListe = request.beregningBarn
                .filter { it.beregningsperiode.fom == beregningsperiode.fom }
                .mapNotNull { beregningBarn ->
                    request.grunnlagsliste
                        .find {
                            it.type == Grunnlagstype.PERSON_SØKNADSBARN &&
                                it.referanse == beregningBarn.søknadsbarnreferanse
                        }
                        ?.innholdTilObjekt<Person>()
                        ?.ident
                        ?.let {
                            PersonStønad(
                                ident = it.verdi,
                                stønadstype = beregningBarn.stønadstype,
                            )
                        }
                }

            // Henter alle bidrag tilknyttet BP som er eller har vært løpende i beregningsperioden. Filtrerer først
            // bort perioder som er utenfor beregningsperioden. Stønader som ikke har noen perioder innenfor beregningsperioden fjernes.
            // Deretter fjernes løpende bidrag til barn som har full beregningsperiode. Må sjekke stønadstype i tillegg til ident for å håndetere
            // 18-årsbidrag (tilfeller hvor det er samme ident, men ulike stønadstyper).
            val løpendeBidragIPerioden = vedtakService.hentAlleStønaderForBidragspliktig(
                LøpendeBidragPeriodeRequest(bpIdent, beregningsperiode),
            ).filtrerForPeriode(beregningsperiode)
                .filterNot {
                    PersonStønad(ident = it.kravhaver.verdi, stønadstype = it.type) in barnMedFullBeregningsperiodeListe
                }

            secureLogger.info {
                "Hentet løpende bidrag i perioden: ${løpendeBidragIPerioden.joinToString { it.toString() }} " +
                    "for BP: ${bpIdent.verdi}"
            }

            if (løpendeBidragIPerioden.isEmpty()) {
                secureLogger.info { "Ingen løpende bidrag funnet i perioden for BP: ${bpIdent.verdi}. Returnerer tomt resultat." }
                return LøpendeBidragOgBeregninger(BidragBeregningResponsDto(emptyList()), emptyList())
            }

            val manuelleVedtakForLøpendeBidrag = vedtakService.finnAlleManuelleVedtakForBp(
                HentManuelleVedtakRequest(
                    skyldner = bpIdent,
                ),
            ).filter { vedtak ->
                løpendeBidragIPerioden.any {
                    it.sak == vedtak.stønadsendring.sak &&
                        it.type == vedtak.stønadsendring.type &&
                        it.kravhaver == vedtak.stønadsendring.kravhaver
                }
            }

            secureLogger.info {
                "Hentede manuelle vedtak filtrert mot løpende bidrag: ${
                    manuelleVedtakForLøpendeBidrag.joinToString {
                        it.toString()
                    }
                } for BP: ${bpIdent.verdi}"
            }

            val manuelleVedtakFiltrertMotBeregningsperiode = manuelleVedtakForLøpendeBidrag
                .sortedByDescending { it.vedtakstidspunkt }
                .groupBy {
                    Stønadsid(
                        type = it.stønadsendring.type,
                        kravhaver = it.stønadsendring.kravhaver,
                        skyldner = it.stønadsendring.skyldner,
                        sak = it.stønadsendring.sak,
                    )
                }
                .filtrerVedtakMotBeregningsperiode(beregningsperiode)

            val beregningsdataIManuelleVedtak = manuelleVedtakFiltrertMotBeregningsperiode.hentBeregning()
            secureLogger.info {
                "Hentede beregningsdata i manuelle vedtak: " +
                    beregningsdataIManuelleVedtak.beregningListe.joinToString { it.toString() }
            }

            return LøpendeBidragOgBeregninger(
                beregnetBeløpListe = beregningsdataIManuelleVedtak,
                løpendeBidragListe = løpendeBidragIPerioden,
            )
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved opprettelse av grunnlag for løpende bidrag for BP evnevurdering: ${e.message}" }
            throw e
        }
    }

    private fun List<VedtakForStønad>.hentBeregning(): BidragBeregningResponsDto {
        val hentBeregningFraBidragVedtakListe = mutableListOf<VedtakForStønad>()
        val hentBeregningFraBBMListe = mutableListOf<VedtakForStønad>()

        // Bestemmer hvilke vedtak som skal hentes fra bidrag-vedtak og hvilke som skal hentes fra BBM og lager en liste for hver
        map {
            if (it.behandlingsreferanser.any { it.kilde == BehandlingsrefKilde.BEHANDLING_ID }) {
                hentBeregningFraBidragVedtakListe.add(it)
            } else {
                hentBeregningFraBBMListe.add(it)
            }
        }

        // Henter beregningsgrunnlag fra BBM
        var bidragBeregningResponsDtoFraBBM = BidragBeregningResponsDto(emptyList())
        if (hentBeregningFraBBMListe.isNotEmpty()) {
            secureLogger.info { "Følgende beregninger skal hentes fra BBM: ${hentBeregningFraBBMListe.joinToString { it.toString() }}" }
            bidragBeregningResponsDtoFraBBM =
                vedtakService.hentAlleBeregningerFraBBM(hentBeregningFraBBMListe)

            secureLogger.info { "Respons fra BBM: $bidragBeregningResponsDtoFraBBM" }
        }

        // Henter beregningsgrunnlag fra bidrag-vedtak
        var bidragBeregningResponsDtoFraBidragVedtak = BidragBeregningResponsDto(emptyList())
        if (hentBeregningFraBidragVedtakListe.isNotEmpty()) {
            secureLogger.info {
                "Følgende beregninger skal hentes fra bidrag-vedtak: " +
                    hentBeregningFraBidragVedtakListe.joinToString { it.toString() }
            }
            val beregningListe = mutableListOf<BidragBeregningResponsDto.BidragBeregning>()

            hentBeregningFraBidragVedtakListe.forEach {
                secureLogger.info { "Behandler VedtakForStønad: $it" }
                val beregning = finnAlleBeregningerIBidragVedtak(it)
                if (beregning.isNotEmpty()) {
                    secureLogger.info { "Legger til følgende beregning for vedtak ${it.vedtaksid} i bidrag-vedtak: $beregning" }
                    beregningListe.addAll(beregning)
                }
            }
            bidragBeregningResponsDtoFraBidragVedtak = BidragBeregningResponsDto(beregningListe)
        }

        val respons = bidragBeregningResponsDtoFraBBM.beregningListe + bidragBeregningResponsDtoFraBidragVedtak.beregningListe

        // Returnerer sammenslått beregningsgrunnlag fra BBM og bidrag-vedtak
        return BidragBeregningResponsDto(
            respons.sortedWith(
                compareBy<BidragBeregningResponsDto.BidragBeregning>
                    { it.personidentBarn }.thenByDescending { it.periode?.fom },
            ),
        )
    }

    fun finnAlleBeregningerIBidragVedtak(vedtakForStønad: VedtakForStønad): List<BidragBeregningResponsDto.BidragBeregning> {
        // Henter vedtak fra bidrag-vedtak (med fullstendige opplysninger)
        val vedtakDto = vedtakService.hentVedtak(vedtakForStønad.vedtaksid)
        if (vedtakDto == null) {
            secureLogger.warn { "Fant ikke vedtak for vedtaksid ${vedtakForStønad.vedtaksid} i bidrag-vedtak." }
            return emptyList()
        }

        val bidragBeregningListe = mutableListOf<BidragBeregningResponsDto.BidragBeregning>()

        // Henter stønadsendringen fra vedtaket som matcher med det som ligger i VedtakForStønad
        val stønadsendringDto =
            vedtakDto.stønadsendringListe.firstOrNull { stønadsendringDto ->
                stønadsendringDto.type == vedtakForStønad.stønadsendring.type &&
                    stønadsendringDto.sak == vedtakForStønad.stønadsendring.sak &&
                    stønadsendringDto.skyldner == vedtakForStønad.stønadsendring.skyldner &&
                    stønadsendringDto.kravhaver == vedtakForStønad.stønadsendring.kravhaver
            }
        if (stønadsendringDto == null) {
            secureLogger.warn { "Fant ikke stønadsendring for vedtak ${vedtakForStønad.vedtaksid} i bidrag-vedtak." }
            return emptyList()
        }
        secureLogger.info { "Fant stønadsendring for vedtak ${vedtakForStønad.vedtaksid} i bidrag-vedtak: $stønadsendringDto" }

        stønadsendringDto.periodeListe.forEach { periode ->
            // Finner sluttberegning-grunnlaget
            val sluttberegningGrunnlag =
                vedtakDto.grunnlagListe.finnSluttberegningIReferanser(periode.grunnlagReferanseListe) ?: return@forEach

            val beregningsdetaljer = vedtakDto.grunnlagListe.byggSluttberegningBarnebidragDetaljer(periode.grunnlagReferanseListe)

            // Hent av samvær skal bare gjøres hvis barnet ikke er regnet som selvforsørget i perioden
            val hentSamvær = beregningsdetaljer?.barnetErSelvforsørget != true

            val samværsklasse = if (hentSamvær) vedtakDto.grunnlagListe.finnSamværsklasse(sluttberegningGrunnlag) else null

            bidragBeregningListe.add(
                BidragBeregningResponsDto.BidragBeregning(
                    periode = periode.periode,
                    saksnummer = vedtakForStønad.stønadsendring.sak.verdi,
                    personidentBarn = vedtakForStønad.stønadsendring.kravhaver,
                    datoSøknad = LocalDate.now(), // Brukes ikke
                    beregnetBeløp = beregningsdetaljer?.beregnetBeløp ?: BigDecimal.ZERO,
                    faktiskBeløp = beregningsdetaljer?.resultatBeløp ?: BigDecimal.ZERO,
                    beløpSamvær = BigDecimal.ZERO, // Brukes ikke
                    stønadstype = Stønadstype.BIDRAG,
                    samværsklasse = samværsklasse,
                    vedtaksid = vedtakForStønad.vedtaksid,
                    bidragJustertForNettoBarnetilleggBP = beregningsdetaljer?.bidragJustertForNettoBarnetilleggBP,
                    bruttoBidragEtterBarnetilleggBM = beregningsdetaljer?.bruttoBidragEtterBarnetilleggBM,
                    bruttoBidragEtterBarnetilleggBP = beregningsdetaljer?.bruttoBidragEtterBarnetilleggBP,
                    erVedtakKildeBBM = false,
                ),
            )
        }
        return bidragBeregningListe
    }

    private fun LøpendeBidragPeriodeResponse.filtrerForPeriode(beregningsperiode: ÅrMånedsperiode): List<LøpendeBidrag> = // Fjerner perioder som ikke overlapper med beregningsperioden
        bidragListe.mapNotNull { bidrag ->
            val beregningsperiodeTil = beregningsperiode.til
            val periodeListe = bidrag.periodeListe.filter {
                it.periode.overlapper(beregningsperiode) &&
                    it.periode.fom != beregningsperiode.til &&
                    it.periode.til != beregningsperiode.fom
            }.map { periode ->
                // Justerer periode.til til beregningsperiode.til hvis til er null eller etter beregningsperiode.til
                val periodeTil = periode.periode.til
                val justerTil = beregningsperiodeTil != null && (periodeTil == null || periodeTil.isAfter(beregningsperiodeTil))

                // Justerer periode.fom til beregningsperiode.fom hvis fom er før beregningsperiode.fom
                val justerFom = periode.periode.fom.isBefore(beregningsperiode.fom)

                if (justerFom || justerTil) {
                    val nyFom = if (justerFom) beregningsperiode.fom else periode.periode.fom
                    val nyTil = if (justerTil) beregningsperiodeTil else periodeTil
                    periode.copy(periode = periode.periode.copy(fom = nyFom, til = nyTil))
                } else {
                    periode
                }
            }
            if (periodeListe.isNotEmpty()) {
                LøpendeBidrag(
                    sak = bidrag.sak,
                    type = bidrag.type,
                    kravhaver = bidrag.kravhaver,
                    mottaker = bidrag.mottaker,
                    periodeListe = periodeListe,
                )
            } else {
                null
            }
        }
}

data class LøpendeBidragOgBeregninger(val beregnetBeløpListe: BidragBeregningResponsDto, val løpendeBidragListe: List<LøpendeBidrag>)

// Skal returnere alle vedtak som overlapper med beregningsperioden. Listen med vedtak er sortert på vedtakstidspunkt descending slik at
// nyeste kommer først.
fun Map<Stønadsid, List<VedtakForStønad>>.filtrerVedtakMotBeregningsperiode(beregningsperiode: ÅrMånedsperiode): List<VedtakForStønad> {
    val relevanteVedtakListe = mutableListOf<VedtakForStønad>()
    this.forEach { (_, vedtakListe) ->
        run {
            vedtakListe.forEach { vedtak ->
                if (vedtak.stønadsendring.periodeListe.firstOrNull()?.periode?.fom?.isBefore(beregningsperiode.fom.plusMonths(1)) == true) {
                    // vedtaket dekker starten av beregningsperioden og vi kan avslutte søket
                    relevanteVedtakListe.add(vedtak)
                    return@run
                }
                relevanteVedtakListe.add(vedtak)
            }
        }
    }
    return relevanteVedtakListe
}

fun LøpendeBidragOgBeregninger.tilBeregnGrunnlag(
    bpReferanse: String,
    søknadsbarnListe: List<PersonStønad>,
    løpendeBarnFødselsdatoMap: Map<Personident, LocalDate?>,
    personConsumer: BeregningPersonConsumer,
    request: BidragsberegningOrkestratorRequestV2,
    utlandssakerListe: List<String>,
): List<BeregnGrunnlag> {
    val beregnGrunnlagListe = mutableListOf<BeregnGrunnlag>()

    this.løpendeBidragListe.sortedBy { it.kravhaver }.forEach { løpendeBidrag ->
        val grunnlagListe = mutableListOf<GrunnlagDto>()

        val utlandssak = utlandssakerListe.contains(løpendeBidrag.sak.verdi)
        val oppfostringsbidrag = løpendeBidrag.type == Stønadstype.OPPFOSTRINGSBIDRAG

        var bMReferanse: String

        if (utlandssak || oppfostringsbidrag) {
            // Sjekker om referanse for BM ligger i grunnlaget, ellers oppretter vi en ny referanse. Egen logikk for oppfostring og utland siden
            // fødselsdato ikke ligger i PDL.
            bMReferanse = request.grunnlagsliste.find {
                it.personIdent == løpendeBidrag.mottaker?.verdi
            }?.referanse ?: Grunnlagstype.PERSON_BIDRAGSMOTTAKER.tilPersonreferanse(
                (løpendeBidrag.mottaker.hashCode().toString()),
                (løpendeBidrag.mottaker!!.verdi + 1).hashCode(),
            )
        } else {
            // For utlandssaker og oppfostringsbidrag skal det ikke hentes inn løpende bidrag, da disse ikke skal påvirke beregningen.
            // Det opprettes likevel et grunnlagsobjekt for å unngå feil i beregningen
            val bMFødselsdato = løpendeBidrag.mottaker?.let {
                try {
                    personConsumer.hentFødselsdatoForPerson(løpendeBidrag.mottaker!!)
                } catch (e: RestClientResponseException) {
                    if (e.statusCode == HttpStatus.FORBIDDEN) {
                        // Hvis SB ikke har tilgang til person så bør ikke det feile i løpende bidrag sjekken.
                        // Fødselsdato er ikke viktig å lagre for BM så ignorerer
                        return@let null
                    }
                    throw e
                }
            } ?: LocalDate.MAX

            // Sjekker om referanse for BM ligger i grunnlaget, ellers oppretter vi en ny referanse
            bMReferanse = request.grunnlagsliste.find {
                it.erPerson() && it.personIdent == løpendeBidrag.mottaker?.verdi
            }?.referanse ?: Grunnlagstype.PERSON_BIDRAGSMOTTAKER.tilPersonreferanse(
                (bMFødselsdato.toCompactString()),
                (løpendeBidrag.mottaker!!.verdi + 1).hashCode(),
            )
        }

        val grunnlagsobjektBP = request.grunnlagsliste.bidragspliktig as GrunnlagDto

        // Henter referanser for barn som er søknadsbarn. Andre barn (ikke søknadsbarn) vil få generert egen referanse
        val barnetErSøknadsbarn = PersonStønad(ident = løpendeBidrag.kravhaver.verdi, stønadstype = løpendeBidrag.type) in søknadsbarnListe

        val søknadsbarnRef: String? = if (barnetErSøknadsbarn) {
            request.grunnlagsliste
                .firstOrNull { it.type == Grunnlagstype.PERSON_SØKNADSBARN && it.personIdent == løpendeBidrag.kravhaver.verdi }
                ?.referanse
        } else {
            null
        }

        // For barn som ikke er søknadsbarn skal det ikke gjøres noen avgrensning av løpendeBidragsperioder.
        val beregningsperiodeBarn = if (!barnetErSøknadsbarn) {
            null
        } else {
            // For søknadsbarn så skal det bare returneres løpende bidrag utenfor barnets beregningsperiode.
            request.beregningBarn.firstOrNull { it.søknadsbarnreferanse == søknadsbarnRef }?.beregningsperiode
        }
        var løpendeBarnReferanse: String? = null
        if (!barnetErSøknadsbarn) {
            // barnet er ikke søknadsbarn, hent fødselsdato fra løpendeBarnFødselsdatoMap
            // Sjekker om barnet ligger som grunnlagsobjekt fra før. Hvis ikke så skal det genereres et grunnlagsobjekt for barnet.
            val grunnlagsobjektKravhaverFraInput = request.grunnlagsliste.find {
                it.erPerson() && it.personIdent == løpendeBidrag.kravhaver.verdi
            }
            val fødselsdato = løpendeBarnFødselsdatoMap[løpendeBidrag.kravhaver]
            løpendeBarnReferanse = grunnlagsobjektKravhaverFraInput?.referanse ?: Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG.tilPersonreferanse(
                (fødselsdato.toCompactString() + "_innhentet"),
                (løpendeBidrag.kravhaver.verdi + 1).hashCode(),
            )

            val grunnlagsobjektKravhaver = grunnlagsobjektKravhaverFraInput
                ?: GrunnlagDto(
                    referanse = løpendeBarnReferanse,
                    gjelderReferanse = bpReferanse,
                    gjelderBarnReferanse = null,
                    type = Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG,
                    innhold = POJONode(
                        Person(
                            ident = løpendeBidrag.kravhaver,
                            navn = null,
                            fødselsdato = fødselsdato ?: LocalDate.parse("2021-01-01"),
                            bidragsmottaker = bMReferanse,
                            delAvOpprinneligBehandling = false,
                        ),
                    ),
                )

            grunnlagListe.add(grunnlagsobjektKravhaver)
        }

        // Sjekker om det er angitt en opphørsdato for barnet og bruker eventuelt denne til å begrense løpende bidrag. Hvis ikke så brukes fom-dato i
        // barnets beregningsperiode.
        val opphørsdato = request.beregningBarn.firstOrNull { it.søknadsbarnreferanse == søknadsbarnRef }?.opphørsdato

        val begrensTilÅrMåned = when {
            opphørsdato != null && (beregningsperiodeBarn?.fom == null || opphørsdato.isBefore(beregningsperiodeBarn.fom)) -> opphørsdato
            else -> beregningsperiodeBarn?.fom
        }

        var justertLøpendeBidragFørsteFom: YearMonth? = null
        var justertLøpendeBidragSisteTil: YearMonth? = null

        løpendeBidrag.periodeListe.forEach { løpendeBidragPeriode ->
            // Sjekker løpendeBidragPerioder mot beregningsperiodeBarn. Perioder som helt overlapper beregningsperiodeBarn skal ignoreres
            // og perioder som delvis overlapper skal avkortes slik at det ikke finnes doble perioder.
            val justertPeriode = if (!barnetErSøknadsbarn) {
                løpendeBidragPeriode.periode
            } else {
                justerPeriodeMotBegrensTilÅrMåned(
                    periode = løpendeBidragPeriode.periode,
                    begrensTilÅrMåned = begrensTilÅrMåned,
                )
            }

            // Fortsett kun hvis det finnes justert periode
            if (justertPeriode == null) return@forEach

            if (justertLøpendeBidragFørsteFom == null) {
                justertLøpendeBidragFørsteFom = justertPeriode.fom
            }
            justertLøpendeBidragSisteTil = justertPeriode.til

            // Finner riktige beregningsdata fra vedtak. Utlandsvedtak og oppfostringsvedtak vil ikke ha beregningsdata.
            val beregning: BidragBeregningResponsDto.BidragBeregning? = if (utlandssak || oppfostringsbidrag) {
                null
            } else {
                this.beregnetBeløpListe.beregningListe
                    .asSequence()
                    .filter { it.personidentBarn == løpendeBidrag.kravhaver }
                    .filter { it.stønadstype == løpendeBidrag.type }
                    .filter { it.saksnummer == løpendeBidrag.sak.verdi }
                    .sortedByDescending { it.periode?.fom }
                    .find {
                        it.periode!!.fom.isBefore(løpendeBidragPeriode.periode.fom.plusMonths(1))
                    } ?: this.beregnetBeløpListe.beregningListe.lastOrNull()
            }
            // hvis manuelt vedtak ikke har matchende periode så må seneste periode brukes

            // Opprett grunnlag for hver justerte periode
            val gjelderBarnReferanse = søknadsbarnRef ?: løpendeBarnReferanse

            grunnlagListe.add(
                GrunnlagDto(
                    referanse = "innhentet_løpende_bidrag_${bpReferanse}_$gjelderBarnReferanse" +
                        "_${justertPeriode.fom.toCompactString()}${
                            justertPeriode.til?.let {
                                "_${it.toCompactString()}"
                            } ?: ""
                        }",
                    gjelderReferanse = bpReferanse,
                    gjelderBarnReferanse = gjelderBarnReferanse,
                    type = Grunnlagstype.LØPENDE_BIDRAG_PERIODE,
                    innhold = POJONode(
                        LøpendeBidragPeriode(
                            periode = justertPeriode,
                            saksnummer = Saksnummer(løpendeBidrag.sak.verdi),
                            stønadstype = løpendeBidrag.type,
                            løpendeBeløp = løpendeBidragPeriode.løpendeBeløp,
                            valutakode = Valutakode.valueOf(løpendeBidragPeriode.valutakode),
                            samværsklasse = beregning?.samværsklasse,
                            beregnetBeløp = beregning?.beregnetBeløp?.avrundetTilNærmesteTier ?: løpendeBidragPeriode.løpendeBeløp,
                            faktiskBeløp = beregning?.faktiskBeløp ?: løpendeBidragPeriode.løpendeBeløp,
                            sakskategori = if (utlandssak) Sakskategori.UTLAND else Sakskategori.NASJONAL,
                            vedtaksid = beregning?.vedtaksid,
                            bidragJustertForNettoBarnetilleggBP = beregning?.bidragJustertForNettoBarnetilleggBP,
                            bruttoBidragEtterBarnetilleggBM = beregning?.bruttoBidragEtterBarnetilleggBM,
                            bruttoBidragEtterBarnetilleggBP = beregning?.bruttoBidragEtterBarnetilleggBP,
                            erVedtakKildeBBM = beregning?.erVedtakKildeBBM ?: false,
                        ),
                    ),
                ),
            )
        }

        if (grunnlagListe.any { it.type == Grunnlagstype.LØPENDE_BIDRAG_PERIODE }) {
            grunnlagListe.add(grunnlagsobjektBP)

            beregnGrunnlagListe.add(
                BeregnGrunnlag(
                    periode = ÅrMånedsperiode(
                        fom = justertLøpendeBidragFørsteFom!!,
                        til = justertLøpendeBidragSisteTil,
                    ),
                    opphørsdato = null,
                    stønadstype = løpendeBidrag.type,
                    søknadsbarnReferanse = søknadsbarnRef ?: løpendeBarnReferanse!!,
                    grunnlagListe = grunnlagListe,
                ),
            )
            if (barnetErSøknadsbarn) {
                val grunnlagsobjektSøknadsbarn = request.grunnlagsliste.first {
                    it.type == Grunnlagstype.PERSON_SØKNADSBARN &&
                        it.referanse == søknadsbarnRef
                }
                grunnlagListe.add(grunnlagsobjektSøknadsbarn)
            }
        }
    }
    return beregnGrunnlagListe
}

/**
 * Justerer en periode med løpende bidrag mot år og måned som løpende bidrag skal begrenses til. BegrensTilÅrMåned er enten fomdato i barnets
 * beregningsperiode eller opphørsdato.
 * - Perioder som starter etter begrensTilÅrMåned ignoreres (returnerer tom liste)
 * - Perioder som delvis overlapper avkortes slik at de ikke overlapper
 * - Perioder uten overlapp returneres uendret
 *
 * @return Liste med justerte perioder (kan være tom, én eller to perioder)
 */
private fun justerPeriodeMotBegrensTilÅrMåned(periode: ÅrMånedsperiode, begrensTilÅrMåned: YearMonth?): ÅrMånedsperiode? {
    // Hvis beregningsperiodeBarn er null er barnet ikke et søknadsbarn
    if (begrensTilÅrMåned == null) {
        return periode
    }

    // Sjekk om perioden helt overlapper med beregningsperiodeBarn (skal ignoreres)
    if (erHeltInnenfor(periode.fom, begrensTilÅrMåned)) {
        return null
    }

    // Sjekk om det er noen overlapp
    if (!harOverlapp(periode.til, begrensTilÅrMåned)) {
        return periode
    }

    // Delvis overlapp - avkort perioden
    var resultat: ÅrMånedsperiode? = null

    // Del før begrensTilÅrMåned
    if (periode.fom.isBefore(begrensTilÅrMåned)) {
        resultat = ÅrMånedsperiode(periode.fom, begrensTilÅrMåned)
    }

    return resultat
}

/**
 * Sjekker om en periode starter samtidig som eller etter begrensTilÅrMåned. Skal da ikke returneres
 */
private fun erHeltInnenfor(periodeFom: YearMonth, begrensTilÅrMåned: YearMonth): Boolean = !periodeFom.isBefore(begrensTilÅrMåned)

/**
 * Sjekker om perioden overlapper med begrensTilÅrMåned. Overlapp hvis periodeTil er null eller etter begrensTilÅrMåned.
 */
private fun harOverlapp(periodeTil: YearMonth?, begrensTilÅrMåned: YearMonth): Boolean = periodeTil == null || periodeTil.isAfter(begrensTilÅrMåned)
