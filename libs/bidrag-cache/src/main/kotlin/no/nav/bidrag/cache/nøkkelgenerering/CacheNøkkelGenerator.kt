package no.nav.bidrag.cache.nøkkelgenerering

import no.nav.bidrag.cache.annotasjoner.KortCache
import no.nav.bidrag.cache.annotasjoner.LangCache
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.convert.DurationStyle
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

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
        val levetid: Duration?,
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
        return CacheNøkkel(info.navn, brukerIdent, params.toList(), info.levetid)
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
        val navn = "${method.declaringClass.simpleName}.${method.name}"
        val (perBruker, levetid) =
            finnInnstillinger(method)
                ?: finnInnstillinger(targetClass)
                ?: error("Fant verken @KortCache eller @LangCache på ${method.name}")
        return MetodeInfo(navn, perBruker, parseLevetid(navn, levetid))
    }

    private fun finnInnstillinger(element: AnnotatedElement): Pair<Boolean, String>? {
        AnnotatedElementUtils.findMergedAnnotation(element, KortCache::class.java)?.let { return it.perBruker to it.levetid }
        AnnotatedElementUtils.findMergedAnnotation(element, LangCache::class.java)?.let { return it.perBruker to it.levetid }
        return null
    }

    private fun parseLevetid(
        metode: String,
        levetid: String,
    ): Duration? {
        if (levetid.isBlank()) return null
        val varighet =
            runCatching { DurationStyle.detectAndParse(levetid) }
                .getOrElse { throw IllegalArgumentException("Ugyldig levetid \"$levetid\" på $metode. Bruk for eksempel \"30s\", \"5m\" eller \"PT1H\".", it) }
        require(varighet.isPositive) { "Levetid på $metode må være større enn null" }
        return varighet
    }
}
