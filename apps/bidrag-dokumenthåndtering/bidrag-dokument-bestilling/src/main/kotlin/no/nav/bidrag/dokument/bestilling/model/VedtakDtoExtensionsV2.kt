package no.nav.bidrag.dokument.bestilling.model

import no.nav.bidrag.dokument.bestilling.tjenester.hentInnslagKapitalinntekt
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.BaseGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.BostatusPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBarnIHusstand
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumInntekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.Grunnlagsreferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.InntektsrapporteringPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.SivilstandPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SjablonSjablontallPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SøknadGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.VirkningstidspunktGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.bidragsmottaker
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.finnGrunnlagSomErReferertFraGrunnlagsreferanseListe
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPerson
import no.nav.bidrag.transport.behandling.felles.grunnlag.hentPersonMedReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.innholdTilObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import no.nav.bidrag.transport.behandling.felles.grunnlag.personObjekt
import no.nav.bidrag.transport.behandling.felles.grunnlag.søknadsbarn
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakPeriodeDto
import no.nav.bidrag.transport.dokumentmaler.BarnIHusstandPeriode
import no.nav.bidrag.transport.dokumentmaler.InntektPeriode
import no.nav.bidrag.transport.dokumentmaler.VedtakPeriodeReferanse
import no.nav.bidrag.transport.dokumentmaler.VedtakSaksbehandlerInfo
import java.math.BigDecimal
import java.time.LocalDate

val kapitalinntektTyper = listOf(Inntektsrapportering.KAPITALINNTEKT, Inntektsrapportering.KAPITALINNTEKT_EGNE_OPPLYSNINGER)

fun List<Person>.inneholder(ident: Personident) = any { it.ident == ident }

fun VedtakDto.tilSaksbehandler() =
    VedtakSaksbehandlerInfo(
        navn = opprettetAvNavn ?: "",
        ident = opprettetAv,
    )

fun VedtakDto.hentEldsteVirkningstidspunkt(): LocalDate? =
    grunnlagListe
        .filtrerBasertPåEgenReferanse(Grunnlagstype.VIRKNINGSTIDSPUNKT)
        .map { it.innholdTilObjekt<VirkningstidspunktGrunnlag>().virkningstidspunkt }
        .minOfOrNull { it }

fun VedtakDto.hentVirkningstidspunktIkkeFF(søknadsbarn: String? = null): VirkningstidspunktGrunnlag? {
    val søknadsbarnReferanse = søknadsbarn?.let { grunnlagListe.hentPerson(søknadsbarn)?.referanse }
    return grunnlagListe
        .filtrerBasertPåEgenReferanse(Grunnlagstype.VIRKNINGSTIDSPUNKT)
        .filter { søknadsbarnReferanse == null || søknadsbarnReferanse == it.gjelderBarnReferanse }
        .map { it.innholdTilObjekt<VirkningstidspunktGrunnlag>() }
        .firstOrNull { it.årsak != VirkningstidspunktÅrsakstype.REVURDERING_MÅNEDEN_ETTER }
}

fun VedtakDto.hentSøknad(): SøknadGrunnlag =
    grunnlagListe
        .filtrerBasertPåEgenReferanse(Grunnlagstype.SØKNAD)
        .firstOrNull()
        ?.innholdTilObjekt<SøknadGrunnlag>() ?: SøknadGrunnlag(
        mottattDato = vedtakstidspunkt?.toLocalDate() ?: opprettetTidspunkt.toLocalDate(),
        søktFraDato = vedtakstidspunkt?.toLocalDate() ?: opprettetTidspunkt.toLocalDate(),
        søktAv = SøktAvType.NAV_BIDRAG,
    )

fun List<GrunnlagDto>.mapSivilstand(): List<SivilstandPeriode> =
    filtrerBasertPåEgenReferanse(Grunnlagstype.SIVILSTAND_PERIODE)
        .map { it.innholdTilObjekt<SivilstandPeriode>() }
        .sammenstillSivilstand()

