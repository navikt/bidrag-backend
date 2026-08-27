package no.nav.bidrag.transport.behandling.beregning.felles

import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import java.math.BigDecimal
import java.time.LocalDate

data class FeilregistrerSøknadRequest(
    val søknadsid: Long,
)

data class FeilregistrerSøknadsBarnRequest(
    val søknadsid: Long,
    val personidentBarn: String,
)

data class HentBPsÅpneSøknaderRequest(
    val personidentBidragspliktig: String,
)

data class HentBPsÅpneSøknaderResponse(
    val åpneSøknader: List<HentSøknad> = emptyList(),
)

@Deprecated("Bruk heller HentSøknad")
data class ÅpenSøknadDto(
    val søknadsid: Long,
    val behandlingstype: Behandlingstype,
    val behandlerenhet: String? = null,
    val saksnummer: String,
    val behandlingstema: Behandlingstema,
    val innkreving: Boolean,
    val behandlingsid: Long? = null,
    val søknadMottattDato: LocalDate,
    val søknadFomDato: LocalDate? = null,
    val søktAvType: SøktAvType,
    val refSøknadsid: Long? = null,
    val refBehandlingsid: Long? = null,
    val refVedtaksid: Int? = null,
    val partISøknadListe: List<PartISøknad> = emptyList(),
)

data class PartISøknad(
    val personident: String? = null,
    val rolletype: Rolletype,
    val behandlingstatus: Behandlingstatus? = null,
    val innbetaltBeløp: BigDecimal? = null,
    val gebyr: Boolean = false,
    val referanseGebyr: String? = null,
)

data class LeggTilBarnIFFSøknadRequest(
    val søknadsid: Long,
    val personidentBarn: String,
    val innbetaltBeløp: BigDecimal? = null,
)

data class OppdaterBehandlerenhetRequest(
    val søknadsid: Long,
    val behandlerenhet: String,
)

data class OppdaterBehandlingsidRequest(
    val søknadsid: Long,
    val eksisterendeBehandlingsid: Long? = null,
    val nyBehandlingsid: Long,
)

data class OpprettSøknadRequest(
    val saksnummer: String,
    val behandlingstema: Behandlingstema,
    val behandlingstype: Behandlingstype?,
    val behandlingsid: Long? = null,
    val refVedtaksid: Int? = null,
    val refSøknadsid: Long? = null,
    val søktAv: SøktAvType? = null,
    val behandlerenhet: String? = null,
    val søknadFomDato: LocalDate,
    val søknadMottattDato: LocalDate = LocalDate.now(),
    val innkreving: Boolean,
    val hovedsøknadsid: Long? = null,
    val barnListe: List<Barn> = emptyList(),
)

data class Barn(
    val personident: String,
    val innbetaltBeløp: BigDecimal? = null,
)

data class OpprettSøknadResponse(
    val søknadsid: Long,
)

data class HentSøknadRequest(
    val søknadsid: Long,
)

data class HentSøknadResponse(
    val søknad: HentSøknad,
)

data class HentSøknad(
    val søknadsid: Long,
    val søknadMottattDato: LocalDate,
    val søknadFomDato: LocalDate? = null,
    val behandlingstema: Behandlingstema,
    val behandlingstype: Behandlingstype = Behandlingstype.SØKNAD,
    val behandlerenhet: String? = null,
    val saksnummer: String,
    val behandlingsid: Long? = null,
    val refVedtaksid: Int? = null,
    val innkreving: Boolean,
    val søktAvType: SøktAvType,
    val refSøknadsid: Long? = null,
    val vedtaksid: Int? = null,
    val behandlingStatusType: BehandlingStatusType,
    val partISøknadListe: List<PartISøknad> = emptyList(),
)

data class OppdaterReferanseGebyrRequest(
    val søknadsid: Long,
    val personident: String,
    val referanse: String,
)
