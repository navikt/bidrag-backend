package no.nav.bidrag.dokument.produksjon

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.bidrag.dokument.produksjon.consumer.BidragDokumentmalConsumer
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.restclient.autoconfigure.RestTemplateAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class RestConfigTest {
    @Test
    fun `skal opprette dokumentmalklienten med applikasjonens restTemplateBuilder`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration::class.java))
            .withUserConfiguration(RestConfig::class.java, BidragDokumentmalConsumer::class.java)
            .withPropertyValues("bidrag-dokumentmal.url=http://localhost")
            .run { context ->
                context.getBean(BidragDokumentmalConsumer::class.java).shouldNotBeNull()
            }
    }
}
