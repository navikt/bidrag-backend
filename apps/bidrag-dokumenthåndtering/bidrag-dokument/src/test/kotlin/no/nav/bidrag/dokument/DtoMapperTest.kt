package no.nav.bidrag.dokument

import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.IdentType
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper
import java.io.IOException

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = [BidragDokumentTest::class])
@ActiveProfiles(TEST_PROFILE)
@EnableMockOAuth2Server
@AutoConfigureTestRestTemplate
internal class DtoMapperTest {
    @Autowired
    private val objectMapper: JsonMapper? = null

    @Test
    @DisplayName("skal mappe aktør til json og tilbake")
    @Throws(IOException::class)
    fun skalMappeAktorTilJson() {
        val ident = genererFødselsnummer()
        val json = objectMapper!!.writeValueAsString(AktorDto(ident, IdentType.FNR))

        Assertions.assertThat(json).contains("\"ident\":\"$ident\"")

        println(json)
        val deserialisert = objectMapper.readValue<AktorDto?>(json, AktorDto::class.java)

        Assertions.assertThat<AktorDto?>(deserialisert).isEqualTo(AktorDto(ident, IdentType.FNR))
    }

    @Test
    @DisplayName("skal mappe BestillOriginal til json og tilbake")
    @Throws(IOException::class)
    fun skalMappeBestillOriginalTilJson() {
        val enhentsnummer = "4806"
        val json =
            objectMapper!!.writeValueAsString(
                Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, enhentsnummer),
            )

        Assertions.assertThat(json).contains("\"avvikType\":\"BESTILL_ORIGINAL\"")

        println(json)
        val deserialisert = objectMapper.readValue<Avvikshendelse?>(json, Avvikshendelse::class.java)

        Assertions.assertThat<Avvikshendelse?>(deserialisert).isEqualTo(
            Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, enhentsnummer),
        )
    }
}
