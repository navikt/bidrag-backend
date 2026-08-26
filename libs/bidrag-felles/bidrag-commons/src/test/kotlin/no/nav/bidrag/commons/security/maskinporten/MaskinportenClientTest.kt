package no.nav.bidrag.commons.security.maskinporten

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.ServerSocket

class MaskinportenClientTest {
    // Server som aksepterer tilkoblinger, men aldri svarer — simulerer hengende Maskinporten.
    private val hengendeServer = ServerSocket(0)

    @AfterEach
    fun tearDown() {
        hengendeServer.close()
    }

    private fun maskinportenConfig(requestTimeoutInSeconds: Long) = MaskinportenConfig(
        tokenUrl = "http://localhost:${hengendeServer.localPort}/token",
        audience = "http://localhost:${hengendeServer.localPort}",
        clientId = "17b3e4e8-8203-4463-a947-5c24021b7742",
        privateKey = RSAKeyGenerator(2048).keyID("123").generate().toString(),
        validInSeconds = 120,
        scope = "skatt:testscope.read",
        connectTimeoutInSeconds = 1,
        requestTimeoutInSeconds = requestTimeoutInSeconds,
    )

    @Test
    fun `Skal kaste MaskinportenClientException ved timeout mot server som ikke svarer`() {
        val client = MaskinportenClient(maskinportenConfig(requestTimeoutInSeconds = 1))

        val exception =
            shouldThrow<MaskinportenClientException> {
                client.hentMaskinportenToken("skatt:testscope.read")
            }
        exception.message shouldContain "Timeout"
    }
}
