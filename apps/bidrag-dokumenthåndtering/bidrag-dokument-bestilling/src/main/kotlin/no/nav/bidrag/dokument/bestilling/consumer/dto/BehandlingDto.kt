package no.nav.bidrag.dokument.bestilling.consumer.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.beregning.Resultatkode
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

data class BehandlingDetaljerDtoV2(
    val id: Long,
    val type: TypeBehandling,
    val innkrevingstype: Innkrevingstype = Innkrevingstype.MED_INNKREVING,
    val vedtakstype: Vedtakstype,
    val stønadstype: Stønadstype? = null,
    val engangsbeløptype: Engangsbeløptype? = null,
    val erVedtakFattet: Boolean,
    val erKlageEllerOmgjøring: Boolean,
    val opprettetTidspunkt: LocalDateTime,
    @Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val søktFomDato: LocalDate,
    @Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val mottattdato: LocalDate,
    val søktAv: SøktAvType,
    val saksnummer: String,
    val søknadsid: Long,
    val søknadRefId: Long? = null,
    val vedtakRefId: Long? = null,
    val behandlerenhet: String,
    val roller: Set<RolleDto>,
    @Schema(type = "string", format = "date", example = "01.12.2025")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val virkningstidspunkt: LocalDate? = null,
    @Schema(name = "årsak", enumAsRef = true)
    val årsak: VirkningstidspunktÅrsakstype? = null,
    @Schema(enumAsRef = true)
    val avslag: Resultatkode? = null,
    val kategori: SærbidragKategoriDto? = null,
    val opprettetAv: SaksbehandlerDto,
)

data class SærbidragKategoriDto(
    val kategori: Særbidragskategori,
    val beskrivelse: String? = null,
)

data class RolleDto(
    val rolletype: Rolletype,
    val ident: String? = null,
    val navn: String? = null,
    val fødselsdato: LocalDate? = null,
    val søknader: List<RolleSøknadDto> = emptyList(),
)

data class RolleSøknadDto(
    val søknadsId: Long,
)
