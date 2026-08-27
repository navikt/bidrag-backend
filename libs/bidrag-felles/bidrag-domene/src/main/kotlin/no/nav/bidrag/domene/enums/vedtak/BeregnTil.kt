package no.nav.bidrag.domene.enums.vedtak

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class BeregnTil {
    @JsonAlias("OMGJORT_VEDTAK_VEDTAKSTIDSPUNKT")
    OPPRINNELIG_VEDTAKSTIDSPUNKT,

    INNEVÆRENDE_MÅNED,
    ETTERFØLGENDE_MANUELL_VEDTAK,
}
