package no.nav.bidrag.admin.dto

import no.nav.bidrag.admin.persistence.entity.DriftsmeldingStatus
import java.time.LocalDateTime

data class OpprettDriftsmeldingRequest(
    val tittel: String,
    val aktivFraTidspunkt: LocalDateTime? = null,
    val aktivTilTidspunkt: LocalDateTime? = null,
    val innhold: String,
    val status: DriftsmeldingStatus,
)

data class OppdaterDriftsmeldingRequest(
    val tittel: String? = null,
    val aktivFraTidspunkt: LocalDateTime? = null,
    val aktivTilTidspunkt: LocalDateTime? = null,
)

data class OppdaterDriftsmeldingHistorikkRequest(
    val innhold: String? = null,
    val aktivFraTidspunkt: LocalDateTime? = null,
    val aktivTilTidspunkt: LocalDateTime? = null,
    val status: DriftsmeldingStatus? = null,
)

data class LeggTilDriftsmeldingHistorikkRequest(
    val innhold: String,
    val aktivFraTidspunkt: LocalDateTime? = null,
    val aktivTilTidspunkt: LocalDateTime? = null,
    val status: DriftsmeldingStatus,
)
