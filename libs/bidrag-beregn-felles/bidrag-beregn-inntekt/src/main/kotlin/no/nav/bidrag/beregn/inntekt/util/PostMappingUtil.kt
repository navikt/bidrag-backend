package no.nav.bidrag.beregn.inntekt.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.domene.enums.diverse.PlussMinus
import no.nav.bidrag.beregn.inntekt.service.MappingPoster
import no.nav.bidrag.beregn.inntekt.service.Post
import no.nav.bidrag.beregn.inntekt.service.PostKonfig
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.time.Year
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.java
class PostMappingUtil
private const val SEKKEPOST_JA = "JA"
private const val MAPPING_KAPS_PATH = "files/mapping_kaps.yaml"
private const val MAPPING_LIGS_PATH = "files/mapping_ligs.yaml"

private val objectMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule().findAndRegisterModules()
private val resourceClassLoader: ClassLoader = MappingPoster::class.java.classLoader
private val mappingCache = ConcurrentHashMap<String, List<MappingPoster>>()

fun hentMappingerKapitalinntekt(): List<MappingPoster> = hentMapping(MAPPING_KAPS_PATH)
fun hentMappingerLigs(): List<MappingPoster> = hentMapping(MAPPING_LIGS_PATH)

private fun hentMapping(path: String): List<MappingPoster> = mappingCache.computeIfAbsent(path.removePrefix("/")) { normalizedPath ->
    loadMapping(normalizedPath)
}

private fun loadMapping(normalizedPath: String): List<MappingPoster> {
    try {
        val mapping: Map<Post, List<PostKonfig>> =
            resourceClassLoader.getResourceAsStream(normalizedPath)?.use { input ->
                objectMapper.readValue(input)
            } ?: throw IllegalArgumentException("Classpath resource not found: $normalizedPath")

        return mapping.flatMap { (post, postKonfigs) ->
            postKonfigs.map { postKonfig ->
                MappingPoster(
                    fulltNavnInntektspost = post.fulltNavnInntektspost,
                    plussMinus = PlussMinus.valueOf(postKonfig.plussMinus),
                    sekkepost = postKonfig.sekkepost.equals(SEKKEPOST_JA, ignoreCase = true),
                    fom = Year.parse(postKonfig.fom),
                    tom = Year.parse(postKonfig.tom),
                )
            }
        }
    } catch (e: IOException) {
        throw IllegalStateException("Kunne ikke laste fil: $normalizedPath", e)
    }
}
