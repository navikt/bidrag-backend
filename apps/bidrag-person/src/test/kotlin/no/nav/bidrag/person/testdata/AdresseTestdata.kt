package no.nav.bidrag.person.testdata

import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.person.query.Bostedsadresse
import no.nav.bidrag.person.query.HentPersonAdresse
import no.nav.bidrag.person.query.Kilde
import no.nav.bidrag.person.query.Kontaktadresse
import no.nav.bidrag.person.query.KontaktadresseType
import no.nav.bidrag.person.query.MatrikkelAdresse
import no.nav.bidrag.person.query.Metadata
import no.nav.bidrag.person.query.Oppholdsadresse
import no.nav.bidrag.person.query.PostadresseIFrittFormat
import no.nav.bidrag.person.query.Postboksadresse
import no.nav.bidrag.person.query.UtenlandskAdresseIFrittFormat
import no.nav.bidrag.person.query.Vegadresse
import java.time.LocalDateTime

val POSTNUMMER = "3015"
val KOMMUNENUMMER = "3005"
val POSTSTED = "DRAMMEN"
val BRUKSENHETSNUMMER = "H0201"
const val MATRIKKEL_TILLEGGSNAVN = "Storgården"
val ADRESSENAVN = "Cappelens gate"
val HUSNUMMER = "41"
val HUSBOKSTAV = "A"
val LANDKODE_SVERIGE_ALPHA3 = Landkode3("SWE")
const val POSTBOKS = "Postboks 1234"
const val POSTBOKS_EIER = "Byggfirma A/S"

fun createBostedsadresse(kilde: Kilde = Kilde.PDL, gyldigFraOgMed: LocalDateTime): Bostedsadresse = Bostedsadresse(
    metadata = Metadata(kilde.name),
    angittFlyttedato = date1,
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = null,
    vegadresse = createVegadresse(),
    utenlandskAdresse = null,
    matrikkeladresse = null,

    ukjentBosted = null,
    coAdressenavn = null,
)

fun createKontaktadresseInnland(kilde: Kilde = Kilde.PDL, gyldigFraOgMed: LocalDateTime): Kontaktadresse = Kontaktadresse(
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = null,
    type = KontaktadresseType.Innland,
    coAdressenavn = null,
    postboksadresse = null,
    vegadresse = createVegadresse("Kontaktadresseveien"),
    postadresseIFrittFormat = null,
    utenlandskAdresse = null,
    utenlandskAdresseIFrittFormat = null,
    metadata = Metadata(kilde.name),
)

fun createOppholdsadresse(kilde: Kilde = Kilde.PDL, gyldigFraOgMed: LocalDateTime): Oppholdsadresse = Oppholdsadresse(
    gyldigFraOgMed = gyldigFraOgMed,
    gyldigTilOgMed = null,
    coAdressenavn = null,
    utenlandskAdresse = null,
    vegadresse = createVegadresse("Oppholdsadresseveien"),
    matrikkeladresse = null,
    metadata = Metadata(kilde.name),
    oppholdAnnetSted = null,
)

fun createVegadresse(adresseNavn: String = ADRESSENAVN): Vegadresse = Vegadresse(
    husnummer = HUSNUMMER,
    husbokstav = HUSBOKSTAV,
    adressenavn = adresseNavn,
    tilleggsnavn = null,
    postnummer = POSTNUMMER,
    bruksenhetsnummer = BRUKSENHETSNUMMER,
    bydelsnummer = null,
    kommunenummer = KOMMUNENUMMER,
    matrikkelId = 12345,
)

fun createMatrikkelAdresse(): MatrikkelAdresse = MatrikkelAdresse(
    matrikkelId = 1L,
    bruksenhetsnummer = BRUKSENHETSNUMMER,
    tilleggsnavn = MATRIKKEL_TILLEGGSNAVN,
    postnummer = POSTNUMMER,
    kommunenummer = KOMMUNENUMMER,
)

fun createPostboksAdresse(withPostboksEier: Boolean? = true): Postboksadresse = Postboksadresse(
    postnummer = POSTNUMMER,
    postboks = POSTBOKS,
    postbokseier = if (withPostboksEier == true) POSTBOKS_EIER else null,
)

fun createPostadresseIFrittFormat(adresselinje1: String = "C/O Kari Hansen"): PostadresseIFrittFormat = PostadresseIFrittFormat(
    adresselinje1 = adresselinje1,
    adresselinje2 = "Kirkegata 15 A",
    adresselinje3 = "Gate 3",
    postnummer = POSTNUMMER,
)

fun createUtenlandskAdresseIFrittFormat(
    adresselinje1: String? = "Gate 1",
    adresselinje2: String? = "Gate 2",
    adresselinje3: String? = "Gate 3",
): UtenlandskAdresseIFrittFormat = UtenlandskAdresseIFrittFormat(
    adresselinje1 = adresselinje1,
    adresselinje2 = adresselinje2,
    adresselinje3 = adresselinje3,
    postkode = "BA",
    byEllerStedsnavn = "Karavella",
    landkode = LANDKODE_SVERIGE_ALPHA3,
)

fun createHentPersonAdresse(
    bostedsadresse: Bostedsadresse,
    kontaktadresse: Kontaktadresse,
    oppholdsadresse: Oppholdsadresse = createOppholdsadresse(Kilde.FREG, dateTime1),
): HentPersonAdresse = HentPersonAdresse(
    bostedsadresse = listOf(bostedsadresse),
    kontaktadresse = listOf(kontaktadresse),
    oppholdsadresse = listOf(oppholdsadresse),
)
