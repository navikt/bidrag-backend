package no.nav.bidrag.transport.behandling.behandling

import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.særbidrag.Særbidragskategori
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.organisasjon.dto.SaksbehandlerDto
import java.time.LocalDate
import java.time.LocalDateTime

/** Respons fra bidrag-behandling sitt endepunkt `/api/v2/behandling/detaljer/{behandlingsid}` */
data class BehandlingDetaljerDtoV2(
    val id: Long,
    val saksnummer: String,
    val opprettetAv: SaksbehandlerDto,
    val forholdsmessigFordeling: BehandlingDetaljerForholdsmessigFordelingDto? = null,
    val roller: Set<BehandlingDetaljerRolleDto> = emptySet(),
    val søknadsid: Long? = null,
    val søknadRefId: Long? = null,
    val vedtakstype: Vedtakstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
    val innkrevingstype: Innkrevingstype? = null,
    val erVedtakFattet: Boolean = false,
    val søktFomDato: LocalDate? = null,
    val mottattdato: LocalDate? = null,
    val behandlerenhet: String? = null,
    val kategori: BehandlingDetaljerSærbidragKategoriDto? = null,
    val opprettetTidspunkt: LocalDateTime? = null,
)

data class BehandlingDetaljerSærbidragKategoriDto(
    val kategori: Særbidragskategori,
)

data class BehandlingDetaljerRolleDto(
    val rolletype: Rolletype,
    val ident: String? = null,
    val stønadstype: Stønadstype? = null,
    val saksnummer: String,
    val søknader: List<BehandlingDetaljerRolleSøknadDto> = emptyList(),
)

data class BehandlingDetaljerRolleSøknadDto(
    val søknadsId: Long,
    val søknadFra: SøktAvType,
    val enhet: String,
    val status: Behandlingstatus? = null,
    val behandlingstype: Behandlingstype? = null,
    val behandlingstema: Behandlingstema? = null,
    val omgjørSøknadsid: Long? = null,
    val omgjørVedtaksid: Int? = null,
    val innkreving: Boolean? = null,
    val mottattDato: LocalDate? = null,
    val søknadFomDato: LocalDate? = null,
)

data class BehandlingDetaljerForholdsmessigFordelingDto(
    val opprettetAvSaksbehandler: String? = null,
    val opprettetAvEnhet: String? = null,
    val overførtTilEnhet: String? = null,
)
