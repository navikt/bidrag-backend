package no.nav.bidrag.domene.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.domene.enums.barnetilsyn.Skolealder
import no.nav.bidrag.domene.enums.barnetilsyn.Tilsynstype
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.beregning.Resultatkode.Companion.erAvslag
import no.nav.bidrag.domene.enums.beregning.ResultatkodeBarnebidrag
import no.nav.bidrag.domene.enums.beregning.ResultatkodeForskudd
import no.nav.bidrag.domene.enums.beregning.ResultatkodeSærtilskudd
import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.inntekt.Inntektsrapportering
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.Bostatuskode
import no.nav.bidrag.domene.enums.person.Sivilstandskode
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.enums.samværskalkulator.SamværskalkulatorFerietype
import no.nav.bidrag.domene.enums.samværskalkulator.SamværskalkulatorNetterFrekvens
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.særbidrag.Utgiftstype
import no.nav.bidrag.domene.enums.vedtak.BeregnTil
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.net.URL
import java.time.YearMonth
import java.time.format.DateTimeFormatter

typealias VisningsnavnKodeMap = Map<String, Visningsnavn>

internal val objectmapper = ObjectMapper(YAMLFactory()).findAndRegisterModules().registerKotlinModule()
private val visningsnavnCache: Map<String, VisningsnavnKodeMap> = emptyMap()
internal val formatterMonthYear = DateTimeFormatter.ofPattern("MM.yyyy")

fun YearMonth?.toMontYear(): String = this?.format(formatterMonthYear) ?: ""

fun visningsnavnMangler(kode: String): Nothing = throw RuntimeException("Fant ingen visningsnavn for kode $kode")

data class Visningsnavn(
    val intern: String,
    val bruker: Map<Språk, String> = mapOf(Språk.NB to intern),
)

val Inntektsrapportering.Companion.visningsnavnSomKreverÅrstall get() =
    listOf(
        Inntektsrapportering.AINNTEKT,
        Inntektsrapportering.KAPITALINNTEKT,
        Inntektsrapportering.LIGNINGSINNTEKT,
    )
val Inntektsrapportering.Companion.visningsnavnSomKreverPeriode get() =
    listOf(
        Inntektsrapportering.OVERGANGSSTØNAD,
    )
val Behandlingstype.visningsnavn get() = lastVisningsnavnFraFil("behandlingstype.yaml")[name] ?: visningsnavnMangler(name)
val PrivatAvtaleType.visningsnavn get() = lastVisningsnavnFraFil("privatavtaletype.yaml")[name] ?: visningsnavnMangler(name)
val Skolealder.visningsnavn get() = lastVisningsnavnFraFil("skolealder.yaml")[name] ?: visningsnavnMangler(name)
val Tilsynstype.visningsnavn get() = lastVisningsnavnFraFil("tilsynstype.yaml")[name] ?: visningsnavnMangler(name)
val Inntektstype.visningsnavn get() = lastVisningsnavnFraFil("inntektstype.yaml")[name] ?: visningsnavnMangler(name)
val Inntektsrapportering.visningsnavn get() = lastVisningsnavnFraFil("inntektsrapportering.yaml")[name] ?: visningsnavnMangler(name)
val Utgiftstype.visningsnavn get() = lastVisningsnavnFraFil("utgiftstype.yaml")[name] ?: visningsnavnMangler(name)
val Engangsbeløptype.visningsnavn get() = lastVisningsnavnFraFil("engangsbeløptype.yaml")[name] ?: visningsnavnMangler(name)
val Særbidragskategori.visningsnavn get() = lastVisningsnavnFraFil("særbidragskategori.yaml")[name] ?: visningsnavnMangler(name)
val Vedtakstype.visningsnavn get() = lastVisningsnavnFraFil("vedtakstype.yaml")[name] ?: visningsnavnMangler(name)
val Samværsklasse.visningsnavn get() = lastVisningsnavnFraFil("samværsklasse.yaml")[name] ?: visningsnavnMangler(name)
val BeregnTil.visningsnavn get() = lastVisningsnavnFraFil("beregntil.yaml")[name] ?: visningsnavnMangler(name)
val SamværskalkulatorFerietype.visningsnavn get() =
    lastVisningsnavnFraFil("samværskalkulatorferietype.yaml")[name]
        ?: visningsnavnMangler(name)
val SamværskalkulatorNetterFrekvens.visningsnavn get() =
    lastVisningsnavnFraFil("samværskalkulatornetterfrekvens.yaml")[name]
        ?: visningsnavnMangler(name)

