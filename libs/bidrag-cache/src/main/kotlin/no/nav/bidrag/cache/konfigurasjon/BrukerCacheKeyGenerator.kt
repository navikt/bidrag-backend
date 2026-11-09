package no.nav.bidrag.cache.konfigurasjon

import org.springframework.cache.interceptor.KeyGenerator
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BrukerCacheKeyGenerator(
    private val valkeyPassord: String,
    private val brukerId: () -> String?,
    private val nøkkelprefix: (Any) -> String? = { null },
) : KeyGenerator {
    companion object {
        const val BEAN_NAME = "brukerCacheKeyGenerator"
    }

    override fun generate(
        target: Any,
        method: Method,
        vararg params: Any?,
    ): Any {
        // Avvis ukjent bruker før cacheoppslaget
        val bruker =
            checkNotNull(brukerId()?.takeIf { it.isNotBlank() }) {
                "Mangler brukeridentitet for brukerbasert cache"
            }
        val parameterTyper = method.parameterTypes.joinToString(separator = ",", prefix = "(", postfix = ")") { it.name }
        val keyParts = listOf(method.declaringClass.name, method.name, parameterTyper, bruker, *params.map(::leggTilTypePrefix).toTypedArray())
        // Mac er ikke trådsikker og må derfor opprettes per kall.
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(valkeyPassord.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        // Lengdeprefikser hindrer at ulike sammensetninger av parametere gir samme nøkkelgrunnlag.
        val signatur = hmac.doFinal(keyParts.joinToString("") { "${it.length}:$it" }.toByteArray(StandardCharsets.UTF_8))
        return signatur.joinToString("") { "%02x".format(it) }
    }

    // Typeprefikser skiller verdier med lik verdi men av forskjellige typer
    private fun leggTilTypePrefix(verdi: Any?): String = when (verdi) {
        null -> "null"

        is String -> "string:$verdi"

        is Number, is Boolean -> "${verdi.javaClass.name}:$verdi"

        is Enum<*> -> "enum:${verdi.declaringJavaClass.name}:${verdi.name}"

        is List<*> -> "list:${verdi.joinToString("") { element -> leggTilTypePrefix(element).let { "${it.length}:$it" } }}"

        else -> nøkkelprefix(verdi)
            ?: throw IllegalArgumentException("Ukjent type for cache-nøkkel: ${verdi.javaClass.name}")
    }
}
