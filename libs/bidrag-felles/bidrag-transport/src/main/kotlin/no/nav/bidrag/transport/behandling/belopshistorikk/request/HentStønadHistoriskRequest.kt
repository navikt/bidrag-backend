package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.LocalDateTime

@Schema(description = "Request for å hente stønad og perioder som var gyldige på angitt tidspunkt")
data class HentStønadHistoriskRequest(
    @Schema(description = "Stønadstype")
    val type: Stønadstype,
    @Schema(description = "Referanse til sak")
    val sak: Saksnummer,
    @Schema(description = "Personidenten til den som skal betale stønaden")
    val skyldner: Personident,
    @Schema(description = "Personidenten til den som krever stønaden")
    val kravhaver: Personident,
    @Schema(description = "Tidspunkt som det ønskes å hente gyldige perioder for")
    val gyldigTidspunkt: LocalDateTime = LocalDateTime.now(),
)
