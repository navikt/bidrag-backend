package no.nav.bidrag.elin.stub.skatt.dto.reskontro

data class Output(
    val innParametre: Input,
    val skyldner: Skyldner? = null,
    val bidragssak: Bidragssak? = null,
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
    val belop: Double? = null,
)

data class BarnISak(
    val fodselsnummer: String? = null,
    val restGjeldOffentlig: Double? = null,
    val restGjeldPrivat: Double? = null,
    val sumIkkeUtbetalt: Double? = null,
    val sumForskuddUtbetalt: Double? = null,
    val restGjeldPrivatAndel: Double? = null,
    val sumInnbetaltAndel: Double? = null,
    val sumForskuddUtbetaltAndel: Double? = null,
    val periodeSisteDatoFom: String? = null,
    val periodeSisteDatoTom: String? = null,
    val stoppUtbetaling: String? = null,
)

data class Bidragssak(
    val bidragssaksnummer: Long,
    val bmGjeldFastsettelsesgebyr: Double,
    val bmGjeldRest: Double,
    val bpGjeldFastsettelsesgebyr: Double,
    val perBarnISak: List<BarnISak>? = emptyList(),
)

data class GjeldendeBetalingsordning(
    val typeBetalingsordning: String? = null,
    val kildeOrgnummer: String? = null,
    val kildeNavn: String? = null,
    val datoSisteGiro: String? = null,
    val datoNesteForfall: String? = null,
    val belop: Double? = null,
    val datoSistEndret: String? = null,
    val aarsakSistEndret: String? = null,
    val sumUbetalt: Double? = null,
)

data class NyBetalingsordning(
    val datoFraOgMed: String? = null,
    val belop: Double? = null,
)

data class Retur(
    val kode: Int,
    val beskrivelse: String? = null,
)

data class Skyldner(
    val fodselsOrgnr: String? = null,
    val sumLopendeBidrag: Double,
    val statusInnkrevingssak: String? = null,
    val fakturamaate: String? = null,
    val sisteAktivitet: String? = null,
    val innbetBelopUfordelt: Double,
    val gjeldIlagtGebyr: Double,
)

data class Transaksjon(
    val transaksjonsId: Long,
    val kode: String? = null,
    val beskrivelse: String? = null,
    val dato: String? = null,
    val kildeFodselsOrgNr: String? = null,
    val mottakerFodslesOrgNr: String? = null,
    val opprinneligBeloep: Double? = null,
    val restBeloep: Double? = null,
    val valutaOpprinneligBeloep: Double? = null,
    val valutakode: String? = null,
    val bidragssaksnummer: Long,
    val periodeSisteDatoFom: String? = null,
    val periodeSisteDatoTom: String? = null,
    val barnFodselsnr: String? = null,
    val bidragsId: String? = null,
    val soeknadsType: String? = null,
)
