package no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import no.nav.bidrag.cache.annotasjoner.KortCache
import no.nav.bidrag.cache.annotasjoner.LangCache
import org.springframework.beans.factory.ObjectProvider
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.core.annotation.AnnotatedElementUtils

/**
 * Bruker samme nøkkel som Springs standard, men legger til brukerident når `perBruker = true`.
 */
class CacheNøkkelGenerator(
    private val brukerIdentifikator: ObjectProvider<CacheBrukerIdentifikator>,
) : KeyGenerator {
    private val erPerBrukerCache = ConcurrentHashMap<Pair<Class<*>, Method>, Boolean>()

    override fun generate(
        target: Any,
        method: Method,
        vararg params: Any?,
    ): Any {
        val nøkkel = SimpleKeyGenerator.generateKey(*params)
        if (!erPerBruker(target.javaClass, method)) return nøkkel

        val identifikator =
            checkNotNull(brukerIdentifikator.ifAvailable) {
                "perBruker = true krever en bean av typen ${CacheBrukerIdentifikator::class.simpleName}"
            }
        val brukerIdent = identifikator.hentBrukerIdent()
        check(brukerIdent.isNotBlank()) { "Brukerident for cache kan ikke være tom" }
        return CacheBrukerNøkkel(brukerIdent, nøkkel)
    }

    private fun erPerBruker(
        targetClass: Class<*>,
        method: Method,
    ): Boolean =
        erPerBrukerCache.computeIfAbsent(targetClass to method) {
            AnnotatedElementUtils.findMergedAnnotation(method, KortCache::class.java)?.perBruker
                ?: AnnotatedElementUtils.findMergedAnnotation(method, LangCache::class.java)?.perBruker
                ?: AnnotatedElementUtils.findMergedAnnotation(targetClass, KortCache::class.java)?.perBruker
                ?: AnnotatedElementUtils.findMergedAnnotation(targetClass, LangCache::class.java)?.perBruker
                ?: false
        }
}
