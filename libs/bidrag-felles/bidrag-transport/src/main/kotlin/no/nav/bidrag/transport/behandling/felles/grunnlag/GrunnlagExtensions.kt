package no.nav.bidrag.transport.behandling.felles.grunnlag

import com.fasterxml.jackson.databind.node.POJONode
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåFremmedReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper

val Grunnlagstype.erIndeksEllerAldersjustering get() =
    listOf(
        Grunnlagstype.SLUTTBEREGNING_INDEKSREGULERING,
        Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG_ALDERSJUSTERING,
    ).contains(this)

inline fun <reified T : List<GrunnlagInnhold>> BaseGrunnlag.innholdTilObjektListe(): T = try {
    commonObjectmapper.treeToValue(innhold)
} catch (e: Exception) {
    commonObjectmapper.readValue(commonObjectmapper.writeValueAsString(innhold))
}

inline fun <reified T : GrunnlagInnhold> BaseGrunnlag.innholdTilObjekt(): T = try {
    commonObjectmapper.treeToValue(innhold)
} catch (e: Exception) {
    commonObjectmapper.readValue(commonObjectmapper.writeValueAsString(innhold))
}

inline fun <reified T : GrunnlagInnhold> List<BaseGrunnlag>.innholdTilObjekt(): List<T> = map(BaseGrunnlag::innholdTilObjekt)

inline fun <reified T : List<GrunnlagInnhold>> List<BaseGrunnlag>.innholdTilObjektListe(): List<T> = map(BaseGrunnlag::innholdTilObjektListe)

fun List<BaseGrunnlag>.finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(
    type: Grunnlagstype,
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
): Set<BaseGrunnlag> {
    val grunnlag = filtrerBasertPåEgenReferanser(type, grunnlagsreferanseListe)

    // Traverser også rekursivt gjennom alle refererte grunnlag for å finne transitive referanser
    val indirekteGrunnlag =
        grunnlagsreferanseListe
            .flatMap { referanse ->
                filtrerBasertPåEgenReferanse(null, referanse)
                    .flatMap {
                        finnGrunnlagSomErReferertAv(type, it)
                    }
            }

    return (grunnlag + indirekteGrunnlag).toSet()
}

fun List<BaseGrunnlag>.finnGrunnlagSomErReferertAv(
    type: Grunnlagstype,
    fraGrunnlag: BaseGrunnlag,
): Set<BaseGrunnlag> {
    val grunnlag = filtrerBasertPåEgenReferanser(type, fraGrunnlag.grunnlagsreferanseListe)

    // Traverser også rekursivt gjennom alle refererte grunnlag for å finne transitive referanser
    val indirekteGrunnlag =
        fraGrunnlag.grunnlagsreferanseListe
            .flatMap { referanse ->
                filtrerBasertPåEgenReferanse(null, referanse)
                    .flatMap {
                        finnGrunnlagSomErReferertAv(type, it)
                    }
            }

    return (grunnlag + indirekteGrunnlag).toSet()
}

fun List<BaseGrunnlag>.filtrerBasertPåEgenReferanser(
    type: Grunnlagstype,
    referanser: List<Grunnlagsreferanse>,
): List<BaseGrunnlag> = filtrerBasertPåEgenReferanse(type)
    .filter { referanser.contains(it.referanse) }

fun List<BaseGrunnlag>.filtrerBasertPåFremmedReferanse(
    grunnlagType: Grunnlagstype? = null,
    referanse: String? = null,
    gjelderBarnReferanse: String? = null,
): List<BaseGrunnlag> = filter { grunnlagType == null || it.type == grunnlagType }
    .filter {
        (
            referanse.isNullOrEmpty() &&
                gjelderBarnReferanse.isNullOrEmpty()
            ) ||
            it.grunnlagsreferanseListe.contains(referanse) ||
            (
                (referanse.isNullOrEmpty() || (referanse == it.gjelderReferanse)) &&
                    (gjelderBarnReferanse.isNullOrEmpty() || (gjelderBarnReferanse == it.gjelderBarnReferanse))
                )
    }

