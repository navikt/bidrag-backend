package no.nav.bidrag.transport.felles

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// Bisys bruker eldre versjon av jackson og støtter derfor ikke JsonMapper
val commonObjectmapper =
    ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

fun tilJsonString(value: Any): String = commonObjectmapper.writeValueAsString(value)
