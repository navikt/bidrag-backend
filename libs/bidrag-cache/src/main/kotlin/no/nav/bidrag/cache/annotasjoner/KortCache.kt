package no.nav.bidrag.cache.annotasjoner

import no.nav.bidrag.cache.util.KORT
import no.nav.bidrag.cache.util.NØKKELGENERATOR
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.annotation.AliasFor

/**
 * Cacher kun lokalt i Caffeine med kort levetid. Brukes når to-nivå-cache med Valkey ikke gir mening.
 *
 * Sett [perBruker] til `true` for å skille verdiene per bruker. Krever en bean av typen
 * [no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering.CacheBrukerIdentifikator].
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Cacheable(cacheManager = KORT, keyGenerator = NØKKELGENERATOR)
annotation class KortCache(
    @get:AliasFor(annotation = Cacheable::class, attribute = "cacheNames")
    vararg val value: String,
    val perBruker: Boolean = false,
)