fun List<BaseGrunnlag>.filtrerBasertPåEgenReferanse(
    grunnlagType: Grunnlagstype? = null,
    referanse: String = "",
): List<BaseGrunnlag> = filter { grunnlagType == null || it.type == grunnlagType }
    .filter { referanse.isEmpty() || referanse == it.referanse }

fun List<BaseGrunnlag>.hentInntekter(): List<InntektsrapporteringPeriode> = filtrerBasertPåEgenReferanse(
    Grunnlagstype.INNTEKT_RAPPORTERING_PERIODE,
).innholdTilObjekt()

data class InnholdMedReferanse<T>(
    val referanse: String,
    val gjelderBarnReferanse: String? = null,
    val gjelderReferanse: String? = null,
    val innhold: T,
    val grunnlag: BaseGrunnlag,
)

inline fun <reified T : GrunnlagInnhold> List<BaseGrunnlag>.finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe(
    type: Grunnlagstype,
    grunnlagsreferanseListe: List<Grunnlagsreferanse>,
): List<InnholdMedReferanse<T>> = finnGrunnlagSomErReferertFraGrunnlagsreferanseListe(type, grunnlagsreferanseListe).map {
    it.tilInnholdMedReferanse<T>()
}

inline fun <reified T : GrunnlagInnhold> List<BaseGrunnlag>.finnOgKonverterGrunnlagSomErReferertAv(
    type: Grunnlagstype,
    fraGrunnlag: BaseGrunnlag,
): List<InnholdMedReferanse<T>> = finnGrunnlagSomErReferertAv(type, fraGrunnlag).map {
    it.tilInnholdMedReferanse<T>()
}

inline fun <reified T : GrunnlagInnhold> List<BaseGrunnlag>.filtrerOgKonverterBasertPåEgenReferanse(
    grunnlagType: Grunnlagstype? = null,
    referanse: String = "",
): List<InnholdMedReferanse<T>> = filtrerBasertPåEgenReferanse(grunnlagType, referanse)
    .map {
        it.tilInnholdMedReferanse<T>()
    }

inline fun <reified T : GrunnlagInnhold> List<BaseGrunnlag>.filtrerOgKonverterBasertPåFremmedReferanse(
    grunnlagType: Grunnlagstype? = null,
    referanse: String? = null,
    gjelderBarnReferanse: String? = null,
): List<InnholdMedReferanse<T>> = filtrerBasertPåFremmedReferanse(grunnlagType, referanse, gjelderBarnReferanse)
    .map {
        it.tilInnholdMedReferanse<T>()
    }

fun Rolletype.tilGrunnlagstype() = when (this) {
    Rolletype.BIDRAGSPLIKTIG -> Grunnlagstype.PERSON_BIDRAGSPLIKTIG
    Rolletype.BARN -> Grunnlagstype.PERSON_SØKNADSBARN
    Rolletype.BIDRAGSMOTTAKER -> Grunnlagstype.PERSON_BIDRAGSMOTTAKER
    Rolletype.REELMOTTAKER -> Grunnlagstype.PERSON_REELL_MOTTAKER
    else -> throw RuntimeException("Mangler grunnlagsmapping for rolletype $this")
}

// Hjelpefunksjoner for personobjekter

fun Collection<BaseGrunnlag>.hentAllePersoner(): Collection<BaseGrunnlag> = filter { it.erPerson() }

fun BaseGrunnlag.erPerson(): Boolean = type.name.startsWith("PERSON_")

val BaseGrunnlag.personObjekt get() = commonObjectmapper.treeToValue(innhold, Person::class.java)!!
val BaseGrunnlag.personIdent get() = personObjekt.ident?.verdi
val BaseGrunnlag.stønadstype get() = personObjekt.stønadstype
val BaseGrunnlag.erRevurderingsbarn get() = !personObjekt.delAvOpprinneligBehandling
val Collection<BaseGrunnlag>.bidragspliktig
    get() =
        find { it.type == Grunnlagstype.PERSON_BIDRAGSPLIKTIG }

val Collection<BaseGrunnlag>.bidragsmottaker
    get() =
        find { it.type == Grunnlagstype.PERSON_BIDRAGSMOTTAKER }
