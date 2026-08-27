package no.nav.bidrag.commons.web.config

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.GenericHttpMessageConverter
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.Method

class RestOperationsAzureTest {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `custom jackson converter deserializes generic list response`() {
        val converter = CustomJacksonHttpMessageConverter()
        val listType = object : ParameterizedTypeReference<List<TestDto>>() {}.type
        val body = """[{"id":"1"},{"id":"2"}]"""

        converter.canRead(listType, null, MediaType.APPLICATION_JSON) shouldBe true

        val result = converter.read(listType, null, TestHttpInputMessage(body, MediaType.APPLICATION_JSON)) as List<*>

        result.shouldHaveSize(2)
        result[0] shouldBe TestDto("1")
        result[1] shouldBe TestDto("2")
    }

    private data class TestDto(
        val id: String,
    )

    private class TestHttpInputMessage(
        body: String,
        mediaType: MediaType,
    ) : HttpInputMessage {
        private val bodyBytes = body.toByteArray(Charsets.UTF_8)
        private val headers = HttpHeaders()

        init {
            headers.contentType = mediaType
        }

        override fun getHeaders(): HttpHeaders = headers

        override fun getBody(): InputStream = ByteArrayInputStream(bodyBytes)
    }
}
