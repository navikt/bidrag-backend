package no.nav.bidrag.cache.annotasjoner

import no.nav.bidrag.cache.util.KORT
import no.nav.bidrag.cache.util.NØKKELGENERATOR
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * Cacher kun lokalt i Caffeine med kort levetid. Brukes når to-nivå-cache med Valkey ikke gir mening.
 * Null-verdier caches. Bruk [unless] for å la være å cache bestemte resultater, for eksempel
 * `unless = "#result == null"` eller `unless = "#result.isEmpty()"`.
 *
 * [perBruker] må alltid oppgis. Bruk `true` når svaret avhenger av hvem som er innlogget, for eksempel
 * tilgangssjekker. Det krever en bean av typen
 * [no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering.CacheBrukerIdentifikator].
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Cacheable(cacheManager = KORT, keyGenerator = NØKKELGENERATOR)
annotation class KortCache(
    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheNames")
    vararg val value: String,
    val perBruker: Boolean,
    @get:AliasFor(annotation = Cacheable::class, attribute = "unless")
    val unless: String = "",
)