val Collection<BaseGrunnlag>.barn
    get() =
        filter {
            it.type == Grunnlagstype.PERSON_SØKNADSBARN || it.type == Grunnlagstype.PERSON_HUSSTANDSMEDLEM
        }.toSet()

val BaseGrunnlag.bidragsmottakerReferanse get() = personObjekt.bidragsmottaker

val Collection<BaseGrunnlag>.søknadsbarn
    get() =
        filter {
            it.type == Grunnlagstype.PERSON_SØKNADSBARN
        }.toSet()

fun Collection<GrunnlagDto>.hentPerson(
    ident: String?,
    stønadstype: Stønadstype? = null,
) = ident?.let {
    filter { it.erPerson() }.find {
        it.personIdent == ident &&
            (stønadstype == null || it.stønadstype == null || stønadstype == it.stønadstype)
    }
}

fun Collection<BaseGrunnlag>.hentPersonMedIdent(
    ident: String?,
    stønadstype: Stønadstype? = null,
) = ident?.let {
    hentAllePersoner().find {
        it.personIdent == ident &&
            (stønadstype == null || it.stønadstype == null || stønadstype == it.stønadstype)
    }
}

fun Collection<BaseGrunnlag>.hentSøknadFraGrunnlagsreferanseListe(grunnlagsreferanseListe: List<Grunnlagsreferanse>) = toList()
    .finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<SøknadGrunnlag>(
        Grunnlagstype.SØKNAD,
        grunnlagsreferanseListe = grunnlagsreferanseListe,
    ).firstOrNull()
    ?.innhold

fun Collection<BaseGrunnlag>.hentPersonMedReferanse(referanse: Grunnlagsreferanse?) = referanse?.let {
    toList()
        .filtrerBasertPåEgenReferanse(referanse = referanse)
        .firstOrNull()
}

fun Collection<BaseGrunnlag>.hentSøknadsiderForPerson(
    personident: Personident,
    stønadstype: Stønadstype?,
): List<Long> {
    val person = hentPersonMedIdent(personident.verdi, stønadstype) ?: return emptyList()
    return toList()
        .filtrerOgKonverterBasertPåFremmedReferanse<SøknadGrunnlag>(
            Grunnlagstype.SØKNAD,
            gjelderBarnReferanse = person.referanse,
        ).map { it.innhold }
        .mapNotNull { it.søknadsid }
}

fun Collection<BaseGrunnlag>.hentSøknadForPerson(
    personident: Personident,
    stønadstype: Stønadstype?,
): SøknadGrunnlag? {
    val person = hentPersonMedIdent(personident.verdi, stønadstype) ?: return null
    return toList()
        .filtrerOgKonverterBasertPåFremmedReferanse<SøknadGrunnlag>(
            Grunnlagstype.SØKNAD,
            gjelderBarnReferanse = person.referanse,
        ).firstOrNull()
        ?.innhold
}

fun Collection<BaseGrunnlag>.hentPersonMedIdentKonvertert(
    ident: String?,
    stønadstype: Stønadstype? = null,
) = hentAllePersoner()
    .find {
        it.personIdent == ident &&
            (stønadstype == null || it.stønadstype == null || stønadstype == it.stønadstype)
    }?.innholdTilObjekt<Person>()

fun Collection<BaseGrunnlag>.hentPersonMedReferanseKonvertert(referanse: Grunnlagsreferanse?) = referanse?.let {
    toList()
        .filtrerBasertPåEgenReferanse(referanse = referanse)
        .firstOrNull()
        ?.innholdTilObjekt<Person>()
}

// Særbidrag grunnlag
val List<BaseGrunnlag>.særbidragskategori get() =
    filtrerBasertPåEgenReferanse(Grunnlagstype.SÆRBIDRAG_KATEGORI).firstOrNull()?.innholdTilObjekt<SærbidragskategoriGrunnlag>()

val List<BaseGrunnlag>.utgiftDirekteBetalt get() =
    filtrerBasertPåEgenReferanse(
        Grunnlagstype.UTGIFT_DIREKTE_BETALT,
    ).firstOrNull()?.innholdTilObjekt<UtgiftDirekteBetaltGrunnlag>()