fun List<GrunnlagDto>.mapHusstandsbarn(): List<Husstandsbarn> =
    filtrerBasertPåEgenReferanse(Grunnlagstype.BOSTATUS_PERIODE)
        .groupBy { if (it.gjelderBarnReferanse.isNullOrEmpty()) it.gjelderReferanse else it.gjelderBarnReferanse }
        .map { (gjelderPersonReferanse, grunnlag) ->
            val person = hentPersonMedReferanse(gjelderPersonReferanse) ?: throw RuntimeException("Mangler person grunnlag for referanse $gjelderPersonReferanse")
            Husstandsbarn(
                person.personObjekt,
                grunnlag.innholdTilObjekt<BostatusPeriode>(),
            )
        }

fun List<GrunnlagDto>.mapBarnIHusstandPerioder(): List<BarnIHusstandPeriode> {
    val barnIHusstand =
        filtrerBasertPåEgenReferanse(Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND)
            .map { it.innholdTilObjekt<DelberegningBarnIHusstand>() }

    return barnIHusstand
        .sortedBy { it.periode.fom }
        .map {
            BarnIHusstandPeriode(
                it.periode,
                it.antallBarn,
            )
        }.sammenstillBarnIHusstandPerioder()
}

fun List<SivilstandPeriode>.sammenstillSivilstand(): List<SivilstandPeriode> =
    sortedBy { it.periode.fom }
        .fold(mutableListOf()) { result, next ->
            val current = result.lastOrNull()
            if (current != null && current.sivilstand == next.sivilstand) {
                result[result.lastIndex] =
                    SivilstandPeriode(
                        ÅrMånedsperiode(current.periode.fom, maxOfNullable(current.periode.til, next.periode.til)),
                        current.sivilstand,
                        current.manueltRegistrert,
                    )
            } else {
                result.add(next)
            }
            result
        }

fun List<BarnIHusstandPeriode>.sammenstillBarnIHusstandPerioder(): List<BarnIHusstandPeriode> =
    sortedBy { it.periode.fom }
        .fold(mutableListOf()) { result, next ->
            val current = result.lastOrNull()
            if (current != null && current.antall == next.antall) {
                result[result.lastIndex] =
                    BarnIHusstandPeriode(
                        ÅrMånedsperiode(current.periode.fom, maxOfNullable(next.periode.til, current.periode.til)),
                        current.antall,
                    )
            } else {
                result.add(next)
            }
            result
        }

fun List<GrunnlagDto>.hentBarnIHusstandPerioderForBarn(ident: String): Husstandsbarn? = mapHusstandsbarn().find { it.gjelderBarn.ident?.verdi == ident }

// fun List<GrunnlagDto>.hentBosstatusForPeriode(ident: String, vedtakPeriode: VedtakPeriodeDto): Bostatuskode {
//    return mapHusstandsbarn().find { it.gjelderBarn.ident?.verdi == ident && it. }!!
// }
fun List<GrunnlagDto>.hentDelberegningBarnIHusstand(periode: VedtakPeriodeDto): BaseGrunnlag? {
    val sluttberegning = filtrerBasertPåEgenReferanser(Grunnlagstype.SLUTTBEREGNING_FORSKUDD, periode.grunnlagReferanseListe).firstOrNull() ?: return null
    return filtrerBasertPåEgenReferanser(Grunnlagstype.DELBEREGNING_BARN_I_HUSSTAND, sluttberegning.grunnlagsreferanseListe).firstOrNull()
}

fun List<GrunnlagDto>.hentDelberegningBarnIHusstandInnhold(periode: VedtakPeriodeDto): DelberegningBarnIHusstand? = hentDelberegningBarnIHusstand(periode)?.innholdTilObjekt<DelberegningBarnIHusstand>()

fun List<GrunnlagDto>.hentDelberegningInntektForPeriode(
    periode: VedtakPeriodeReferanse,
): Set<BaseGrunnlag> {
    val sluttberegning =
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.SLUTTBEREGNING_FORSKUDD, periode.grunnlagReferanseListe).firstOrNull()
            ?: finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.SLUTTBEREGNING_SÆRBIDRAG, periode.grunnlagReferanseListe).firstOrNull()
            ?: finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG, periode.grunnlagReferanseListe).firstOrNull() ?: return emptySet()

    if (sluttberegning.type == Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG) {
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.DELBEREGNING_BIDRAGSPLIKTIGES_ANDEL, sluttberegning.grunnlagsreferanseListe)
            .let {
                if (it.isNotEmpty()) {
                    return finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, it.first().grunnlagsreferanseListe)
                } else {
                    return emptySet()
                }
            }
    } else {
        return finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, sluttberegning.grunnlagsreferanseListe)
    }
}

