package no.nav.bidrag.domene.enums.særbidrag

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Særbidragskategori {
    KONFIRMASJON,
    TANNREGULERING,
    OPTIKK,
    ANNET,
}
