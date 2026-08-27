package no.nav.bidrag.domene.felles

import org.springframework.core.convert.converter.Converter
import java.util.Locale

internal class StringToBooleanConverter : Converter<String, Boolean> {
    private val trueValues = setOf("true", "on", "yes", "1")

    private val falseValues = setOf("false", "off", "no", "0")

    override fun convert(source: String): Boolean {
        val value = source.trim().lowercase(Locale.getDefault())
        return if (trueValues.contains(value)) {
            true
        } else if (falseValues.contains(value)) {
            false
        } else {
            throw IllegalArgumentException("Invalid boolean value '$source'")
        }
    }
}
