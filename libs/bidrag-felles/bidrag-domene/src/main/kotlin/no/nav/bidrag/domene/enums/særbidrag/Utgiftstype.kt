package no.nav.bidrag.domene.enums.særbidrag

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Utgiftstype(
    val kategori: Særbidragskategori,
) {
    KONFIRMASJONSAVGIFT(Særbidragskategori.KONFIRMASJON),
    KONFIRMASJONSLEIR(Særbidragskategori.KONFIRMASJON),
    SELSKAP(Særbidragskategori.KONFIRMASJON),
    KLÆR(Særbidragskategori.KONFIRMASJON),
    REISEUTGIFT(Særbidragskategori.KONFIRMASJON),

    TANNREGULERING(Særbidragskategori.TANNREGULERING),
    OPTIKK(Særbidragskategori.OPTIKK),
    ANNET(Særbidragskategori.ANNET),
}
