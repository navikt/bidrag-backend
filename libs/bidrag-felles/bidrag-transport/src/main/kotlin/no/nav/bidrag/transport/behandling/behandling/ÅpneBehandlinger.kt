package no.nav.bidrag.transport.behandling.behandling

import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.dokument.forsendelse.BehandlingType
import java.time.LocalDate

data class HentÅpneBehandlingerRequest(
    val barnIdent: String,
)

data class HentÅpneBehandlingerRespons(
    val behandlinger: List<ÅpenBehandling>,
)

data class ÅpenBehandling(
    val stønadstype: Stønadstype,
    val behandlingId: Long,
    val barn: List<ÅpenBehandlingBarn>,
)

data class ÅpenBehandlingBarn(
    val saksnummer: String,
    val ident: String,
    val bidragsmottakerIdent: String,
    val søknader: List<ÅpenBehandlingBarnSøknad> = emptyList(),
)

data class ÅpenBehandlingBarnSøknad(
    val søknadsid: Long,
    val søktFraDato: LocalDate?,
    val mottattDato: LocalDate,
    val søktAvType: SøktAvType,
    val behandlingstype: Behandlingstype?,
    val behandlingstema: Behandlingstema?,
)
