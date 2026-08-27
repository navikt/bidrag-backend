package no.nav.bidrag.transport.felles

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.util.LinkedMultiValueMap
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Interface med metode for å konvertere et objekt til en LinkedMultiValueMap
 * En LinkedMultiValueMap kan brukes i UriComponentsBuilder med .queryParams(<linkeMultiValueMap>)
 */
interface QueryObject {
    private val objectMapper: ObjectMapper
        get() =
            ObjectMapper()
                .registerKotlinModule()
                .registerModule(
                    JavaTimeModule()
                        .addDeserializer(
                            YearMonth::class.java,
                            // Denne trengs for å parse år over 9999 riktig.
                            YearMonthDeserializer(DateTimeFormatter.ofPattern("u-MM")),
                        ),
                ).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun toQueryParams(): LinkedMultiValueMap<String, String> {
        val writeValueAsString = objectMapper.writeValueAsString(this)
        val readValue: LinkedHashMap<String, Any?> = objectMapper.readValue(writeValueAsString)
        val queryParams = LinkedMultiValueMap<String, String>()
        readValue
            .filterNot { it.value == null }
            .filterNot { it.value is List<*> && (it.value as List<*>).isEmpty() }
            .forEach {
                if (it.value is List<*>) {
                    val liste = (it.value as List<*>).map { elem -> elem.toString() }
                    queryParams.addAll(it.key, liste)
                } else {
                    queryParams.add(it.key, it.value.toString())
                }
            }
        return queryParams
    }
}
