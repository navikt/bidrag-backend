package no.nav.bidrag.transport.behandling.stonad.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import java.time.LocalDateTime

@Schema(description = "Request for å hente stønad og perioder som var gyldige på angitt tidspunkt", deprecated = true)
data class HentStønadHistoriskRequest(
    @Schema(description = "Stønadstype")
    val type: Stønadstype,
    @Schema(description = "Referanse til sak")
    val sak: Saksnummer,
    @Schema(description = "Personidenten til den som skal betale stønadem")
    val skyldner: Personident,
    @Schema(description = "Personidenten til den som krever stønadem")
    val kravhaver: Personident,
    @Schema(description = "Tidspunkt som det ønskes å hente gyldige perioder for")
    val gyldigTidspunkt: LocalDateTime = LocalDateTime.now(),
)
