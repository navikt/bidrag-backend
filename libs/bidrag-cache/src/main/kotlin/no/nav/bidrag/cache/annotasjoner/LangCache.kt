package no.nav.bidrag.cache.annotasjoner

import no.nav.bidrag.cache.util.LANG
import no.nav.bidrag.cache.util.NØKKELGENERATOR
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * Cacher i Caffeine og Valkey fram til starten av neste arbeidsdag.
 * Ved bom i Caffeine slås verdien opp i Valkey før metoden kalles.
 *
 * [perBruker] må alltid oppgis. Bruk `true` når svaret avhenger av hvem som er innlogget, for eksempel
 * tilgangssjekker. Det krever en bean av typen
 * [no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering.CacheBrukerIdentifikator].
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Cacheable(cacheManager = LANG, keyGenerator = NØKKELGENERATOR)
annotation class LangCache(
    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheNames")
    vararg val value: String,
    val perBruker: Boolean,
)
