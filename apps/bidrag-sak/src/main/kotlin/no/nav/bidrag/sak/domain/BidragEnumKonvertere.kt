package no.nav.bidrag.sak.domain

import jakarta.persistence.AttributeConverter
import no.nav.bidrag.domene.enums.behandling.HendelseType
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.TypeEndring
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Sakskategori
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE

@jakarta.persistence.Converter
class HendelseTypeConverter : AttributeConverter<HendelseType?, String?> {
    override fun convertToEntityAttribute(source: String?): HendelseType? = source?.let { HendelseType.fraKode(it) }

    override fun convertToDatabaseColumn(source: HendelseType?): String? = source?.kode
}

@jakarta.persistence.Converter
class RolletypeConverter : AttributeConverter<Rolletype, String?> {
    override fun convertToEntityAttribute(source: String?): Rolletype? = when (source) {
        "BP" -> Rolletype.BIDRAGSPLIKTIG
        "BM" -> Rolletype.BIDRAGSMOTTAKER
        "BA" -> Rolletype.BARN
        "FR" -> Rolletype.FEILREGISTRERT
        "RM" -> Rolletype.REELMOTTAKER
        else -> null
    }

    override fun convertToDatabaseColumn(source: Rolletype?): String? = when (source) {
        Rolletype.BIDRAGSPLIKTIG -> "BP"
        Rolletype.BIDRAGSMOTTAKER -> "BM"
        Rolletype.BARN -> "BA"
        Rolletype.FEILREGISTRERT -> "FR"
        Rolletype.REELMOTTAKER -> "RM"
        else -> null
    }
}

@jakarta.persistence.Converter
class TypeEndringConverter : AttributeConverter<TypeEndring, String?> {
    override fun convertToEntityAttribute(source: String?): TypeEndring? = when {
        source?.contains("Satt til BM manuelt", ignoreCase = true) == true -> TypeEndring.SATT_TIL_BM
        source?.contains("Satt RM manuelt", ignoreCase = true) == true -> TypeEndring.SATT_NY_RM
        source?.contains("Endret RM manuelt", ignoreCase = true) == true -> TypeEndring.SATT_RM
        source?.contains("RM-endring maskinelt", ignoreCase = true) == true -> TypeEndring.ENDRE_RM
        source?.contains("FNR-endring maskinelt", ignoreCase = true) == true -> TypeEndring.ENDRE_FNR
        else -> null
    }

    override fun convertToDatabaseColumn(source: TypeEndring?): String? = when (source) {
        TypeEndring.SATT_TIL_BM -> "Satt til BM manuelt"
        TypeEndring.SATT_NY_RM -> "Satt RM manuelt"
        TypeEndring.SATT_RM -> "Endret RM manuelt"
        TypeEndring.ENDRE_RM -> "RM-endring maskinelt"
        TypeEndring.ENDRE_FNR -> "FNR-endring maskinelt"
        else -> null
    }
}

@jakarta.persistence.Converter
class BooleanConverter : AttributeConverter<Boolean, Char> {
    override fun convertToEntityAttribute(source: Char): Boolean = if (source == '1') TRUE else FALSE

    override fun convertToDatabaseColumn(source: Boolean?): Char = if (source == true) '1' else '0'
}

@jakarta.persistence.Converter
class SakskategoriConverter : AttributeConverter<Sakskategori, String?> {
    override fun convertToEntityAttribute(source: String?): Sakskategori? = when (source) {
        "N" -> Sakskategori.NASJONAL
        "U" -> Sakskategori.UTLAND
        else -> null
    }

    override fun convertToDatabaseColumn(source: Sakskategori?): String? = when (source) {
        Sakskategori.NASJONAL -> "N"
        Sakskategori.UTLAND -> "U"
        else -> null
    }
}

@jakarta.persistence.Converter
class ArbeidsfordelingConverter : AttributeConverter<Arbeidsfordeling, String?> {
    override fun convertToEntityAttribute(source: String?): Arbeidsfordeling? = when (source) {
        "BBF" -> Arbeidsfordeling.BARNEBORTFØRING
        "EEN" -> Arbeidsfordeling.EIERENHET
        "EFS" -> Arbeidsfordeling.EKTEFELLLESAK
        "FRS" -> Arbeidsfordeling.FARSKAP
        "INH" -> Arbeidsfordeling.SETTEKONTOR
        "OPS" -> Arbeidsfordeling.OPPFOSTRINGSSAK
        "RKS" -> Arbeidsfordeling.REISEKOSTNADSAK
        else -> null
    }

    override fun convertToDatabaseColumn(source: Arbeidsfordeling?): String? = when (source) {
        Arbeidsfordeling.BARNEBORTFØRING -> "BBF"
        Arbeidsfordeling.EIERENHET -> "EEN"
        Arbeidsfordeling.EKTEFELLLESAK -> "EEN"
        Arbeidsfordeling.FARSKAP -> "FRS"
        Arbeidsfordeling.SETTEKONTOR -> "INH"
        Arbeidsfordeling.OPPFOSTRINGSSAK -> "OPS"
        Arbeidsfordeling.REISEKOSTNADSAK -> "RKS"
        else -> null
    }
}