fun Vedtakstype.visningsnavnIntern(opprinneligVedtakstype: Vedtakstype? = null) =
    opprinneligVedtakstype?.let {
        if (it == Vedtakstype.FASTSETTELSE && this == Vedtakstype.ENDRING) {
            it.visningsnavn.intern
        } else {
            "${it.visningsnavn.intern} (${this.visningsnavn.intern})"
        }
    } ?: this.visningsnavn.intern

fun Inntektsrapportering.visningsnavnIntern(årstall: Int?) = "${visningsnavn.intern} $årstall".trim()

fun Inntektsrapportering.visningsnavnPeriode(periode: ÅrMånedsperiode) =
    "${visningsnavn.intern} ${periode.fom.toMontYear()} - ${periode.til.toMontYear()}".trim()

fun Inntektsrapportering.visningsnavnMedÅrstall(
    årstall: Int?,
    periode: ÅrMånedsperiode? = null,
) = if (Inntektsrapportering.visningsnavnSomKreverÅrstall.contains(
        this,
    )
) {
    visningsnavnIntern(årstall)
} else if (Inntektsrapportering.visningsnavnSomKreverPeriode.contains(this) && periode != null) {
    visningsnavnPeriode(periode)
} else {
    visningsnavn.intern
}

val VirkningstidspunktÅrsakstype.visningsnavn get() = lastVisningsnavnFraFil("årsak.yaml")[name] ?: visningsnavnMangler(name)
val Sivilstandskode.visningsnavn get() = lastVisningsnavnFraFil("sivilstand.yaml")[name] ?: visningsnavnMangler(name)
val Bostatuskode.visningsnavn get() = lastVisningsnavnFraFil("bostatus.yaml")[name] ?: visningsnavnMangler(name)
val ResultatkodeForskudd.visningsnavn get() =
    lastVisningsnavnFraFil("resultatDeprecated.yaml", "FORSKUDD")[name]
        ?: visningsnavnMangler(name)
val ResultatkodeBarnebidrag.visningsnavn get() =
    lastVisningsnavnFraFil("resultatDeprecated.yaml", "BARNEBIDRAG")[name]
        ?: visningsnavnMangler(name)
val ResultatkodeSærtilskudd.visningsnavn get() =
    lastVisningsnavnFraFil("resultatDeprecated.yaml", "SÆRTILSKUDD")[name]
        ?: visningsnavnMangler(name)
val Resultatkode.visningsnavn get() = lastVisningsnavnFraFil("resultat.yaml")[name] ?: visningsnavnMangler(name)
val OffentligIdType.visningsnavn get() = lastVisningsnavnFraFil("offentligidtype.yaml")[name] ?: visningsnavnMangler(name)
val Områdekode.visningsnavn get() = lastVisningsnavnFraFil("områdekode.yaml")[name] ?: visningsnavnMangler(name)

fun Resultatkode.visningsnavnIntern(vedtakstype: Vedtakstype? = null) = visningsnavnIntern(vedtakstype, null)

fun Resultatkode.visningsnavnIntern(
    vedtakstype: Vedtakstype? = null,
    løperStønad: Boolean? = null,
) = when {
    this.erAvslag() -> {
        val prefiks =
            when {
                (løperStønad != null && løperStønad) || vedtakstype == Vedtakstype.OPPHØR -> "Opphør"
                else -> "Avslag"
            }
        "$prefiks, ${visningsnavn.intern.lowercase().fjernAvslagOpphørPrefiks()}"
    }

    else -> {
        visningsnavn.intern
    }
}

fun String?.fjernAvslagOpphørPrefiks() =
    this
        ?.replace("Avslag, ", "", ignoreCase = true)
        ?.replace("Avslag ", "", ignoreCase = true)
        ?.replace("Opphør, ", "", ignoreCase = true)
        ?.replace("Opphør ", "", ignoreCase = true)

fun lastVisningsnavnFraFil(
    filnavn: String,
    category: String? = null,
): VisningsnavnKodeMap {
    val fil = hentFil("/kodeverk/visningsnavn/$filnavn")
    return visningsnavnCache.getOrDefault(
        filnavn,
        if (category != null) lastVisningsnavnFraFilForKategori(fil, category) else objectmapper.readValue(fil),
    )
}

private fun lastVisningsnavnFraFilForKategori(
    fil: URL,
    kategori: String,
): VisningsnavnKodeMap {
    val jsonNode: Map<String, VisningsnavnKodeMap> = objectmapper.readValue(fil)
    return jsonNode[kategori] ?: throw RuntimeException("Fant ikke visningsnavn for kategori $kategori i filsti $fil")
}

private fun hentFil(filsti: String) =
    Visningsnavn::class.java.getResource(
        filsti,
    ) ?: throw RuntimeException("Fant ingen fil på sti $filsti")
