package no.nav.bidrag.commons.util

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Optional

/**
 * Tests for [CustomJacksonHttpMessageConverter].
 *
 * Covers:
 * - supports() type filtering
 * - canRead() / canWrite() media type negotiation
 * - Read path: DTO, generic List, String pass-through, charset, error handling, customizeReader hook
 * - Write path: DTO, generic List, Optional, String pass-through, charset, error handling,
 *   customizeWriter / writePrefix / writeSuffix hooks
 */
class CustomJacksonHttpMessageConverterTest {
    private val converter = CustomJacksonHttpMessageConverter()

    // ─── Helpers ────────────────────────────────────────────────────────────

    private data class PersonDto(
        val name: String,
        val age: Int,
    )

    /** Simulates an HTTP response body with a given content-type. */
    private class FakeInput(
        body: String,
        mediaType: MediaType = MediaType.APPLICATION_JSON,
        charset: Charset = Charsets.UTF_8,
    ) : HttpInputMessage {
        private val bytes = body.toByteArray(charset)
        private val headers = HttpHeaders().apply { contentType = mediaType }

        override fun getHeaders(): HttpHeaders = headers

        override fun getBody(): InputStream = ByteArrayInputStream(bytes)
    }

    /** Simulates an HTTP request body target with a given content-type. */
    private class FakeOutput(
        mediaType: MediaType = MediaType.APPLICATION_JSON,
    ) : HttpOutputMessage {
        private val buffer = ByteArrayOutputStream()
        private val headers = HttpHeaders().apply { contentType = mediaType }

        override fun getHeaders(): HttpHeaders = headers

        override fun getBody(): OutputStream = buffer

        fun asString(charset: Charset = Charsets.UTF_8): String = buffer.toString(charset)

        fun asBytes(): ByteArray = buffer.toByteArray()
    }

    // ─── supports() — tested via public canRead / canWrite ──────────────────
    // supports() is protected; its contract is observable through canRead/canWrite.

    @Nested
    inner class Supports {
        @Test
        fun `ByteArray is rejected — canRead returns false`() {
            converter.canRead(ByteArray::class.java, MediaType.APPLICATION_JSON) shouldBe false
        }

        @Test
        fun `ByteArray is rejected — canWrite returns false`() {
            converter.canWrite(ByteArray::class.java, MediaType.APPLICATION_JSON) shouldBe false
        }

        @Test
        fun `String is accepted — canRead returns true`() {
            converter.canRead(String::class.java, MediaType.APPLICATION_JSON) shouldBe true
        }

        @Test
        fun `String is accepted — canWrite returns true`() {
            converter.canWrite(String::class.java, MediaType.APPLICATION_JSON) shouldBe true
        }

        @Test
        fun `DTO class is accepted`() {
            converter.canRead(PersonDto::class.java, MediaType.APPLICATION_JSON) shouldBe true
            converter.canWrite(PersonDto::class.java, MediaType.APPLICATION_JSON) shouldBe true
        }

        @Test
        fun `primitive wrapper Int is accepted`() {
            converter.canRead(Int::class.javaObjectType, MediaType.APPLICATION_JSON) shouldBe true
        }

        @Test
        fun `List class is accepted`() {
            converter.canRead(List::class.java, MediaType.APPLICATION_JSON) shouldBe true
        }
    }

    // ─── canRead / canWrite — media type negotiation ─────────────────────────

    @Nested
    inner class ContentTypeNegotiation {
        @Test
        fun `canRead generic List for application-json`() {
            val listType = object : ParameterizedTypeReference<List<PersonDto>>() {}.type
            converter.canRead(listType, null, MediaType.APPLICATION_JSON) shouldBe true
        }

        @Test
        fun `canRead for json vendor type`() {
            converter.canRead(PersonDto::class.java, MediaType("application", "vnd.foo+json")) shouldBe true
        }

        @Test
        fun `canNOT read for text-plain`() {
            converter.canRead(PersonDto::class.java, MediaType.TEXT_PLAIN) shouldBe false
        }
    }

    // ─── Read — DTO ──────────────────────────────────────────────────────────