fun List<GrunnlagDto>.hentInntekterForPeriode(
    periode: VedtakPeriodeReferanse,
    person: BaseGrunnlag? = null,
): List<BaseGrunnlag> {
    val delberegningInntekt = hentDelberegningInntektForPeriode(periode)
    return filtrerBasertPåEgenReferanse(Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE).filter { inntekt ->
        delberegningInntekt.any { it.grunnlagsreferanseListe.contains(inntekt.referanse) } && (person == null || inntekt.gjelderReferanse == person.referanse)
    }
}

fun List<GrunnlagDto>.hentKapitalinntekterForPeriode(
    periode: VedtakPeriodeReferanse,
    rolle: BaseGrunnlag? = null,
): Map<Grunnlagsreferanse, List<InntektsrapporteringPeriode>> =
    hentInntekterForPeriode(periode, rolle)
        .groupBy { it.gjelderReferanse }
        .map { (gjelderReferanse, grunnlag) -> gjelderReferanse to grunnlag.innholdTilObjekt<InntektsrapporteringPeriode>().filter { kapitalinntektTyper.contains(it.inntektsrapportering) } }
        .associate { it.first!! to it.second }

fun List<InntektsrapporteringPeriode>.totalKapitalinntekt(): BigDecimal =
    filter { kapitalinntektTyper.contains(it.inntektsrapportering) }
        .map { it.beløp }
        .reduceOrNull { acc, num -> acc + num } ?: BigDecimal.ZERO

fun List<GrunnlagDto>.hentNettoKapitalinntektForRolle(
    vedtakPeriodeDto: VedtakPeriodeReferanse,
    innteksgrense: BigDecimal,
): List<InntektPeriode> =
    hentKapitalinntekterForPeriode(vedtakPeriodeDto)
        .mapNotNull { (gjelderReferanse, grunnlag) ->
            grunnlag
                .totalKapitalinntekt()
                .takeIf { it > BigDecimal.ZERO }
                ?.let { totalBelop ->
                    val gjelderGrunnlag = hentPersonMedReferanse(gjelderReferanse)!!
                    val rollePersonInfo = gjelderGrunnlag.personObjekt
                    val innslagKapitalInntektSjablonVerdi =
                        finnSjablonMedType(SjablonTallNavn.INNSLAG_KAPITALINNTEKT_BELØP)?.verdi ?: hentInnslagKapitalinntekt(vedtakPeriodeDto.periode.tilLocalDateTil())
                    val nettoKapitalinntekt = totalBelop - innslagKapitalInntektSjablonVerdi
                    nettoKapitalinntekt.takeIf { it > BigDecimal.ZERO }?.let {
                        InntektPeriode(
                            periode = vedtakPeriodeDto.periode,
                            nettoKapitalInntekt = true,
                            rolle = gjelderGrunnlag.type.tilRolletype(),
                            fødselsnummer = rollePersonInfo.ident!!.verdi,
                            beløp = it,
                            innteksgrense = innteksgrense,
                        )
                    }
                }
        }

data class Husstandsbarn(
    val gjelderBarn: Person,
    val bostatus: List<BostatusPeriode>,
)

fun List<BaseGrunnlag>.finnGrunnlagMedType(
    type: Grunnlagstype,
    fraGrunnlag: BaseGrunnlag,
): BaseGrunnlag? {
    val grunnlag = filtrerBasertPåEgenReferanser(type, fraGrunnlag.grunnlagsreferanseListe)
    if (grunnlag.isEmpty()) {
        fraGrunnlag.grunnlagsreferanseListe.filter {
            val nesteGrunnlag = filtrerBasertPåEgenReferanse(null, it)
            return finnGrunnlagMedType(type, nesteGrunnlag.first())
        }
    }
    return null
}

