@file:Suppress("unused")

package no.nav.bidrag.commons.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import no.nav.bidrag.domene.util.Visningsnavn
import org.slf4j.LoggerFactory
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

internal val objectmapper = ObjectMapper(YAMLFactory()).findAndRegisterModules().registerKotlinModule()
private val visningsnavnCache: Map<String, Map<String, String>> = emptyMap()
private val kodeverkUrl = AtomicReference("")
const val POSTNUMMER = "Postnummer"
const val LANDKODER = "Landkoder"
const val NAV_SKJEMA = "NAVSkjema"
const val LANDKODER_ISO2 = "LandkoderISO2"
const val SPRÅK = "Språk"
const val VALUTAER = "Valutaer"
const val SUMMERT_SKATTEGRUNNLAG = "Summert skattegrunnlag"
const val SPESIFISERT_SUMMERT_SKATTEGRUNNLAG = "SpesifisertSummertSkattegrunnlag"
const val LOENNSBESKRIVELSE = "Loennsbeskrivelse"
const val YTELSEFRAOFFENTLIGE = "YtelseFraOffentligeBeskrivelse"
const val PENSJONELLERTRYGDEBESKRIVELSE = "PensjonEllerTrygdeBeskrivelse"
const val NAERINGSINNTEKTSBESKRIVELSE = "Naeringsinntektsbeskrivelse"
private val kodeverkCache: Cache<String, KodeverkKoderBetydningerResponse> =
    Caffeine
        .newBuilder()
        .maximumSize(1000)
        .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
        .build()
private val log = LoggerFactory.getLogger(KodeverkProvider::class.java)

fun finnVisningsnavnSkattegrunnlag(fulltNavnInntektspost: String): String =
    finnVisningsnavnForKode(fulltNavnInntektspost, SUMMERT_SKATTEGRUNNLAG) ?: ""

fun finnPoststedForPostnummer(postnummer: String): String? = finnVisningsnavnForKode(postnummer, POSTNUMMER)

fun finnLandkodeForLandkoder(landkode: String): String? = finnVisningsnavnForKode(landkode, LANDKODER)

fun finnLandkodeForLandkoderIso2(landkode: String): String? = finnVisningsnavnForKode(landkode, LANDKODER_ISO2)

fun finnSpråknavnForSpråkkode(språkkode: String): String? = finnVisningsnavnForKode(språkkode, SPRÅK)

fun finnValutanavnForValutakode(valutakode: String): String? = finnVisningsnavnForKode(valutakode, VALUTAER)

fun finnVisningsnavnLønnsbeskrivelse(fulltNavnInntektspost: String): String =
    finnVisningsnavnForKode(fulltNavnInntektspost, LOENNSBESKRIVELSE) ?: ""

fun hentNavSkjemaKodeverk() = hentKodeverkKodeBeskrivelseMap(NAV_SKJEMA)

fun finnVisningsnavnKodeverk(
    fulltNavnInntektspost: String,
    kodeverk: String,
): String = finnVisningsnavnForKode(fulltNavnInntektspost, kodeverk) ?: ""

fun finnVisningsnavn(fulltNavnInntektspost: String): String =
    finnVisningsnavnFraFil(fulltNavnInntektspost)
        ?: finnVisningsnavnForKode(fulltNavnInntektspost, SUMMERT_SKATTEGRUNNLAG)
        ?: finnVisningsnavnForKode(fulltNavnInntektspost, LOENNSBESKRIVELSE)
        ?: finnVisningsnavnForKode(fulltNavnInntektspost, YTELSEFRAOFFENTLIGE)
        ?: finnVisningsnavnForKode(fulltNavnInntektspost, PENSJONELLERTRYGDEBESKRIVELSE)
        ?: finnVisningsnavnForKode(fulltNavnInntektspost, NAERINGSINNTEKTSBESKRIVELSE)
        ?: finnVisningsnavnForKode(fulltNavnInntektspost, SPESIFISERT_SUMMERT_SKATTEGRUNNLAG)
        ?: ""

