package no.nav.bidrag.domene.enums.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Unntakskode(
    val legacyKode: String,
) {
    ENSLIG_ADOPTANT("EAD"),
    EN_FORELDRE_DØD("EFD"),
    FARSKAP_IKKE_FASTSLÅTT("FIF"),
    HJEMMEL_SATT_MANUELT("MAN"),
    ;

    companion object {
        fun fraLegacyKode(legacyKode: String): Unntakskode? =
            try {
                enumValues<Unntakskode>().find { it.legacyKode == legacyKode } ?: Unntakskode.valueOf(legacyKode)
            } catch (e: Exception) {
                null
            }
    }
}
