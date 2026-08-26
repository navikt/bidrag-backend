package no.nav.bidrag.domene.enums.vedtak

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class VedtaksforslagStatus {
    OPPRETTET,
    OPPDATERT,
    SLETTET,
    FATTET,
}
