package no.nav.bidrag.domene.enums.sjablon

enum class SjablonInnholdNavn(
    val navn: String,
) {
    BARNETILSYN_BELØP("BarnetilsynBeløp"),
    BOUTGIFT_BELØP("BoutgiftBeløp"),
    UNDERHOLD_BELØP("UnderholdBeløp"),
    FORBRUK_TOTAL_BELØP("ForbrukTotalBeløp"),
    MAKS_FRADRAG_BELØP("MaksFradragBeløp"),
    MAKS_TILSYN_BELØP("MaksTilsynBeløp"),
    ANTALL_DAGER_TOM("AntallDagerTOM"),
    ANTALL_NETTER_TOM("AntallNetterTOM"),
    FRADRAG_BELØP("FradragBeløp"),
    SJABLON_VERDI("SjablonVerdi"),
    INNTEKTSGRENSE_BELØP("InntektsgrenseBeløp"),
    SKATTESATS_PROSENT("SkattesatsProsent"),
}