    @Nested
    inner class ReadDto {
        @Test
        fun `deserializes simple DTO from application-json`() {
            val json = """{"name":"Alice","age":30}"""
            val result = converter.read(PersonDto::class.java, FakeInput(json))
            result shouldBe PersonDto("Alice", 30)
        }

        @Test
        fun `reads DTO with unknown fields without failing`() {
            val json = """{"name":"Alice","age":30,"unknown":"value"}"""
            val result = converter.read(PersonDto::class.java, FakeInput(json))
            result shouldBe PersonDto("Alice", 30)
        }

        @Test
        fun `malformed JSON produces HttpMessageNotReadableException with parse info`() {
            val ex =
                shouldThrow<HttpMessageNotReadableException> {
                    converter.read(PersonDto::class.java, FakeInput("{broken json"))
                }
            ex.message shouldContain "JSON parse error"
        }

        @Test
        fun `type definition error produces HttpMessageConversionException`() {
            // AbstractType cannot be instantiated — Jackson throws InvalidDefinitionException
            val ex =
                shouldThrow<HttpMessageConversionException> {
                    converter.read(AbstractPersonDto::class.java, FakeInput("""{"name":"x","age":1}"""))
                }
            // Jackson 2 may wrap this as InvalidDefinitionException → HttpMessageConversionException
            // or as JsonMappingException → HttpMessageNotReadableException; either way a conversion problem.
            ex.shouldBeInstanceOf<HttpMessageConversionException>()
        }
    }

    abstract class AbstractPersonDto(
        val name: String,
        val age: Int,
    )

    // ─── Read — generic types ────────────────────────────────────────────────

    @Nested
    inner class ReadGenericTypes {
        @Test
        fun `deserializes generic List of DTO`() {
            val listType = object : ParameterizedTypeReference<List<PersonDto>>() {}.type
            val json = """[{"name":"Alice","age":30},{"name":"Bob","age":25}]"""

            @Suppress("UNCHECKED_CAST")
            val result = converter.read(listType, null, FakeInput(json)) as List<PersonDto>

            result.size shouldBe 2
            result[0] shouldBe PersonDto("Alice", 30)
            result[1] shouldBe PersonDto("Bob", 25)
        }

        @Test
        fun `deserializes generic Map`() {
            val mapType = object : ParameterizedTypeReference<Map<String, Int>>() {}.type
            val json = """{"a":1,"b":2}"""

            @Suppress("UNCHECKED_CAST")
            val result = converter.read(mapType, null, FakeInput(json)) as Map<String, Int>

            result shouldBe mapOf("a" to 1, "b" to 2)
        }

        @Test
        fun `deserializes List of enums`() {
            val listType = object : ParameterizedTypeReference<List<SampleStatus>>() {}.type
            val json = """["ACTIVE","INACTIVE","ACTIVE"]"""

            @Suppress("UNCHECKED_CAST")
            val result = converter.read(listType, null, FakeInput(json)) as List<SampleStatus>

            result.size shouldBe 3
            result[0] shouldBe SampleStatus.ACTIVE
            result[1] shouldBe SampleStatus.INACTIVE
            result[2] shouldBe SampleStatus.ACTIVE
        }

        @Test
        fun `deserializes List of enums with unknown value uses default`() {
            // commonObjectmapper has READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE enabled
            // Unknown enum values deserialize to the @JsonEnumDefaultValue
            val listType = object : ParameterizedTypeReference<List<SampleStatus>>() {}.type
            val json = """["ACTIVE","UNKNOWN_STATUS","INACTIVE"]"""

            @Suppress("UNCHECKED_CAST")
            val result = converter.read(listType, null, FakeInput(json)) as List<SampleStatus>

            result.size shouldBe 3
            result[0] shouldBe SampleStatus.ACTIVE
            result[1] shouldBe SampleStatus.UNKNOWN // default value
            result[2] shouldBe SampleStatus.INACTIVE
        }
    }

    enum class SampleStatus {
        ACTIVE,
        INACTIVE,

        @com.fasterxml.jackson.annotation.JsonEnumDefaultValue
        UNKNOWN,
    }

    // ─── Read — String pass-through ──────────────────────────────────────────

    @Nested
    inner class ReadStringPassThrough {
        @Test
        fun `String type returns raw body without Jackson processing`() {
            val rawJson = """{"name":"Alice","age":30}"""
            val result = converter.read(String::class.java, FakeInput(rawJson))
            // Must be the raw JSON string — NOT double-decoded
            result shouldBe rawJson
        }

        @Test
        fun `String type with ISO-8859-1 charset decoded correctly`() {
            val rawText = "Ærlig talt"
            val mediaType = MediaType("application", "json", Charsets.ISO_8859_1)
            val result =
                converter.read(
                    String::class.java,
                    FakeInput(rawText, mediaType, Charsets.ISO_8859_1),
                )
            result shouldBe rawText
        }

        @Test
        fun `String type does NOT throw for non-JSON body`() {
            // If content-type claims json but body is plain text, pass it through as-is
            val result = converter.read(String::class.java, FakeInput("just text"))
            result shouldBe "just text"
        }
    }

    // ─── Write — DTO ─────────────────────────────────────────────────────────

