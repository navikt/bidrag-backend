package no.nav.bidrag.domene.inntekt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.domene.enums.diverse.PlussMinus
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.time.Year

data class MappingPoster(
    val fulltNavnInntektspost: String,
    val plussMinus: PlussMinus,
    val sekkepost: Boolean,
    val fom: Year,
    val tom: Year,
)

data class Post(
    val fulltNavnInntektspost: String,
)

data class PostKonfig(
    val plussMinus: String,
    val sekkepost: String,
    val fom: String,
    val tom: String,
)

data class Beskrivelser(
    val beskrivelser: List<String>,
)

fun hentMappingerKapitalinntekt(): List<MappingPoster> = hentMapping("/kodeverk/inntekt/mapping_kaps.yaml")

fun hentMappingerLigs(): List<MappingPoster> = hentMapping("/kodeverk/inntekt/mapping_ligs.yaml")

fun hentMappingerAlle() = hentMappingerLigs() + hentMappingerKapitalinntekt()

fun hentMappingYtelser(): Map<String, Beskrivelser> {
    val objectmapper = ObjectMapper(YAMLFactory()).findAndRegisterModules().registerKotlinModule()
    val fil =
        PostKonfig::class.java.getResource("/kodeverk/inntekt/mapping_ytelser.yaml")
            ?: throw RuntimeException("Fant ingen fil på sti mapping_ytelser.yaml")
    return objectmapper.readValue(fil)
}

fun finnInntektsmapping(post: String) = hentMappingerAlle().firstOrNull { it.fulltNavnInntektspost == post }

fun erPostLigs(post: String) = hentMappingerLigs().any { it.fulltNavnInntektspost == post }

fun erPostKaps(post: String) = hentMappingerKapitalinntekt().any { it.fulltNavnInntektspost == post }

private fun hentMapping(path: String): List<MappingPoster> {
    try {
        val objectMapper = ObjectMapper(YAMLFactory())
        objectMapper.findAndRegisterModules()
        objectMapper.registerKotlinModule()
        val pathKapsfil = ClassPathResource(path).inputStream
        val mapping: Map<Post, List<PostKonfig>> = objectMapper.readValue(pathKapsfil)
        return mapping.flatMap { (post, postKonfigs) ->
            postKonfigs.map { postKonfig ->
                MappingPoster(
                    fulltNavnInntektspost = post.fulltNavnInntektspost,
                    plussMinus = PlussMinus.valueOf(postKonfig.plussMinus),
                    sekkepost = postKonfig.sekkepost == "JA",
                    fom = Year.parse(postKonfig.fom),
                    tom = Year.parse(postKonfig.tom),
                )
            }
        }
    } catch (e: IOException) {
        throw RuntimeException("Kunne ikke laste fil", e)
    }
}
