package no.nav.bidrag.transport.behandling.beregning.felles

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

data class HentBeregningForSøknaderRequest(
    @field:Valid
    @field:NotEmpty(message = "Søknadlisten kan ikke være tom")
    val søknadListe: List<HentBeregningForSøknad>,
)

@Schema(description = "Hent beregning for søknad. Mottatt dato på søknaden brukes som datoSøknad i beregningen.")
data class HentBeregningForSøknad(
    val søknadsid: Long,
    @field:NotBlank(message = "Saksnummer kan ikke være blank")
    @field:Size(max = 7, min = 7, message = "Saksnummer må bestå av 7 tegn")
    val saksnummer: String,
    @field:GyldigPersonidentLengde
    val personidentBarn: Personident,
    val stønadstype: Stønadstype,
)

data class HentBeregningForSøknaderRespons(
    val søknadListe: List<BeregningForSøknad>,
)

data class BeregningForSøknad(
    val søknadsid: Long,
    @Schema(description = "Mottatt dato på søknaden. Null hvis søknaden ikke finnes")
    val søknadMottattDato: LocalDate?,
    @Schema(description = "Om siste vedtakshendelse for søknaden er registrert fra BBM (FRA_BBM = 1)")
    val erVedtakMedBeregning: Boolean,
    @Schema(description = "Om søknaden og en vedtakshendelse for søknaden ble funnet")
    val hendelseFunnet: Boolean,
    @Schema(description = "Beregninger for søknaden. Hentes med søknadens mottatt dato som datoSøknad. Tom hvis søknaden ikke finnes")
    val beregningListe: List<BidragBeregningResponsDto.BidragBeregning>,
)