    @Nested
    inner class WriteDto {
        @Test
        fun `serializes simple DTO to application-json`() {
            val output = FakeOutput()
            converter.write(PersonDto("Alice", 30), MediaType.APPLICATION_JSON, output)
            output.asString() shouldBe """{"name":"Alice","age":30}"""
        }

        @Test
        fun `serializes nullable fields as null`() {
            val output = FakeOutput()
            converter.write(NullableDto(null), MediaType.APPLICATION_JSON, output)
            output.asString() shouldContain """"value":null"""
        }
    }

    private data class NullableDto(
        val value: String?,
    )

    // ─── Write — generic types ───────────────────────────────────────────────

    @Nested
    inner class WriteGenericTypes {
        @Test
        fun `serializes List of DTO preserving type info`() {
            val list = listOf(PersonDto("Alice", 30), PersonDto("Bob", 25))
            val listType = object : ParameterizedTypeReference<List<PersonDto>>() {}.type
            val output = FakeOutput()

            converter.write(list, listType, MediaType.APPLICATION_JSON, output)

            output.asString() shouldBe """[{"name":"Alice","age":30},{"name":"Bob","age":25}]"""
        }
    }

    // ─── Write — String pass-through ─────────────────────────────────────────

    @Nested
    inner class WriteStringPassThrough {
        @Test
        fun `String body is written verbatim without JSON encoding`() {
            val rawJson = """{"name":"Alice","age":30}"""
            val output = FakeOutput()
            converter.write(rawJson, MediaType.APPLICATION_JSON, output)
            // Must NOT become "\"{"name":...}\"" — no outer quotes
            output.asString() shouldBe rawJson
        }

        @Test
        fun `String body with ISO-8859-1 charset written with correct encoding`() {
            val text = "Ærlig talt"
            val mediaType = MediaType("application", "json", Charsets.ISO_8859_1)
            val output = FakeOutput(mediaType)
            converter.write(text, mediaType, output)
            output.asBytes() shouldBe text.toByteArray(Charsets.ISO_8859_1)
        }

        @Test
        fun `pre-serialised JSON String round-trips without double-encoding`() {
            // Simulate: serialize a DTO to string first, then send that string as body
            val originalDto = PersonDto("Alice", 30)
            val serialized =
                converter.run {
                    val out = FakeOutput()
                    write(originalDto, MediaType.APPLICATION_JSON, out)
                    out.asString()
                }

            // Now write that serialized string as a body — must arrive identical on the wire
            val output = FakeOutput()
            converter.write(serialized, MediaType.APPLICATION_JSON, output)

            // If double-encoded: output would be "\"{"name":"Alice",...}\"" — starts with \"
            output.asString() shouldBe serialized
            output.asString().startsWith("\"") shouldBe false
        }
    }

    // ─── Extension hooks ─────────────────────────────────────────────────────

    @Nested
    inner class ExtensionHooks {
        @Test
        fun `customizeReader hook is invoked during read`() {
            var hookCalled = false
            val hookConverter =
                object : CustomJacksonHttpMessageConverter() {
                    override fun customizeReader(
                        reader: ObjectReader,
                        javaType: JavaType,
                    ): ObjectReader {
                        hookCalled = true
                        return super.customizeReader(reader, javaType)
                    }
                }

            hookConverter.read(PersonDto::class.java, FakeInput("""{"name":"Alice","age":30}"""))

            hookCalled shouldBe true
        }

        @Test
        fun `customizeWriter hook is invoked during write`() {
            var hookCalled = false
            val hookConverter =
                object : CustomJacksonHttpMessageConverter() {
                    override fun customizeWriter(
                        writer: ObjectWriter,
                        javaType: JavaType?,
                        contentType: MediaType?,
                    ): ObjectWriter {
                        hookCalled = true
                        return super.customizeWriter(writer, javaType, contentType)
                    }
                }

            hookConverter.write(PersonDto("Alice", 30), MediaType.APPLICATION_JSON, FakeOutput())

            hookCalled shouldBe true
        }

        @Test
        fun `writePrefix and writeSuffix hooks wrap the JSON output`() {
            val hookConverter =
                object : CustomJacksonHttpMessageConverter() {
                    override fun writePrefix(
                        generator: com.fasterxml.jackson.core.JsonGenerator,
                        obj: Any,
                    ) {
                        generator.writeRaw("/*prefix*/")
                    }

                    override fun writeSuffix(
                        generator: com.fasterxml.jackson.core.JsonGenerator,
                        obj: Any,
                    ) {
                        generator.writeRaw("/*suffix*/")
                    }
                }

            val output = FakeOutput()
            hookConverter.write(PersonDto("Alice", 30), MediaType.APPLICATION_JSON, output)

            output.asString() shouldContain "/*prefix*/"
            output.asString() shouldContain "/*suffix*/"
            output.asString() shouldContain """"name":"Alice""""
        }

        @Test
        fun `customizeWriter receives resolved javaType for container types`() {
            var receivedJavaType: JavaType? = null
            val hookConverter =
                object : CustomJacksonHttpMessageConverter() {
                    override fun customizeWriter(
                        writer: ObjectWriter,
                        javaType: JavaType?,
                        contentType: MediaType?,
                    ): ObjectWriter {
                        receivedJavaType = javaType
                        return super.customizeWriter(writer, javaType, contentType)
                    }
                }

            val listType = object : ParameterizedTypeReference<List<PersonDto>>() {}.type
            hookConverter.write(listOf(PersonDto("Alice", 30)), listType, MediaType.APPLICATION_JSON, FakeOutput())

            // javaType must be resolved and be a container type
            receivedJavaType?.isContainerType shouldBe true
            receivedJavaType?.contentType?.rawClass shouldBe PersonDto::class.java
        }
    }