fun List<BaseGrunnlag>.finnSjablonMedType(
    type: SjablonTallNavn,
    referanser: List<Grunnlagsreferanse>? = null,
): SjablonSjablontallPeriode? =
    if (referanser != null) {
        finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(Grunnlagstype.SJABLON_SJABLONTALL, referanser)
            .map { it.innholdTilObjekt<SjablonSjablontallPeriode>() }
            .find { it.sjablon == type }
    } else {
        filtrerBasertPåEgenReferanse(Grunnlagstype.SJABLON_SJABLONTALL)
            .map { it.innholdTilObjekt<SjablonSjablontallPeriode>() }
            .find { it.sjablon == type }
    }

fun List<BaseGrunnlag>.filtrerBasertPåEgenReferanser(
    type: Grunnlagstype,
    referanser: List<Grunnlagsreferanse>,
): List<BaseGrunnlag> =
    filtrerBasertPåEgenReferanse(type)
        .filter { referanser.contains(it.referanse) }

fun Grunnlagstype.tilRolletype() =
    when (this) {
        Grunnlagstype.PERSON_BIDRAGSPLIKTIG -> Rolletype.BIDRAGSPLIKTIG
        Grunnlagstype.PERSON_SØKNADSBARN -> Rolletype.BARN
        Grunnlagstype.PERSON_BIDRAGSMOTTAKER -> Rolletype.BIDRAGSMOTTAKER
        Grunnlagstype.PERSON_REELL_MOTTAKER -> Rolletype.REELMOTTAKER
        else -> throw RuntimeException("Mangler grunnlagsmapping for rolletype $this")
    }

fun ÅrMånedsperiode.tilLocalDateFom() = fom.atDay(1)

fun ÅrMånedsperiode?.tilLocalDateTil() = this?.til?.atEndOfMonth()

fun List<GrunnlagDto>.hentTotalInntektForPeriode(
    vedtakPeriode: VedtakPeriodeReferanse,
    innteksgrense: BigDecimal,
): List<InntektPeriode> =
    hentDelberegningInntektForPeriode(vedtakPeriode).groupBy { it.gjelderReferanse }.flatMap { (gjelderReferanse, inntektPeriode) ->
//        val førsteInntekt = filtrerBasertPåFremmedReferanse(Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE, inntektPeriode.grunnlagsreferanseListe).firstOrNull()
    val delberegningInntekt = inntektPeriode.first().innholdTilObjekt<DelberegningSumInntekt>()
    val gjelderPersonGrunnlag = if (gjelderReferanse == null && vedtakPeriode.typeBehandling == TypeBehandling.FORSKUDD) bidragsmottaker!! else hentPersonMedReferanse(gjelderReferanse)!!
    listOf(
        InntektPeriode(
            periode = vedtakPeriode.periode,
            beløp = delberegningInntekt.totalinntekt,
            periodeTotalinntekt = true,
            beløpÅr = vedtakPeriode.periode.fom.year,
            rolle = gjelderPersonGrunnlag.type.tilRolletype(),
            fødselsnummer = gjelderPersonGrunnlag.personIdent,
            innteksgrense = innteksgrense,
        ),
    )
}

internal fun List<GrunnlagDto>.hentSøknader(gjelderReferanse: String? = null): List<SøknadGrunnlag> =
    filtrerBasertPåEgenReferanse(Grunnlagstype.SØKNAD)
        .filter {
            gjelderReferanse.isNullOrEmpty() || it.gjelderBarnReferanse == gjelderReferanse || it.gjelderReferanse == gjelderReferanse
        }.map { it.innholdTilObjekt<SøknadGrunnlag>() }

internal fun List<GrunnlagDto>.hentSøknadsbarnForSøknad(søknadsid: Long? = null): List<BaseGrunnlag> =
    søknadsbarn
        .filter {
            if (søknadsid != null) {
                val søknader = hentSøknader(it.referanse)
                søknader.any { søknad -> søknad.søknadsid == søknadsid }
            } else {
                true
            }
        }.takeIf { it.isNotEmpty() } ?: søknadsbarn.map { it }

fun <T> T?.toList() = this?.let { listOf(it) } ?: emptyList()

fun <T> T?.toSet() = this?.let { setOf(it) } ?: emptySet()

fun <T : Comparable<T>> maxOfNullable(
    a: T?,
    b: T?,
): T? =
    if (a == null && b == null) {
        null
    } else if (a == null) {
        b
    } else if (b == null) {
        a
    } else {
        maxOf(a, b)
    }
