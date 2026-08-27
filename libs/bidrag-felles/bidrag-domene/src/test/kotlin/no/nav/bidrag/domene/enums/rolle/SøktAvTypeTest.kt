package no.nav.bidrag.domene.enums.rolle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SøktAvTypeTest {
    val objectmapper = ObjectMapper().findAndRegisterModules().registerKotlinModule()

    @Test
    fun `Skal skrive BARN_18_ÅR til string`() {
        objectmapper.writeValueAsString(SøktAvType.BARN_18_ÅR) shouldBe "\"BARN_18_ÅR\""
    }

    @Test
    fun `Skal lese BARN_18_AAR til BARN_18_ÅR`() {
        objectmapper.readValue("\"BARN_18_ÅR\"", SøktAvType::class.java) shouldBe SøktAvType.BARN_18_ÅR
        objectmapper.readValue("\"BARN_18_AAR\"", SøktAvType::class.java) shouldBe SøktAvType.BARN_18_ÅR
    }
}
