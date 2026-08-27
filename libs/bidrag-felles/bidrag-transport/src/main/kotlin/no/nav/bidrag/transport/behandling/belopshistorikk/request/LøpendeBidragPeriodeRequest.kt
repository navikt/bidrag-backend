package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.time.LocalDate

@Schema(description = "Request for å hente bidragssaker med perioder for skyldner i angitt periode")
data class LøpendeBidragPeriodeRequest(
    @param:Schema(description = "Skyldners personident")
    val skyldner: Personident,
    @param:Schema(description = "Periode som det ønskes å hente stønadsperioder for")
    val periode: ÅrMånedsperiode,
)
