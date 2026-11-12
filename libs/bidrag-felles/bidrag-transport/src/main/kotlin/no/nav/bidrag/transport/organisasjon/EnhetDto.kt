package no.nav.bidrag.transport.organisasjon

import no.nav.bidrag.domene.enums.diverse.Enhetsstatus
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.sak.Arbeidsfordeling
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.vedtak.Behandlingstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer

data class EnhetBrukerDto(
    val navIdent: String,
    val navn: String,
)

data class EnhetDto(
    val nummer: Enhetsnummer,
    val navn: String? = null,
    @Deprecated("bruk nummer", ReplaceWith("nummer"))
    val enhetIdent: Enhetsnummer = nummer,
    @Deprecated("bruk navn", ReplaceWith("navn"))
    val enhetNavn: String? = navn,
    val status: Enhetsstatus = Enhetsstatus.AKTIV,
)

/**
 * Forespørsel om enheten arbeidsfordelingen i Norg gir for en sak.
 * [hovedident] gir geografisk tilknytning. [parter] er alle nåværende og tidligere parter i saken, også hovedidenten.
 * [arbeidsfordeling] INH gir settekontoret til enheten, med mindre en av partene er skjermet eller har adressebeskyttelse.
 */
data class ArbeidsfordelingSakRequest(
    val hovedident: Personident,
    val parter: Set<Personident>,
    val arbeidsfordeling: Arbeidsfordeling,
    val sakskategori: Sakskategori,
    val behandlingstype: Behandlingstype,
)

/**
 * Enheten arbeidsfordelingen i Norg gir for en sak.
 * [skjermet] er sann når minst én av partene er skjermet som egen ansatt.
 */
data class ArbeidsfordelingSakResponse(
    val enhet: EnhetDto,
    val skjermet: Boolean,
)

data class EnhetKontaktinfoDto(
    val nummer: Enhetsnummer,
    val navn: String? = null,
    @Deprecated("bruk nummer", ReplaceWith("nummer"))
    val enhetIdent: Enhetsnummer = nummer,
    @Deprecated("bruk navn", ReplaceWith("navn"))
    val enhetNavn: String? = navn,
    val telefonnummer: String? = null,
    val postadresse: EnhetPostadresseDto? = null,
) {
    companion object {
        fun medStandardadresse(enhetsnummer: Enhetsnummer) = EnhetKontaktinfoDto(
            nummer = enhetsnummer,
            navn = "Nav Familie- og pensjonsytelser",
            telefonnummer = "55553333",
            postadresse =
            EnhetPostadresseDto(
                postnummer = "0603",
                adresselinje1 = "Postboks 6215 Etterstad",
                poststed = "Oslo",
                land = "Norway",
                kommunenr = "0301",
            ),
        )
    }
}

data class EnhetPostadresseDto(
    val navn: String? = null,
    val postnummer: String? = null,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val poststed: String? = null,
    val land: String? = null,
    val kommunenr: String? = null,
)

data class EnhetDetaljerDto(
    val enhetId: String,
    val navn: String?,
    val sakskategori: Sakskategori = Sakskategori.NASJONAL,
    val settekontorEnhetId: String?,
    val telefonnummer: String? = null,
    val postadresse: Map<Språk, EnhetPostadresseDto> = emptyMap(),
)

data class EnheterGruppeDto(
    val gruppeNavn: String,
    val enheter: List<EnhetDetaljerDto>,
)

data class BidragEnheterResponsDto(
    val grupper: List<EnheterGruppeDto>,
)
