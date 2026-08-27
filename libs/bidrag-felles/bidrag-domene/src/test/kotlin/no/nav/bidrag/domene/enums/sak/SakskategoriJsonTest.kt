package no.nav.bidrag.domene.enums.sak

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SakskategoriJsonTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `serialize long and short uses JsonProperty (bisys code)`() {
        // Because BARNEBORTFØRING is annotated with @JsonProperty("BBF"), serialization yields the short code
        assertEquals("\"N\"", mapper.writeValueAsString(Sakskategori.NASJONAL))
        assertEquals(Sakskategori.NASJONAL, mapper.readValue("\"N\"", Sakskategori::class.java))
        assertEquals(Sakskategori.NASJONAL, mapper.readValue("\"NASJONAL\"", Sakskategori::class.java))

        assertEquals("\"U\"", mapper.writeValueAsString(Sakskategori.UTLAND))
        assertEquals(Sakskategori.UTLAND, mapper.readValue("\"U\"", Sakskategori::class.java))
        assertEquals(Sakskategori.UTLAND, mapper.readValue("\"UTLAND\"", Sakskategori::class.java))
    }
}
