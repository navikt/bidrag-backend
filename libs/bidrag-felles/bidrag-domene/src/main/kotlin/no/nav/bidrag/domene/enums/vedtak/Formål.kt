package no.nav.bidrag.domene.enums.vedtak

enum class Formål {
    FORSKUDD,
    BIDRAG,

    @Deprecated("SÆRTILSKUDD er erstattet med SÆRBIDRAG", ReplaceWith("SÆRBIDRAG"))
    SÆRTILSKUDD,
    SÆRBIDRAG,
}
