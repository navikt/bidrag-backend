package no.nav.bidrag.dokument.produksjon.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.util.StdDateFormat
import no.nav.bidrag.domene.enums.rolle.Rolletype
import java.io.IOException
import java.time.YearMonth

fun getObjectmapper(): ObjectMapper {
    val mapper = ObjectMapper().findAndRegisterModules()
    val module = SimpleModule()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
    mapper.dateFormat = StdDateFormat()
    module.addDeserializer(YearMonth::class.java, YearMontDeserializer())
    module.addSerializer(YearMonth::class.java, YearMonthSerializer())
    module.addSerializer(Rolletype::class.java, RolletypeSerializer())
    mapper.registerModule(module)
    return mapper
}

class YearMontDeserializer(
    t: Class<YearMonth>? = null,
) : StdDeserializer<YearMonth?>(t) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(
        jp: JsonParser,
        ctxt: DeserializationContext?,
    ): YearMonth {
        val node: JsonNode = jp.codec.readTree(jp)
        return formatToYearMonth(node.asText())
    }
}

class YearMonthSerializer(
    t: Class<YearMonth>? = null,
) : StdSerializer<YearMonth>(t) {
    override fun serialize(
        value: YearMonth,
        gen: JsonGenerator,
        provider: SerializerProvider,
    ) {
        gen.writeString(value.atDay(1).toString())
    }
}

class RolletypeSerializer(
    t: Class<Enum<Rolletype>>? = null,
) : StdSerializer<Enum<Rolletype>>(t) {
    override fun serialize(
        value: Enum<Rolletype>,
        gen: JsonGenerator,
        provider: SerializerProvider,
    ) {
        gen.writeString(value.name)
    }
}