val List<BaseGrunnlag>.utgiftMaksGodkjentBeløp get() =
    filtrerBasertPåEgenReferanse(
        Grunnlagstype.UTGIFT_MAKS_GODKJENT_BELØP,
    ).firstOrNull()?.innholdTilObjekt<UtgiftMaksGodkjentBeløpGrunnlag>()

val List<BaseGrunnlag>.utgiftsposter get() =
    filtrerBasertPåEgenReferanse(
        Grunnlagstype.UTGIFTSPOSTER,
    ).firstOrNull()?.innholdTilObjektListe<List<UtgiftspostGrunnlag>>() ?: emptyList()

val List<BaseGrunnlag>.delberegningSamværsklasse get() =
    find {
        it.type == Grunnlagstype.DELBEREGNING_SAMVÆRSKLASSE
    }!!
        .innholdTilObjekt<DelberegningSamværsklasse>()

fun List<BaseGrunnlag>.finnSluttberegningBarnebidragAldersjusteringGrunnlagIReferanser(grunnlagsreferanseListe: List<Grunnlagsreferanse>) = finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<SluttberegningBarnebidrag>(
    Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG_ALDERSJUSTERING,
    grunnlagsreferanseListe,
).firstOrNull()

fun List<BaseGrunnlag>.finnSluttberegningBarnebidragGrunnlagIReferanser(grunnlagsreferanseListe: List<Grunnlagsreferanse>) = finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<SluttberegningBarnebidrag>(
    Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
    grunnlagsreferanseListe,
).firstOrNull()

fun List<GrunnlagDto>.erResultatEndringUnderGrense(
    søknadsbarnReferanse: String,
    periodeGrunnlagsreferanseListe: List<Grunnlagsreferanse> = emptyList(),
): Boolean {
    val erIndeksreguleringEllerAldersjustering =
        finnSluttberegningIReferanser(periodeGrunnlagsreferanseListe)?.type?.erIndeksEllerAldersjustering == true
    if (erIndeksreguleringEllerAldersjustering) {
        return false
    }
    val delberegningGrense = finnDelberegningSjekkGrense(søknadsbarnReferanse)
    return delberegningGrense?.innhold?.endringErOverGrense == false
}

fun List<GrunnlagDto>.hentVirkningstidspunktGrunnlagForBarn(gjelderBarnReferanse: String? = null): VirkningstidspunktGrunnlag? = filtrerBasertPåEgenReferanse(Grunnlagstype.VIRKNINGSTIDSPUNKT)
    .firstOrNull { gjelderBarnReferanse.isNullOrEmpty() || it.gjelderBarnReferanse == gjelderBarnReferanse }
    ?.innholdTilObjekt<VirkningstidspunktGrunnlag>()

fun List<GrunnlagDto>.erResultatEndringUnderGrenseForPeriode(
    periode: ÅrMånedsperiode,
    søknadsbarnReferanse: String?,
    periodeGrunnlagsreferanseListe: List<Grunnlagsreferanse> = emptyList(),
): Boolean {
    val erIndeksreguleringEllerAldersjustering =
        finnSluttberegningIReferanser(periodeGrunnlagsreferanseListe)?.type?.erIndeksEllerAldersjustering == true
    if (erIndeksreguleringEllerAldersjustering) {
        return false
    }
    val delberegningGrense = finnDelberegningSjekkGrenseForPeriode(periode, søknadsbarnReferanse)
    return delberegningGrense?.innhold?.endringErOverGrense == false
}

fun List<GrunnlagDto>.finnDelberegningSjekkGrensePeriode(
    periode: ÅrMånedsperiode,
    søknadsbarnReferanse: String?,
) = filtrerOgKonverterBasertPåFremmedReferanse<DelberegningEndringSjekkGrensePeriode>(
    Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE_PERIODE,
    gjelderBarnReferanse = søknadsbarnReferanse,
).find { it.innhold.periode.inneholder(periode) }

fun List<GrunnlagDto>.finnDelberegningSjekkGrenseForPeriode(
    periode: ÅrMånedsperiode,
    søknadsbarnReferanse: String?,
) = filtrerOgKonverterBasertPåFremmedReferanse<DelberegningEndringSjekkGrense>(
    Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE,
    gjelderBarnReferanse = søknadsbarnReferanse,
).find { it.innhold.periode.inneholder(periode) }

