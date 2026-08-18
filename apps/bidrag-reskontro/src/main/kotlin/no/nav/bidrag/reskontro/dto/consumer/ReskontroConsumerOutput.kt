package no.nav.bidrag.reskontro.dto.consumer

import java.math.BigDecimal

data class ReskontroConsumerOutput(
    val innParametre: ReskontroConsumerInput,
    val skyldner: Skyldner? = null,
    val bidragssaker: List<Bidragssak>? = null,
    val transaksjoner: List<Transaksjon>? = null,
    val retur: Retur? = null,
    val gjeldendeBetalingsordning: GjeldendeBetalingsordning? = null,
    val nyBetalingsordning: NyBetalingsordning? = null,
    val innkrevingssaksHistorikk: List<Aktivitet>? = emptyList(),
)

data class Aktivitet(
    val beskrivelse: String? = null,
    val fodselsOrgNr: String? = null,
    val navn: String? = null,
    val dato: String? = null,
    val belop: BigDecimal? = null,
)

data class BarnISak(
    val fodselsnummer: String? = null,
    val restGjeldOffentlig: BigDecimal? = null,
    val restGjeldPrivat: BigDecimal? = null,
    val sumIkkeUtbetalt: BigDecimal? = null,
    val sumForskuddUtbetalt: BigDecimal? = null,
    val restGjeldPrivatAndel: BigDecimal? = null,
    val sumUtbetaltAndel: BigDecimal? = null,
    val sumForskuddUtbetaltAndel: BigDecimal? = null,
    val periodeSisteDatoFom: String? = null,
    val periodeSisteDatoTom: String? = null,
    val stoppUtbetaling: String? = null,
)

data class Bidragssak(
    val bidragssaksnummer: Long,
    val bmGjeldFastsettelsesgebyr: BigDecimal,
    val bmGjeldRest: BigDecimal,
    val bpGjeldFastsettelsesgebyr: BigDecimal,
    val perBarnISak: List<BarnISak>? = emptyList(),
)

data class GjeldendeBetalingsordning(
    val typeBetalingsordning: String? = null,
    val kildeOrgnummer: String? = null,
    val kildeNavn: String? = null,
    val datoSisteGiro: String? = null,
    val datoNesteForfall: String? = null,
    val belop: BigDecimal? = null,
    val datoSistEndret: String? = null,
    val aarsakSistEndret: String? = null,
    val sumUbetalt: BigDecimal? = null,
)

data class NyBetalingsordning(
    val datoFraOgMed: String? = null,
    val belop: BigDecimal? = null,
)

data class Retur(
    val kode: Int,
    val beskrivelse: String? = null,
)

data class Skyldner(
    val fodselsOrgnr: String? = null,
    val sumLopendeBidrag: BigDecimal,
    val statusInnkrevingssak: String? = null,
    val fakturamaate: String? = null,
    val sisteAktivitet: String? = null,
    val innbetBelopUfordelt: BigDecimal,
    val gjeldIlagtGebyr: BigDecimal,
)

data class Transaksjon(
    val transaksjonsId: Long,
    val kode: String? = null,
    val beskrivelse: String? = null,
    val dato: String? = null,
    val kildeFodselsOrgNr: String? = null,
    val mottakerFodselsOrgNr: String? = null,
    val opprinneligBeloep: BigDecimal? = null,
    val restBeloep: BigDecimal? = null,
    val valutaOpprinneligBeloep: BigDecimal? = null,
    val valutakode: String? = null,
    val bidragssaksnummer: Long? = null,
    val periodeSisteDatoFom: String? = null,
    val periodeSisteDatoTom: String? = null,
    val barnFodselsnr: String? = null,
    val bidragsId: String? = null,
    val soeknadsType: String? = null,
)
