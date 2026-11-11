package no.nav.bidrag.cache.nøkkelgenerering

import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat

/**
 * Nøkkel for en cacheverdi. [brukerIdent] er null når verdien deles mellom alle brukere.
 * [levetid] er null når cachens standard levetid skal brukes.
 *
 * [toString] brukes som nøkkel i Valkey. Brukerident og parametere gjøres om til JSON og hashes med SHA-256,
 * slik at fødselsnumre og andre personopplysninger ikke ligger i klartekst.
 */
data class CacheNøkkel(
    val metode: String,
    val brukerIdent: String?,
    val parametere: List<Any?>,
    val levetid: Duration? = null,
) {
    override fun toString(): String = "$metode::${sha256(JSON.writeValueAsString(mapOf("bruker" to brukerIdent, "parametere" to parametere)))}"

    private fun sha256(verdi: String): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(verdi.toByteArray()))

    private companion object {
        val JSON: JsonMapper =
            JsonMapper
                .builder()
                .addModule(KotlinModule.Builder().build())
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build()
    }
}