class KodeverkProvider {
    companion object {
        fun initialiser(url: String) {
            kodeverkUrl.set(url)
        }

        fun initialiserKodeverkCache() {
            kodeverkCache.get(SUMMERT_SKATTEGRUNNLAG) { hentKodeverk(SUMMERT_SKATTEGRUNNLAG) }
            kodeverkCache.get(NAV_SKJEMA) { hentKodeverk(NAV_SKJEMA) }
            kodeverkCache.get(LOENNSBESKRIVELSE) { hentKodeverk(LOENNSBESKRIVELSE) }
            kodeverkCache.get(YTELSEFRAOFFENTLIGE) { hentKodeverk(YTELSEFRAOFFENTLIGE) }
            kodeverkCache.get(PENSJONELLERTRYGDEBESKRIVELSE) { hentKodeverk(PENSJONELLERTRYGDEBESKRIVELSE) }
            kodeverkCache.get(NAERINGSINNTEKTSBESKRIVELSE) { hentKodeverk(NAERINGSINNTEKTSBESKRIVELSE) }
            kodeverkCache.get(POSTNUMMER) { hentKodeverk(POSTNUMMER) }
        }

        fun invaliderKodeverkCache() {
            kodeverkCache.invalidate(NAV_SKJEMA)
            kodeverkCache.invalidate(SUMMERT_SKATTEGRUNNLAG)
            kodeverkCache.invalidate(LOENNSBESKRIVELSE)
            kodeverkCache.invalidate(YTELSEFRAOFFENTLIGE)
            kodeverkCache.invalidate(PENSJONELLERTRYGDEBESKRIVELSE)
            kodeverkCache.invalidate(NAERINGSINNTEKTSBESKRIVELSE)
            kodeverkCache.invalidate(POSTNUMMER)
        }
    }
}

fun hentKodeverkKodeBeskrivelseMap(kodeverk: String): Map<String, String> =
    kodeverkCache
        .get(kodeverk) { hentKodeverk(kodeverk) }
        ?.betydninger
        ?.map {
            it.key to
                run {
                    val betydning = it.value.maxBy { it.gyldigFra }.beskrivelser["nb"]
                    if (betydning?.tekst.isNullOrEmpty()) betydning?.term ?: "" else betydning.tekst
                }
        }?.toMap() ?: emptyMap()

fun finnVisningsnavnForKode(
    kode: String,
    kodeverk: String,
): String? {
    val betydning =
        kodeverkCache
            .get(kodeverk) { hentKodeverk(kodeverk) }
            .betydninger[kode]
            ?.firstNotNullOf { betydning -> betydning.beskrivelser["nb"] }
    return if (betydning?.tekst.isNullOrEmpty()) betydning?.term else betydning?.tekst
}

private fun finnVisningsnavnFraFil(kode: String): String? {
    val visningsnavnMap = lastVisningsnavnFraFil("inntektsposter.yaml")
    return visningsnavnMap[kode]
}

private fun hentKodeverk(kodeverk: String): KodeverkKoderBetydningerResponse {
    val kodeverkContext = "${kodeverkUrl.get()}/kodeverk/$kodeverk"
    val restTemplate: RestTemplate =
        RestTemplateBuilder()
            .defaultHeader("Nav-Call-Id", CorrelationId.fetchCorrelationIdForThread())
            .defaultHeader("Nav-Consumer-Id", System.getenv("NAIS_APP_NAME") ?: "bidrag-commons")
            .build()
    log.debug("Laster kodeverk for $kodeverk")
    return restTemplate.getForEntity<KodeverkKoderBetydningerResponse>(kodeverkContext).body!!
}

data class KodeverkKoderBetydningerResponse(
    val betydninger: Map<String, List<KodeverkBetydning>> = emptyMap(),
)

data class KodeverkBetydning(
    val gyldigFra: LocalDate,
    val gyldigTil: LocalDate,
    val beskrivelser: Map<String, KodeverkBeskrivelse>,
)

data class KodeverkBeskrivelse(
    val tekst: String,
    val term: String,
)

private fun lastVisningsnavnFraFil(filnavn: String): Map<String, String> {
    val fil = hentFil("/kodeverk/visningsnavn/$filnavn")
    return visningsnavnCache.getOrDefault(
        filnavn,
        objectmapper.readValue(fil),
    )
}

private fun hentFil(filsti: String) =
    Visningsnavn::class.java.getResource(
        filsti,
    ) ?: throw RuntimeException("Fant ingen fil på sti $filsti")
