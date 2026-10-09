package no.nav.bidrag.automatiskjobb.configuration

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

class KafkaConfigurationTest {
    @Test
    fun `404 som klientfeil skal ikke prøves på nytt`() {
        val feil = ListenerExecutionFailedException("feil", HttpClientErrorException(HttpStatus.NOT_FOUND))

        feil.erIkkeFunnet() shouldBe true
    }

    @Test
    fun `404 som serverfeil skal ikke prøves på nytt`() {
        val feil = ListenerExecutionFailedException("feil", HttpServerErrorException(HttpStatus.NOT_FOUND))

        feil.erIkkeFunnet() shouldBe true
    }

    @Test
    fun `400 skal prøves på nytt`() {
        val feil = ListenerExecutionFailedException("feil", HttpClientErrorException(HttpStatus.BAD_REQUEST))

        feil.erIkkeFunnet() shouldBe false
    }

    @Test
    fun `500 skal prøves på nytt`() {
        val feil = ListenerExecutionFailedException("feil", HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))

        feil.erIkkeFunnet() shouldBe false
    }
}
