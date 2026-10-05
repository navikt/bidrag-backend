package no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.behandling.dto.grunnlag.LøpendeBidragGrunnlagForholdsmessigFordeling
import no.nav.bidrag.behandling.dto.v1.behandling.RolleDto
import no.nav.bidrag.behandling.service.forholdsmessigfordeling.SimulertInntektGrunnlag
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import java.time.LocalDate
import java.time.YearMonth

typealias ForholdmessigFordelingDetaljerDto = no.nav.bidrag.transport.behandling.behandling.ForholdmessigFordelingDetaljerDto

typealias ForholdsmessigFordelingBarnDto = no.nav.bidrag.transport.behandling.behandling.ForholdsmessigFordelingBarnDto

typealias ForholdsmessigFordelingPrivateAvtaleDto = no.nav.bidrag.transport.behandling.behandling.ForholdsmessigFordelingPrivateAvtaleDto

typealias ForholdsmessigFordelingÅpenBehandlingDto = no.nav.bidrag.transport.behandling.behandling.ForholdsmessigFordelingÅpenBehandlingDto

data class OpprettFFRequest(
    @JsonFormat(pattern = "dd.MM.yyyy")
    val revurderingFraDato: LocalDate? = null,
    val opprettetAvEnhet: String? = null,
    val detaljerBarn: List<OpprettFFRequestBarnDetaljer> = emptyList(),
) {
    data class OpprettFFRequestBarnDetaljer(
        val manueltOverstyrtRevurderingFraDato: LocalDate? = null,
        val ident: String,
        val stønadstype: Stønadstype,
    )
}

data class SjekkForholdmessigFordelingResponse(
    val skalBehandlesAvEnhet: String,
    val kanOppretteForholdsmessigFordeling: Boolean = false,
    val måOppretteForholdsmessigFordeling: Boolean = false,
    val simulertGrunnlag: List<SimulertInntektGrunnlag> = emptyList(),
    val harSlåttUtTilForholdsmessigFordeling: Boolean = false,
    val eldsteSøktFraDato: LocalDate,
    val barn: List<ForholdsmessigFordelingBarnDto> = emptyList(),
    val løpendeBidragBarn: List<LøpendeBidragGrunnlagForholdsmessigFordeling> = emptyList(),
    val søknaderRevurdering: List<SøknadRevurdering> = emptyList(),
)

data class SøknadRevurdering(
    val søknad: HentSøknad,
    val hovedsøknadsid: Long? = null,
    val erDelAvFF: Boolean,
)

