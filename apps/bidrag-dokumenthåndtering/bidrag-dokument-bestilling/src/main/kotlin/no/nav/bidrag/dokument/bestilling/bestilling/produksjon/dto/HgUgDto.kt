package no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.dokument.bestilling.model.BehandlingType
import no.nav.bidrag.dokument.bestilling.model.SoknadType
import no.nav.bidrag.domene.enums.rolle.SøktAvType

data class HgUgDtoFromJson(
    @JsonProperty("SOKN_TYPE")
    val soknadType: String,
    @JsonProperty("SOKN_FRA_KODE")
    val soknadFra: String,
    @JsonProperty("KODE_SOKN_GR")
    val behandlingType: String,
    @JsonProperty("HG")
    val hg: String?,
    @JsonProperty("UG")
    val ug: String?,
)

data class HgUgDto(
    val soknadType: SoknadType?,
    val soknadFra: SøktAvType?,
    val behandlingType: BehandlingType?,
    val hg: String?,
    val ug: String?,
)