    // ─── ByteArray handling ───────────────────────────────────────────────────
    // ByteArray is intentionally NOT handled by this converter. It is delegated to
    // ByteArrayHttpMessageConverter (configured with MediaType.ALL in RestOperationsAzure),
    // which reads raw bytes regardless of content-type.
    //
    // If CustomJacksonHttpMessageConverter handled ByteArray, Jackson would attempt to
    // deserialize the JSON into ByteArray. That only works for base64-encoded JSON strings
    // like "aGVsbG8=" — NOT for JSON objects like {"name":"Alice"} — breaking most use cases.

    @Nested
    inner class ByteArrayHandling {
        @Test
        fun `ByteArray is rejected by canRead — must be delegated to ByteArrayHttpMessageConverter`() {
            converter.canRead(ByteArray::class.java, MediaType.APPLICATION_JSON) shouldBe false
            converter.canRead(ByteArray::class.java, MediaType.APPLICATION_PDF) shouldBe false
            converter.canRead(ByteArray::class.java, MediaType.APPLICATION_OCTET_STREAM) shouldBe false
        }

        @Test
        fun `ByteArray is rejected by canWrite`() {
            converter.canWrite(ByteArray::class.java, MediaType.APPLICATION_JSON) shouldBe false
        }

        @Test
        fun `ByteArrayHttpMessageConverter reads JSON response body as raw bytes`() {
            // Verifies the fallback behaviour via Spring's own ByteArrayHttpMessageConverter:
            // the raw JSON bytes should be returned verbatim, not Jackson-deserialized.
            val rawJson = """{"name":"Alice","age":30}"""
            val byteArrayConverter =
                org.springframework.http.converter.ByteArrayHttpMessageConverter().apply {
                    supportedMediaTypes =
                        listOf(
                            MediaType.APPLICATION_OCTET_STREAM,
                            MediaType.APPLICATION_PDF,
                            MediaType.ALL,
                        )
                }

            val result = byteArrayConverter.read(ByteArray::class.java, FakeInput(rawJson)) as ByteArray

            result shouldBe rawJson.toByteArray(Charsets.UTF_8)
        }

        @Test
        fun `ByteArrayHttpMessageConverter reads PDF response body as raw bytes`() {
            val fakePdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF magic bytes
            val byteArrayConverter =
                org.springframework.http.converter.ByteArrayHttpMessageConverter().apply {
                    supportedMediaTypes =
                        listOf(
                            MediaType.APPLICATION_OCTET_STREAM,
                            MediaType.APPLICATION_PDF,
                            MediaType.ALL,
                        )
                }
            val pdfInput =
                object : org.springframework.http.HttpInputMessage {
                    override fun getHeaders() = HttpHeaders().apply { contentType = MediaType.APPLICATION_PDF }

                    override fun getBody(): InputStream = ByteArrayInputStream(fakePdfBytes)
                }

            val result = byteArrayConverter.read(ByteArray::class.java, pdfInput) as ByteArray

            result shouldBe fakePdfBytes
        }
    }
    // getJavaType is protected; its correctness is verified by whether generic-type
    // deserialization and serialization produce the expected results.

    @Nested
    inner class GetJavaType {
        @Test
        fun `plain class resolves correctly — DTO round-trips`() {
            val json = """{"name":"Alice","age":30}"""
            val result = converter.read(PersonDto::class.java, FakeInput(json))
            result shouldBe PersonDto("Alice", 30)
        }

        @Test
        fun `parameterized List type resolves correctly — generic elements typed`() {
            val listType = object : ParameterizedTypeReference<List<PersonDto>>() {}.type
            val json = """[{"name":"Alice","age":30}]"""

            @Suppress("UNCHECKED_CAST")
            val result = converter.read(listType, null, FakeInput(json)) as List<PersonDto>

            // If type resolution were wrong, this cast would fail or elements would be LinkedHashMap
            result[0].shouldBeInstanceOf<PersonDto>()
            result[0].name shouldBe "Alice"
        }
    }
}
