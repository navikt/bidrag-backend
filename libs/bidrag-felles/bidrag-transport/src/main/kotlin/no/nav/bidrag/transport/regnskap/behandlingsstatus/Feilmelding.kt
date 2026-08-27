package no.nav.bidrag.transport.regnskap.behandlingsstatus

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode

@Schema(name = "Behandlingstatus feilmelding", description = "Feilmelding i responsen ved kall mot behandlingsstatus med batch-uid.")
data class Feilmelding(
    val feilkode: String?,
    val fagsystemId: String?,
    val transaksjonskode: Transaksjonskode?,
    val delytelseId: String?,
    val periode: String?,
    val feilmelding: String,
)
