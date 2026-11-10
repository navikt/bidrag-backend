package no.nav.bidrag.cache.annotasjoner

import no.nav.bidrag.cache.util.LANG_CACHE_MANAGER
import no.nav.bidrag.cache.util.NØKKELGENERATOR
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * Cacher i Caffeine og Valkey fram til starten av neste arbeidsdag.
 * Ved bom i Caffeine slås verdien opp i Valkey før metoden kalles.
 * Null-verdier caches. Bruk [unless] for å la være å cache bestemte resultater, for eksempel
 * `unless = "#result == null"` eller `unless = "#result.isEmpty()"`.
 *
 * [perBruker] må alltid oppgis. Bruk `true` når svaret avhenger av hvem som er innlogget, for eksempel
 * tilgangssjekker. Det krever en bean av typen
 * [no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering.CacheBrukerIdentifikator].
 *
 * [levetid] overstyrer standard levetid fram til kl. 06:00 neste dag, både i Caffeine og Valkey.
 * Eksempler: `"30s"`, `"5m"`, `"2h"` eller `"PT1H"`.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Cacheable(cacheManager = LANG_CACHE_MANAGER, keyGenerator = NØKKELGENERATOR)
annotation class LangCache(
    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheNames")
    vararg val value: String,
    val perBruker: Boolean,
    @get:AliasFor(annotation = Cacheable::class, attribute = "unless")
    val unless: String = "",
    val levetid: String = "",
)
