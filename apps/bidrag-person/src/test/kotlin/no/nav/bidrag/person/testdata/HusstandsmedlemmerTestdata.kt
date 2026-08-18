package no.nav.bidrag.person.testdata

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.query.Bostedsadresse
import no.nav.bidrag.person.query.Folkeregisteridentifikator
import no.nav.bidrag.person.query.FolkreregisterIdent
import no.nav.bidrag.person.query.Fødselsdato
import no.nav.bidrag.person.query.HentHusstandsmedlemmer
import no.nav.bidrag.person.query.IdentStatus
import no.nav.bidrag.person.query.Kilde
import no.nav.bidrag.person.query.Metadata
import no.nav.bidrag.person.query.Navn
import no.nav.bidrag.person.query.PersonResponse.HentPersonNavnFødselsdatoDødsfallResponse
import no.nav.bidrag.person.query.Vegadresse
import no.nav.bidrag.transport.person.Husstandsmedlem
import java.time.LocalDate
import java.time.LocalDateTime

fun lagBostedsadresse(): Bostedsadresse = Bostedsadresse(
    metadata = Metadata(Kilde.PDL.name),
    angittFlyttedato = LocalDate.parse("2020-01-01"),
    gyldigFraOgMed = LocalDateTime.parse("2020-01-01T00:00:01"),
    gyldigTilOgMed = null,
    vegadresse = lagVegadresse(),
    utenlandskAdresse = null,
    matrikkeladresse = null,
    ukjentBosted = null,
    coAdressenavn = null,
)

fun lagVegadresse(): Vegadresse = Vegadresse(
    husnummer = "42",
    husbokstav = "B",
    adressenavn = "Cappelens gate",
    tilleggsnavn = null,
    postnummer = "3015",
    bruksenhetsnummer = null,
    bydelsnummer = null,
    kommunenummer = "3005",
    matrikkelId = null,
)

fun lagVegadresseAnnenHusstand(): Vegadresse = Vegadresse(
    husnummer = "17",
    husbokstav = "X",
    adressenavn = "Ingens gate",
    tilleggsnavn = null,
    postnummer = "0123",
    bruksenhetsnummer = null,
    bydelsnummer = null,
    kommunenummer = "1701",
    matrikkelId = null,
)

fun lagHusstandsmedlemBolig1(): List<Husstandsmedlem> = listOf(
    Husstandsmedlem(
        gyldigFraOgMed = LocalDate.parse("2018-10-01"),
        gyldigTilOgMed = LocalDate.parse("2019-11-30"),
        personId = Personident("234"),
        navn = "Tykk Smalgang",
        fødselsdato = LocalDate.parse("2010-10-01"),
        dødsdato = null,
    ),
)

fun lagHusstandsmedlemBolig2(): List<Husstandsmedlem> = listOf(
    Husstandsmedlem(
        gyldigFraOgMed = LocalDate.parse("2023-09-04"),
        gyldigTilOgMed = LocalDate.parse("2023-11-28"),
        personId = Personident("234"),
        navn = "Tykk Smalgang",
        fødselsdato = LocalDate.parse("2010-10-01"),
        dødsdato = null,
    ),
)

fun lagHusstandsmedlemBolig3(): List<Husstandsmedlem> = listOf(
    Husstandsmedlem(
        gyldigFraOgMed = LocalDate.parse("2023-11-29"),
        gyldigTilOgMed = null,
        personId = Personident("234"),
        navn = "Tykk Smalgang",
        fødselsdato = LocalDate.parse("2010-10-01"),
        dødsdato = null,
    ),
)

fun lagHusstandsmedlemMedDødsdatoBolig1(): List<Husstandsmedlem> = listOf(
    Husstandsmedlem(
        gyldigFraOgMed = LocalDate.parse("2023-09-04"),
        gyldigTilOgMed = LocalDate.parse("2023-11-28"),
        personId = Personident("234"),
        navn = "Tykk Smalgang",
        fødselsdato = LocalDate.parse("2010-10-01"),
        dødsdato = LocalDate.parse("2024-04-24"),
    ),
)

fun lagHusstandsmedlemMedDødsdatoBolig2(): List<Husstandsmedlem> = listOf(
    Husstandsmedlem(
        gyldigFraOgMed = LocalDate.parse("2023-11-29"),
        gyldigTilOgMed = null,
        personId = Personident("234"),
        navn = "Tykk Smalgang",
        fødselsdato = LocalDate.parse("2010-10-01"),
        dødsdato = LocalDate.parse("2024-04-24"),
    ),
)

fun lagHentPersonBostedsadresse(
    angittFlyttedato: LocalDate?,
    gyldigFraOgMed: LocalDateTime?,
    gyldigTilOgMed: LocalDateTime?,
    adresseNavn: String,
): Bostedsadresse = Bostedsadresse(
    metadata = Metadata(Kilde.FREG.name),
    angittFlyttedato = angittFlyttedato,
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = gyldigTilOgMed,
    vegadresse = lagVegadresse(adresseNavn),
    utenlandskAdresse = null,
    matrikkeladresse = null,
    ukjentBosted = null,
    coAdressenavn = null,
)

fun lagVegadresse(adresseNavn: String, husnummer: String = "17", postnummer: String = "7066"): Vegadresse = Vegadresse(
    husnummer = husnummer,
    husbokstav = null,
    adressenavn = adresseNavn,
    tilleggsnavn = null,
    postnummer = postnummer,
    bruksenhetsnummer = null,
    bydelsnummer = null,
    kommunenummer = null,
    matrikkelId = null,
)

fun lagHentHusstandsmedlemmerListe(): List<HentHusstandsmedlemmer> = listOf(
    HentHusstandsmedlemmer(
        person =
        HentPersonNavnFødselsdatoDødsfallResponse(
            navn =
            listOf(
                Navn(
                    fornavn = "Fet",
                    mellomnavn = null,
                    etternavn = "Lettbrus",
                ),
            ),
            folkeregisteridentifikator =
            listOf(
                Folkeregisteridentifikator(
                    identifikasjonsnummer = Personident("12345678901"),
                    status = IdentStatus.I_BRUK,
                    type = FolkreregisterIdent.FNR,
                ),
            ),
            bostedsadresse =
            listOf(
                Bostedsadresse(
                    metadata = Metadata(Kilde.PDL.name),
                    angittFlyttedato = LocalDate.parse("1999-04-01"),
                    gyldigFraOgMed = LocalDateTime.parse("1999-04-01T00:00:01"),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresseAnnenHusstand(),
                    utenlandskAdresse = null,
                    matrikkeladresse = null,
                    ukjentBosted = null,
                    coAdressenavn = null,
                ),
                Bostedsadresse(
                    metadata = Metadata(Kilde.PDL.name),
                    angittFlyttedato = LocalDate.parse("2021-12-01"),
                    gyldigFraOgMed = LocalDateTime.parse("2021-12-01T00:00:01"),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(),
                    utenlandskAdresse = null,
                    matrikkeladresse = null,
                    ukjentBosted = null,
                    coAdressenavn = null,
                ),
            ),
            foedselsdato =
            listOf(
                Fødselsdato(
                    foedselsdato = LocalDate.parse("1990-01-01"),
                    foedselsaar = 1990,
                ),
            ),
            doedsfall = listOf(),
        ),
    ),
)
