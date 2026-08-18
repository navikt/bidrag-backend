package no.nav.bidrag.samhandler.controller

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.samhandler.SpringTestRunner
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.kafka.test.context.EmbeddedKafka

@EmbeddedKafka(
    partitions = 1,
    brokerProperties = [
        "listeners=EXTERNAL://localhost:0,CONTROLLER://localhost:0",
        "listener.security.protocol.map=EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT",
        "controller.listener.names=CONTROLLER",
        "inter.broker.listener.name=EXTERNAL",
    ],
)
class SamhandlerControllerIntegrationTest : SpringTestRunner() {
    fun urlForPost() = rootUriComponentsBuilder().pathSegment("samhandler").build().toUri()

    @Test
    fun `post for Ident 204 om samhandler ikke finnes`() {
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<SamhandlerDto>(urlForPost(), Ident("80000000003"))

        responseEntity.statusCode shouldBe HttpStatus.NO_CONTENT
    }
}
