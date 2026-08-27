package no.nav.bidrag.transport.behandling.vedtak

import no.nav.bidrag.domene.enums.vedtak.VedtaksforslagStatus
import no.nav.bidrag.domene.sak.Saksnummer

data class VedtaksforslagHendelse(
    val status: VedtaksforslagStatus,
    val vedtaksid: Int,
    val saksnummerListe: List<Saksnummer>,
    val sporingsdata: Sporingsdata,
)
