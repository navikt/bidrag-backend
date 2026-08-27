package no.nav.bidrag.domene.enums.sak

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArbeidsfordelingJsonTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `serialize long and short uses JsonProperty (bisys code)`() {
        // Because BARNEBORTFØRING is annotated with @JsonProperty("BBF"), serialization yields the short code
        assertEquals("\"BBF\"", mapper.writeValueAsString(Arbeidsfordeling.BARNEBORTFØRING))
        assertEquals(Arbeidsfordeling.BARNEBORTFØRING, mapper.readValue("\"BBF\"", Arbeidsfordeling::class.java))
        assertEquals(Arbeidsfordeling.BARNEBORTFØRING, mapper.readValue("\"BARNEBORTFØRING\"", Arbeidsfordeling::class.java))

        assertEquals("\"RKS\"", mapper.writeValueAsString(Arbeidsfordeling.REISEKOSTNADSAK))
        assertEquals(Arbeidsfordeling.REISEKOSTNADSAK, mapper.readValue("\"RKS\"", Arbeidsfordeling::class.java))
        assertEquals(Arbeidsfordeling.REISEKOSTNADSAK, mapper.readValue("\"REISEKOSTNADSAK\"", Arbeidsfordeling::class.java))

        assertEquals("\"EEN\"", mapper.writeValueAsString(Arbeidsfordeling.EIERENHET))
        assertEquals(Arbeidsfordeling.EIERENHET, mapper.readValue("\"EEN\"", Arbeidsfordeling::class.java))
        assertEquals(Arbeidsfordeling.EIERENHET, mapper.readValue("\"EIERENHET\"", Arbeidsfordeling::class.java))
    }
}
