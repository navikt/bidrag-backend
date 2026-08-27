package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal
import java.time.LocalDate

data class SluttberegningForholdsmessigFordeling(
    override val periode: ÅrMånedsperiode,
    val saksnummer: Saksnummer,
    val datoBeregning: LocalDate? = null,
    val stønadstype: Stønadstype,
    val underholdskostnad: BigDecimal,
    val bpAndelUFaktor: BigDecimal,
    val bpAndelUBeløp: BigDecimal,
    val samværsfradrag: BigDecimal,
    val beregnetBidrag: BigDecimal,
    val faktiskBidrag: BigDecimal,
    val samværsklasse: Samværsklasse,
    val bidragsevne: BigDecimal,
    val bpsInntekt: BigDecimal,
    val tilleggsbidrag: BigDecimal? = null,
    val resultat: String,
    val forsvaretsBarnetillegg: Boolean = false,
    val status: BisysForholdsmessigFordelingStatus? = null,
) : Sluttberegning

enum class BisysForholdsmessigFordelingStatus {
    BEREGNET,
    UKJENT,
    FERDIG,
    UNDER_BEHANDLING,
    UTENLANDS_SAK,
}
