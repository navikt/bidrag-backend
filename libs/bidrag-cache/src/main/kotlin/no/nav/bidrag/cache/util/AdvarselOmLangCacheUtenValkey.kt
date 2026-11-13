package no.nav.bidrag.cache.util

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.cache.annotasjoner.LangCache
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.EnvironmentAware
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.env.Environment
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils

/** Advarer ved oppstart når appen bruker [LangCache] uten Valkey. Da caches verdiene bare i Caffeine i hver pod. */
class AdvarselOmLangCacheUtenValkey :
    BeanPostProcessor,
    EnvironmentAware {
    private var valkeyErSattOpp = true

    override fun setEnvironment(environment: Environment) {
        valkeyErSattOpp = environment.containsProperty(VALKEY_INSTANS)
    }

    override fun postProcessBeforeInitialization(
        bean: Any,
        beanName: String,
    ): Any {
        if (valkeyErSattOpp) return bean
        if (brukerLangCache(ClassUtils.getUserClass(bean))) {
            LOGGER.warn {
                "$beanName bruker @LangCache, men $VALKEY_INSTANS er ikke satt. Verdiene caches bare i Caffeine i hver pod " +
                    "og overlever ikke restart. Sett opp Valkey eller bruk @KortCache."
            }
        }
        return bean
    }

    private fun brukerLangCache(klasse: Class<*>): Boolean = !klasse.name.startsWith(SPRING_PAKKE) &&
        (
            AnnotatedElementUtils.hasAnnotation(klasse, LangCache::class.java) ||
                ReflectionUtils.getUniqueDeclaredMethods(klasse).any { AnnotatedElementUtils.hasAnnotation(it, LangCache::class.java) }
            )

    private companion object {
        const val SPRING_PAKKE = "org.springframework."
        val LOGGER = KotlinLogging.logger { }
    }
}
