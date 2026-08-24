package no.nav.bidrag.dokument.bestilling.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.dokument.bestilling.model.FeilSpråkKoder
import no.nav.bidrag.dokument.bestilling.model.Ident
import no.nav.bidrag.dokument.bestilling.model.Saksbehandler
import no.nav.bidrag.dokument.bestilling.model.SpråkKoder
import no.nav.bidrag.dokument.bestilling.model.erSamhandler
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto

@Schema(description = "Metadata som brukes ved bestilling av ny dokument")
data class DokumentBestillingForespørsel(
    val erBatchBrev: Boolean = false,
    val mottakerId: Ident? = null,
    val mottaker: MottakerTo? = null,
    @Schema(
        description = "Informasjon samhandler hvis mottakerid er en samhandlerid. Påkrevd hvis mottaker er en samhandler",
        deprecated = true,
    )
    val samhandlerInformasjon: SamhandlerInformasjon? = null,
    @Schema(description = "Informasjon om saksbehandler som skal brukes ved opprettelse av dokument")
    val saksbehandler: Saksbehandler? = null,
    val gjelderId: Ident? = null,
    val saksnummer: String,
    val vedtakId: Int? = null,
    val søknadId: Long? = null,
    val behandlingId: Int? = null,
    @Schema(deprecated = true)
    val dokumentReferanse: String? = null,
    @Schema(description = "Dokumentreferanse dokumentet skal bli opprettet med. Det vil ikke bli opprettet ny journalpost hvis dette er satt.")
    val dokumentreferanse: String? = null,
    val tittel: String? = null,
    val enhet: String? = null,
    @Schema(deprecated = true)
    val spraak: String? = null,
    val språk: String? = null,
    val barnIBehandling: List<String> = emptyList(),
) {
    val mottakerIdent get(): String? = mottaker?.ident ?: mottakerId

    fun hentSpråk() = språk ?: spraak

    fun erMottakerSamhandler(): Boolean = (mottaker?.ident?.erSamhandler ?: mottakerIdent?.erSamhandler) == true

    fun harMottakerKontaktinformasjon() = mottaker?.adresse != null

    fun hentRiktigSpråkkode(): String {
        val språk = hentSpråk()
        if (språk.isNullOrEmpty()) {
            return SpråkKoder.BOKMAL
        }
        return when (språk) {
            FeilSpråkKoder.BOKMAL -> SpråkKoder.BOKMAL
            FeilSpråkKoder.NYNORSK -> SpråkKoder.NYNORSK
            FeilSpråkKoder.TYSK -> SpråkKoder.TYSK
            else -> språk
        }
    }
}

data class MottakerTo(
    val ident: Ident? = null,
    val navn: String? = null,
    val språk: String? = null,
    val adresse: MottakerAdresseTo? = null,
)

data class MottakerAdresseTo(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val bruksenhetsnummer: String? = null,
    @Schema(description = "Lankode må være i ISO 3166-1 alpha-2 format") val landkode: String? = null,
    @Schema(description = "Lankode må være i ISO 3166-1 alpha-3 format") val landkode3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
)

data class DokumentBestillingResponse(
    val dokumentId: String,
    val journalpostId: String,
    val arkivSystem: DokumentArkivSystemDto? = null,
)

data class SamhandlerAdresse(
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val landkode: String? = null,
)

data class SamhandlerInformasjon(
    val navn: String? = null,
    val spraak: String? = null,
    val adresse: SamhandlerAdresse? = null,
)
