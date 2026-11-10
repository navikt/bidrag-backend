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
 * Lager nøkkel av metodenavn, parametere og eventuelt brukerident.
 * Metodenavnet hindrer at ulike metoder med samme cachenavn og like parametere deler verdi.
 */
class CacheNøkkelGenerator(
    private val brukerIdentifikator: ObjectProvider<CacheBrukerIdentifikator>,
) : KeyGenerator {
    private data class MetodeInfo(
        val navn: String,
        val perBruker: Boolean,
    )

    // Annotasjonsoppslag er tregt, så resultatet lagres per klasse og metode.
    private val metodeInfo = ConcurrentHashMap<Pair<Class<*>, Method>, MetodeInfo>()

    override fun generate(
        target: Any,
        method: Method,
        vararg params: Any?,
    ): Any {
        val info = metodeInfo.computeIfAbsent(target.javaClass to method) { lagMetodeInfo(target.javaClass, method) }
        val brukerIdent = if (info.perBruker) hentBrukerIdent() else null
        return CacheNøkkel(info.navn, brukerIdent, SimpleKeyGenerator.generateKey(*params))
    }

    private fun hentBrukerIdent(): String {
        val identifikator =
            checkNotNull(brukerIdentifikator.ifAvailable) {
                "perBruker = true krever en bean av typen ${CacheBrukerIdentifikator::class.simpleName}"
            }
        return identifikator.hentBrukerIdent().also {
            check(it.isNotBlank()) { "Brukerident for cache kan ikke være tom" }
        }
    }

    private fun lagMetodeInfo(
        targetClass: Class<*>,
        method: Method,
    ): MetodeInfo {
        val perBruker =
            AnnotatedElementUtils.findMergedAnnotation(method, KortCache::class.java)?.perBruker
                ?: AnnotatedElementUtils.findMergedAnnotation(method, LangCache::class.java)?.perBruker
                ?: AnnotatedElementUtils.findMergedAnnotation(targetClass, KortCache::class.java)?.perBruker
                ?: AnnotatedElementUtils.findMergedAnnotation(targetClass, LangCache::class.java)?.perBruker
                ?: error("Fant verken @KortCache eller @LangCache på ${method.name}")
        return MetodeInfo("${method.declaringClass.simpleName}.${method.name}", perBruker)
    }
}
