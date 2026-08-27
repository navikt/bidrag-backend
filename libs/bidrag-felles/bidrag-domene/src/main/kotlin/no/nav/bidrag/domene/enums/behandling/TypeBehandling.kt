package no.nav.bidrag.domene.enums.behandling

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class TypeBehandling {
    FORSKUDD,
    SÆRBIDRAG,
    BIDRAG,
    BIDRAG_18_ÅR,
}
