package no.nav.bidrag.transport.behandling.behandling

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.privatavtale.PrivatAvtaleType
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.enums.vedtak.VirkningstidspunktÅrsakstype
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Antall år som legges til fødselsdato for revurderingsbarn slik at de sorteres etter søknadsbarna */
const val FØDSELSDATO_SORTERING_JUSTERING_ÅR = 100L

/** Respons fra bidrag-behandling sitt endepunkt `/api/v2/behandling/detaljer/{behandlingsid}` */
data class BehandlingDetaljerDtoV2(
    val id: Long,
    val type: TypeBehandling,
    val innkrevingstype: Innkrevingstype = Innkrevingstype.MED_INNKREVING,
    val vedtakstype: Vedtakstype,
    val opprinneligVedtakstype: Vedtakstype? = null,
    val stønadstype: Stønadstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
    val erVedtakFattet: Boolean,
    val erKlageEllerOmgjøring: Boolean,
    val opprettetTidspunkt: LocalDateTime,
    @get:Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val søktFomDato: LocalDate,
    @get:Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattdato: LocalDate,
    val søktAv: SøktAvType,
    val saksnummer: String,
    val søknadsid: Long,
    val søknadRefId: Long? = null,
    val vedtakRefId: Int? = null,
    val behandlerenhet: String,
    val roller: Set<RolleDto>,
    @get:Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val virkningstidspunkt: LocalDate? = null,
    @get:Schema(name = "årsak", enumAsRef = true)
    @get:JsonProperty("årsak")
    val årsak: VirkningstidspunktÅrsakstype? = null,
    @get:Schema(enumAsRef = true)
    val avslag: Resultatkode? = null,
    val kategori: SærbidragKategoriDto? = null,
    val opprettetAv: SaksbehandlerDto,
    val forholdsmessigFordeling: ForholdmessigFordelingDetaljerDto? = null,
)

data class SærbidragKategoriDto(
    val kategori: Særbidragskategori,
    val beskrivelse: String? = null,
)

data class RolleDto(
    val id: Long,
    val rolletype: Rolletype,
    val ident: String? = null,
    val navn: String? = null,
    val fødselsdato: LocalDate? = null,
    val harInnvilgetTilleggsstønad: Boolean? = null,
    val delAvOpprinneligBehandling: Boolean?,
    val erRevurdering: Boolean?,
    val stønadstype: Stønadstype?,
    val saksnummer: String,
    val beregnFraDato: YearMonth? = null,
    val beregnTilDato: YearMonth? = null,
    val bidragsmottaker: String? = null,
    val harLøpendeForskudd: Boolean? = false,
    val harLøpendeBidrag: Boolean? = false,
    val søknader: List<RolleSøknadDto> = emptyList(),
) {
    val fødselsdatoSortering get() = if (erRevurdering == true) fødselsdato?.plusYears(FØDSELSDATO_SORTERING_JUSTERING_ÅR) else fødselsdato

    @get:JsonIgnore
    val identifikator get() = ident + stønadstype
}

data class RolleSøknadDto(
    val søknadsId: Long,
    val søknadFra: SøktAvType,
    val enhet: String,
    val vedtakstype: Vedtakstype,
    val status: Behandlingstatus? = null,
    val behandlingstype: Behandlingstype? = null,
    val behandlingstema: Behandlingstema? = null,
    val omgjørSøknadsid: Long? = null,
    val omgjørVedtaksid: Int? = null,
    val innkreving: Boolean? = null,
    val mottattDato: LocalDate? = null,
    val søknadFomDato: LocalDate? = null,
)

data class ForholdmessigFordelingDetaljerDto(
    val barn: List<ForholdsmessigFordelingBarnDto>,
    val opprettetAvSaksbehandler: String? = null,
    val opprettetAvEnhet: String? = null,
    val overførtTilEnhet: String? = null,
)

data class ForholdsmessigFordelingBarnDto(
    val ident: String,
    val bidragsmottaker: RolleDto?,
    val navn: String,
    val fødselsdato: LocalDate?,
    val saksnr: String?,
    val enhet: String,
    val erRevurdering: Boolean,
    val harOpprettetForholdsmessigFordeling: Boolean,
    val stønadstype: Stønadstype?,
    val eldsteSøktFraDato: LocalDate?,
    val harLøpendeBidrag: Boolean,
    val innkrevesFraDato: YearMonth?,
    val opphørsdato: YearMonth?,
    val sammeSakSomBehandling: Boolean,
    @get:Schema(name = "åpneBehandlinger")
    val åpneBehandlinger: List<ForholdsmessigFordelingÅpenBehandlingDto> = emptyList(),
    val privateAvtale: ForholdsmessigFordelingPrivateAvtaleDto? = null,
)

data class ForholdsmessigFordelingPrivateAvtaleDto(
    val avtaleDato: LocalDate? = null,
    val utenlandsk: Boolean = false,
    val avtaleType: PrivatAvtaleType? = null,
    val stønadstype: Stønadstype? = null,
)

data class ForholdsmessigFordelingÅpenBehandlingDto(
    val søktFraDato: LocalDate?,
    val mottattDato: LocalDate?,
    val stønadstype: Stønadstype,
    val behandlingstema: Behandlingstema?,
    val behandlingstype: Behandlingstype? = null,
    val søktAvType: SøktAvType,
    val medInnkreving: Boolean,
    val behandlerEnhet: String,
    val behandlingId: Long?,
    val søknadsid: Long?,
)
