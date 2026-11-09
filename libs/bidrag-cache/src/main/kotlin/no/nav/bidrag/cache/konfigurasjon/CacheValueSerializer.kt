package no.nav.bidrag.cache.konfigurasjon

import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity

internal class CacheValueSerializer(
    private val jsonSerializer: GenericJacksonJsonRedisSerializer,
) : RedisSerializer<Any> {
    override fun serialize(entry: Any?): ByteArray {
        require(entry is CachetVerdi) { "Ugyldig cacheformat" }
        val response = entry.verdi
        val storedValue = if (response is ResponseEntity<*>) {
            entry.copy(
                verdi = CachedHttpResponse(
                    response.statusCode.value(),
                    response.headers.headerSet().associate { it.key to it.value.toList() },
                    response.body,
                ),
            )
        } else {
            entry
        }
        return requireNotNull(jsonSerializer.serialize(storedValue)) { "Kunne ikke serialisere cacheverdi" }
    }

    override fun deserialize(bytes: ByteArray?): Any? {
        val entry = jsonSerializer.deserialize(bytes) ?: return null
        check(entry is CachetVerdi) { "Ugyldig cacheformat" }
        val value = entry.verdi
        if (value is CachedHttpResponse) {
            val headers = HttpHeaders()
            value.headers.forEach { (name, values) -> headers.addAll(name, values) }
            val response = ResponseEntity.status(HttpStatusCode.valueOf(value.status)).headers(headers).body(value.body)
            return entry.copy(verdi = response)
        }
        return entry
    }
}

internal data class CachedHttpResponse(
    val status: Int,
    val headers: Map<String, List<String>>,
    val body: Any?,
)
