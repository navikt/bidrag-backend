package no.nav.bidrag.commons.util

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.core.GenericTypeResolver
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractGenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.util.StreamUtils
import org.springframework.util.TypeUtils
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Optional

/**
 * Custom JSON message converter that uses the shared [commonObjectmapper] configuration.
 * This avoids ClassLoader/version conflicts by ensuring all deserialization
 * uses the same ObjectMapper instance.
 *
 * Modelled after Spring's AbstractJacksonHttpMessageConverter:
 * - Uses [GenericTypeResolver] for proper generic type resolution.
 * - Uses [StreamUtils.nonClosing] to avoid closing the underlying HTTP stream.
 * - Uses [com.fasterxml.jackson.databind.ObjectReader]/[com.fasterxml.jackson.databind.ObjectWriter]
 *   pattern for charset-aware, type-aware serialization.
 * - Wraps Jackson exceptions into [HttpMessageNotReadableException] / [HttpMessageNotWritableException].
 */
open class CustomJacksonHttpMessageConverter(
    private val objectMapper: ObjectMapper = commonObjectmapper,
) : AbstractGenericHttpMessageConverter<Any>(
        MediaType.APPLICATION_JSON,
        MediaType("application", "*+json"),
    ) {
    companion object {
        /**
         * Charset names that Jackson can auto-detect from the byte-order mark or JSON
         * encoding indicators — no need to wrap in an [InputStreamReader] for these.
         */
        private val UNICODE_CHARSET_NAMES =
            setOf(
                "UTF-8",
                "UTF-16",
                "UTF-16BE",
                "UTF-16LE",
                "UTF-32",
                "UTF-32BE",
                "UTF-32LE",
                "US-ASCII",
            )

        /** Maps Java charset names to Jackson's [JsonEncoding], mirroring Spring's own map. */
        private val JSON_ENCODINGS: Map<String, JsonEncoding> =
            JsonEncoding.entries.associateBy { it.javaName } +
                mapOf("US-ASCII" to JsonEncoding.UTF8)
    }

    /**
     * Handles all types except [ByteArray], which has a dedicated [org.springframework.http.converter.ByteArrayHttpMessageConverter].
     *
     * [String] is intentionally included: when the expected type is [String] and the content-type
     * is `application/json`, the raw body is returned as-is without Jackson involvement (see
     * [readJavaType] and [writeInternal]). This avoids double-encoding while keeping the converter
     * self-contained.
     */
    public override fun supports(clazz: Class<*>): Boolean = clazz != ByteArray::class.java

    override fun read(
        type: Type,
        contextClass: Class<*>?,
        inputMessage: HttpInputMessage,
    ): Any = readJavaType(getJavaType(type, contextClass), inputMessage)

    override fun readInternal(
        clazz: Class<out Any>,
        inputMessage: HttpInputMessage,
    ): Any = readJavaType(getJavaType(clazz, null), inputMessage)

    /**
     * Core read path — mirrors `AbstractJacksonHttpMessageConverter.readJavaType`.
     * Selects charset from the Content-Type header; falls back to UTF-8.
     * Uses a non-closing stream wrapper so Spring can still read response metadata
     * after deserialization.
     */
    private fun readJavaType(
        javaType: JavaType,
        inputMessage: HttpInputMessage,
    ): Any {
        val charset = resolveCharset(inputMessage.headers.contentType)

        // Short-circuit: String type with JSON content-type — return raw body without Jackson.
        // This prevents double-encoding and matches StringHttpMessageConverter behaviour.
        if (javaType.rawClass == String::class.java) {
            return StreamUtils.nonClosing(inputMessage.body).readBytes().toString(charset)
        }

        try {
            val stream = StreamUtils.nonClosing(inputMessage.body)
            val reader = customizeReader(objectMapper.readerFor(javaType), javaType)
            return if (charset.name() in UNICODE_CHARSET_NAMES) {
                reader.readValue(stream)
            } else {
                reader.readValue(InputStreamReader(stream, charset))
            }
        } catch (ex: InvalidDefinitionException) {
            throw HttpMessageConversionException("Type definition error: ${ex.type}", ex)
        } catch (ex: JsonProcessingException) {
            throw HttpMessageNotReadableException("JSON parse error: ${ex.originalMessage}", ex, inputMessage)
        }
    }

    /**
     * Core write path — mirrors `AbstractJacksonHttpMessageConverter.writeInternal`.
     * Uses [JsonEncoding] derived from the Content-Type charset for proper byte-level encoding.
     * Applies typed writing ([ObjectWriter.forType]) only for container types (e.g., [List], [Map],
     * [Optional]) AND only when the declared type is actually assignable from the runtime object —
     * guarding against incompatible Spring-internal types such as [org.springframework.core.ResolvableType].
     * Exposes [customizeWriter] extension hooks for subclasses.
     */
    override fun writeInternal(
        obj: Any,
        type: Type?,
        outputMessage: HttpOutputMessage,
    ) {
        val contentType = outputMessage.headers.contentType
        val encoding = resolveJsonEncoding(contentType)

        // Short-circuit: String type — write raw bytes without Jackson to prevent double-encoding.
        // A String body (e.g. pre-serialised JSON) must reach the wire verbatim.
        if (obj is String) {
            val charset = resolveCharset(contentType)
            StreamUtils.nonClosing(outputMessage.body).write(obj.toByteArray(charset))
            return
        }

        var javaType: JavaType? = null
        if (type != null) {
            try {
                val resolved = getJavaType(type, null)
                // Mirror Spring: only use the declared type when it is actually compatible with the
                // object's runtime class. Avoids using incompatible Spring-internal types.
                if (TypeUtils.isAssignable(type, obj.javaClass)) {
                    javaType = resolved
                }
            } catch (_: IllegalArgumentException) {
                // Type not resolvable by Jackson (e.g. ResolvableType$EmptyType); use untyped writer.
            }
        }

        var objectWriter = objectMapper.writer()
        if (javaType != null && (javaType.isContainerType || javaType.isTypeOrSubTypeOf(Optional::class.java))) {
            objectWriter = objectWriter.forType(javaType)
        }
        objectWriter = customizeWriter(objectWriter, javaType, contentType)

        try {
            val stream = StreamUtils.nonClosing(outputMessage.body)
            objectMapper.factory.createGenerator(stream, encoding).use { generator ->
                writePrefix(generator, obj)
                objectWriter.writeValue(generator, obj)
                writeSuffix(generator, obj)
                generator.flush()
            }
        } catch (ex: InvalidDefinitionException) {
            throw HttpMessageConversionException("Type definition error: ${ex.type}", ex)
        } catch (ex: JsonProcessingException) {
            throw HttpMessageNotWritableException("Could not write JSON: ${ex.originalMessage}", ex)
        }
    }

    /**
     * Write a prefix before the main content.
     * No-op by default; override in subclasses when needed (e.g. JSONP).
     * Mirrors `AbstractJacksonHttpMessageConverter.writePrefix`.
     */
    protected open fun writePrefix(
        generator: JsonGenerator,
        obj: Any,
    ) {}

    /**
     * Write a suffix after the main content.
     * No-op by default; override in subclasses when needed.
     * Mirrors `AbstractJacksonHttpMessageConverter.writeSuffix`.
     */
    protected open fun writeSuffix(
        generator: JsonGenerator,
        obj: Any,
    ) {}

    /**
     * Customise the [ObjectReader] before it is used to read the value.
     * No-op by default; override to apply views or other reader settings.
     * Mirrors `AbstractJacksonHttpMessageConverter.customizeReader`.
     */
    protected open fun customizeReader(
        reader: ObjectReader,
        javaType: JavaType,
    ): ObjectReader = reader

    /**
     * Customise the [ObjectWriter] before it is used to write the value.
     * No-op by default; override to apply views, filters or other settings.
     * Mirrors `AbstractJacksonHttpMessageConverter.customizeWriter`.
     *
     * @param writer the writer instance to customise
     * @param javaType the resolved [JavaType], or `null` if the type could not be resolved
     * @param contentType the selected [MediaType]
     */
    protected open fun customizeWriter(
        writer: ObjectWriter,
        javaType: JavaType?,
        contentType: MediaType?,
    ): ObjectWriter = writer

    /**
     * Return the Jackson [JavaType] for the specified type and context class.
     * Mirrors `AbstractJacksonHttpMessageConverter.getJavaType`.
     *
     * @param type the generic type to return the Jackson JavaType for
     * @param contextClass a context class for the target type (can be `null`)
     * @return the Jackson [JavaType]
     */
    protected fun getJavaType(
        type: Type,
        contextClass: Class<*>?,
    ): JavaType = objectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass))

    /**
     * Resolves the charset from the [MediaType], defaulting to UTF-8.
     * Mirrors `AbstractJacksonHttpMessageConverter.getCharset`.
     */
    private fun resolveCharset(contentType: MediaType?): Charset = contentType?.charset ?: StandardCharsets.UTF_8

    /**
     * Maps the [MediaType] charset to a Jackson [JsonEncoding], defaulting to UTF-8.
     * Mirrors `AbstractJacksonHttpMessageConverter.getJsonEncoding`.
     */
    private fun resolveJsonEncoding(contentType: MediaType?): JsonEncoding =
        contentType?.charset?.let { JSON_ENCODINGS[it.name()] } ?: JsonEncoding.UTF8
}
