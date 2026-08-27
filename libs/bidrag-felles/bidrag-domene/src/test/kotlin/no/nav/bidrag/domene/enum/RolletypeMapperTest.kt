package no.nav.bidrag.domene.enum

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.rolle.Rolletype
import org.junit.jupiter.api.Test

class RolletypeMapperTest {
    val objectmapper = ObjectMapper().findAndRegisterModules().registerKotlinModule()

    @Test
    fun `skal serialisere rolletype BM til BIDRAGSMOTTKER`() {
        objectmapper.writeValueAsString(Rolletype.BIDRAGSMOTTAKER) shouldBe "\"BM\""
        objectmapper.writeValueAsString(Rolletype.BIDRAGSPLIKTIG) shouldBe "\"BP\""
        objectmapper.writeValueAsString(Rolletype.BARN) shouldBe "\"BA\""
        objectmapper.writeValueAsString(Rolletype.REELMOTTAKER) shouldBe "\"RM\""
        objectmapper.writeValueAsString(Rolletype.FEILREGISTRERT) shouldBe "\"FR\""
    }

    @Test
    fun `skal deserialisere rolletype`() {
        objectmapper.readValue("\"BARN\"", Rolletype::class.java) shouldBe Rolletype.BARN
        objectmapper.readValue("\"BA\"", Rolletype::class.java) shouldBe Rolletype.BARN

        objectmapper.readValue("\"BIDRAGSMOTTAKER\"", Rolletype::class.java) shouldBe Rolletype.BIDRAGSMOTTAKER
        objectmapper.readValue("\"BM\"", Rolletype::class.java) shouldBe Rolletype.BIDRAGSMOTTAKER

        objectmapper.readValue("\"BIDRAGSPLIKTIG\"", Rolletype::class.java) shouldBe Rolletype.BIDRAGSPLIKTIG
        objectmapper.readValue("\"BP\"", Rolletype::class.java) shouldBe Rolletype.BIDRAGSPLIKTIG

        objectmapper.readValue("\"REELMOTTAKER\"", Rolletype::class.java) shouldBe Rolletype.REELMOTTAKER
        objectmapper.readValue("\"RM\"", Rolletype::class.java) shouldBe Rolletype.REELMOTTAKER
    }
}
