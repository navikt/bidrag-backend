package no.nav.bidrag.sak.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.bidrag.domene.sak.Saksnummer

@Converter
class SaksnummerConverter : AttributeConverter<Saksnummer?, String?> {
    override fun convertToDatabaseColumn(attribute: Saksnummer?): String? = attribute?.verdi

    override fun convertToEntityAttribute(dbData: String?): Saksnummer? = if (!dbData.isNullOrBlank()) {
        Saksnummer(dbData)
    } else {
        null
    }
}