fun List<GrunnlagDto>.finnDelberegningSjekkGrense(søknadsbarnReferanse: String) = filtrerOgKonverterBasertPåFremmedReferanse<DelberegningEndringSjekkGrense>(
    Grunnlagstype.DELBEREGNING_ENDRING_SJEKK_GRENSE,
    gjelderBarnReferanse = søknadsbarnReferanse,
).firstOrNull()

// Filtrerer ut grunnlag som er gyldige for en gitt BM/BP/Barn kombinasjon
fun List<GrunnlagDto>.finnGyldigeGrunnlagForBarn(
    bmRef: Grunnlagsreferanse,
    bpRef: Grunnlagsreferanse,
    barnRef: Grunnlagsreferanse,
): List<GrunnlagDto> = this.filter {
    it.erGyldigForBarn(bmRef, bpRef, barnRef)
}

fun GrunnlagDto.personBmRef(): String? {
    if (!erPerson()) return null
    return if (innhold is POJONode) {
        (innhold.pojo as Person).bidragsmottaker
    } else {
        innhold["bidragsmottaker"]?.asText()
    }
}

// Sjekker om et grunnlag er gyldig for en gitt BM/BP/Barn kombinasjon
fun GrunnlagDto.erGyldigForBarn(
    bmRef: Grunnlagsreferanse,
    bpRef: Grunnlagsreferanse,
    barnRef: Grunnlagsreferanse,
): Boolean {
    val gjelderRef = this.gjelderReferanse
    val gjelderBarnRef = this.gjelderBarnReferanse
    val grunnlagstype = this.type
    return when {
        // Gjelder BM og riktig barn (eller ingen barn)
        gjelderRef == bmRef && (gjelderBarnRef == barnRef || gjelderBarnRef == null) -> true

        // Gjelder BP og riktig barn (eller ingen barn)
        gjelderRef == bpRef && (gjelderBarnRef == barnRef || gjelderBarnRef == null) -> true

        // Gjelder barnet selv
        gjelderRef == barnRef && (gjelderBarnRef == barnRef || gjelderBarnRef == null) -> true

        // Ingen gjelderReferanse, men gjelderBarnReferanse matcher barn (eller null)
        gjelderRef == null && (gjelderBarnRef == barnRef || gjelderBarnRef == null) -> true

        // BOSTATUS_PERIODE skal være med uansett hvis gjelderReferanse = BP
        grunnlagstype == Grunnlagstype.BOSTATUS_PERIODE && gjelderRef == bpRef -> true

        // FAKTISK_UTGIFT_PERIODE skal være med uansett hvis gjelderReferanse = BM
        grunnlagstype == Grunnlagstype.FAKTISK_UTGIFT_PERIODE && gjelderRef == bmRef -> true

        // PERSON_SØKNADSBARN skal være med uansett hvis BM matcher (brukes i beregning av tilsynsutgifter)
        grunnlagstype == Grunnlagstype.PERSON_SØKNADSBARN && personBmRef() == bmRef -> true

        else -> false
    }
}

inline fun <reified T : GrunnlagInnhold> BaseGrunnlag.tilInnholdMedReferanse() = InnholdMedReferanse(
    referanse,
    gjelderBarnReferanse,
    gjelderReferanse,
    innholdTilObjekt<T>(),
    this,
)

fun List<BaseGrunnlag>.hentAldersjusteringDetaljerGrunnlag(grunnlagsreferanseListe: List<Grunnlagsreferanse>) = finnOgKonverterGrunnlagSomErReferertFraGrunnlagsreferanseListe<AldersjusteringDetaljerGrunnlag>(
    Grunnlagstype.ALDERSJUSTERING_DETALJER,
    grunnlagsreferanseListe = grunnlagsreferanseListe,
).firstOrNull()

fun List<BaseGrunnlag>.hentBehandlingDetaljer(): BehandlingDetaljerGrunnlag? = filtrerOgKonverterBasertPåEgenReferanse<BehandlingDetaljerGrunnlag>(Grunnlagstype.BEHANDLING_DETALJER).firstOrNull()?.innhold
