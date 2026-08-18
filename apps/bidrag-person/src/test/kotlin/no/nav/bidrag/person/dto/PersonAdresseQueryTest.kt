package no.nav.bidrag.person.dto

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.unmockkConstructor
import no.nav.bidrag.commons.service.KodeverkProvider
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.person.query.Bostedsadresse
import no.nav.bidrag.person.query.HentPersonAdresse
import no.nav.bidrag.person.query.Kilde
import no.nav.bidrag.person.query.Kontaktadresse
import no.nav.bidrag.person.query.KontaktadresseType
import no.nav.bidrag.person.query.Metadata
import no.nav.bidrag.person.query.Oppholdsadresse
import no.nav.bidrag.person.query.PersonAdresseResponse
import no.nav.bidrag.person.testdata.ADRESSENAVN
import no.nav.bidrag.person.testdata.BRUKSENHETSNUMMER
import no.nav.bidrag.person.testdata.HUSBOKSTAV
import no.nav.bidrag.person.testdata.HUSNUMMER
import no.nav.bidrag.person.testdata.MATRIKKEL_TILLEGGSNAVN
import no.nav.bidrag.person.testdata.POSTBOKS
import no.nav.bidrag.person.testdata.POSTBOKS_EIER
import no.nav.bidrag.person.testdata.POSTNUMMER
import no.nav.bidrag.person.testdata.POSTSTED
import no.nav.bidrag.person.testdata.createMatrikkelAdresse
import no.nav.bidrag.person.testdata.createPostadresseIFrittFormat
import no.nav.bidrag.person.testdata.createPostboksAdresse
import no.nav.bidrag.person.testdata.createUtenlandskAdresseIFrittFormat
import no.nav.bidrag.person.testdata.createVegadresse
import no.nav.bidrag.person.testdata.date1
import no.nav.bidrag.person.testdata.date3
import no.nav.bidrag.person.testdata.dateTime1
import no.nav.bidrag.person.testdata.dateTime2
import no.nav.bidrag.person.testdata.dateTime3
import no.nav.bidrag.person.testdata.mockKodeverkResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.boot.restclient.RestTemplateBuilder

class PersonAdresseQueryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun initMock() {
            mockKodeverkResponse()
        }

        @JvmStatic
        @AfterAll
        fun resetMocks() {
            unmockkConstructor(RestTemplateBuilder::class)
            clearAllMocks()
            KodeverkProvider.invaliderKodeverkCache()
        }
    }

    @Test
    fun shouldMapKontaktadresseWithMasterPDL() {
        val adressePDL = createPostadresseIFrittFormat("Adresse PDL")
        val adresseFREG = createPostadresseIFrittFormat("Adresse FREG")

        Metadata()
        val personAdresse =
            HentPersonAdresse(
                kontaktadresse =
                listOf(
                    Kontaktadresse(
                        metadata = Metadata(master = Kilde.PDL.name),
                        gyldigFraOgMed = dateTime1,
                        type = KontaktadresseType.Innland,
                        postadresseIFrittFormat = adressePDL,
                    ),
                    Kontaktadresse(
                        metadata = Metadata(Kilde.FREG.name),
                        gyldigFraOgMed = dateTime2,
                        type = KontaktadresseType.Innland,
                        postadresseIFrittFormat = adresseFREG,
                    ),
                ),
                oppholdsadresse = createOppholdsadresseList(),
                bostedsadresse = createBoststedsadresseList(),
            )

        val response = PersonAdresseResponse(personAdresse)
        val mappedValue = response.hentPostadresse()

        mappedValue?.adresselinje1 shouldBe adressePDL.adresselinje1!!
    }

    @Test
    fun shouldMapNewestKontaktadresseWithMasterFREG() {
        val adresseFregOld = createPostadresseIFrittFormat("Adresse FREG OLD")
        val adresseFregNew = createPostadresseIFrittFormat("Adresse FREG NEW")
        val personAdresse =
            HentPersonAdresse(
                kontaktadresse =
                listOf(
                    Kontaktadresse(
                        metadata = Metadata(Kilde.FREG.name),
                        gyldigFraOgMed = dateTime1,
                        type = KontaktadresseType.Innland,
                        postadresseIFrittFormat = adresseFregOld,
                    ),
                    Kontaktadresse(
                        metadata = Metadata(Kilde.FREG.name),
                        gyldigFraOgMed = dateTime2,
                        type = KontaktadresseType.Innland,
                        postadresseIFrittFormat = adresseFregNew,
                    ),
                ),
                oppholdsadresse = createOppholdsadresseList(),
                bostedsadresse = createBoststedsadresseList(),
            )

        val response = PersonAdresseResponse(personAdresse)
        val mappedValue = response.hentPostadresse()
        mappedValue?.adresselinje1 shouldBe adresseFregNew.adresselinje1
    }

    @Test
    fun shouldMapOppholdsadresseWithMasterPDL() {
        val adressePDL = createVegadresse("Adresse PDL")
        val adresseFREG = createVegadresse("Adresse FREG")
        val personAdresse =
            HentPersonAdresse(
                oppholdsadresse =
                listOf(
                    Oppholdsadresse(
                        metadata = Metadata(Kilde.PDL.name),
                        gyldigFraOgMed = dateTime1,
                        vegadresse = adressePDL,
                    ),
                    Oppholdsadresse(
                        metadata = Metadata(Kilde.FREG.name),
                        gyldigFraOgMed = dateTime2,
                        vegadresse = adresseFREG,
                    ),
                ),
                bostedsadresse = createBoststedsadresseList(),
            )

        val response = PersonAdresseResponse(personAdresse)

        val mappedValue = response.hentPostadresse()
        mappedValue?.adresselinje1 shouldBe "${adressePDL.adressenavn} ${adressePDL.husnummer}${adressePDL.husbokstav}"
    }

    @Test
    fun shouldMapOppholdsadresseWithMasterFREG() {
        val adresseFREG = createVegadresse("Adresse FREG")
        val personAdresse =
            HentPersonAdresse(
                oppholdsadresse =
                listOf(
                    Oppholdsadresse(
                        metadata = Metadata(Kilde.FREG.name),
                        gyldigFraOgMed = dateTime2,
                        vegadresse = adresseFREG,
                    ),
                ),
                bostedsadresse = createBoststedsadresseList(),
            )

        val response = PersonAdresseResponse(personAdresse)
        val mappedValue = response.hentPostadresse()
        mappedValue?.adresselinje1 shouldBe "${adresseFREG.adressenavn} ${adresseFREG.husnummer}${adresseFREG.husbokstav}"
    }

    @Test
    fun shouldMapBoststedsadresse() {
        val adressePDL = createVegadresse("Adresse Bosted")
        val personAdresse =
            HentPersonAdresse(
                bostedsadresse =
                listOf(
                    Bostedsadresse(
                        metadata = Metadata(Kilde.PDL.name),
                        angittFlyttedato = date1,
                        vegadresse = adressePDL,
                    ),
                ),
            )

        val response = PersonAdresseResponse(personAdresse)
        val mappedValue = response.hentPostadresse()
        mappedValue?.adresselinje1 shouldBe "${adressePDL.adressenavn} ${adressePDL.husnummer}${adressePDL.husbokstav}"
    }

    @Test
    fun shouldMapBoststedsadresseWhenNewest() {
        val adresseBosted = createVegadresse("Adresse Bosted")
        val personAdresse =
            HentPersonAdresse(
                kontaktadresse = createKontaktadresseList(),
                oppholdsadresse = createOppholdsadresseList(),
                bostedsadresse =
                listOf(
                    Bostedsadresse(
                        metadata = Metadata(Kilde.PDL.name),
                        angittFlyttedato = date3,
                        vegadresse = adresseBosted,
                    ),
                ),
            )

        val response = PersonAdresseResponse(personAdresse)
        val mappedValue = response.hentPostadresse()
        mappedValue?.adresselinje1 shouldBe "${adresseBosted.adressenavn} ${adresseBosted.husnummer}${adresseBosted.husbokstav}"
    }

    @Nested
    inner class VegadresseMapper {
        @Test
        fun shouldMapVegadresse() {
            val adresseBosted = createVegadresse()
            val personAdresse =
                HentPersonAdresse(
                    bostedsadresse =
                    listOf(
                        Bostedsadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            angittFlyttedato = date3,
                            vegadresse = adresseBosted,
                            coAdressenavn = "",
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            mappedValue?.adresselinje1 shouldBe "$ADRESSENAVN $HUSNUMMER$HUSBOKSTAV"
            mappedValue?.adresselinje2 shouldBe null
            mappedValue?.adresselinje3 shouldBe null
            mappedValue?.bruksenhetsnummer shouldBe BRUKSENHETSNUMMER
            mappedValue?.postnummer shouldBe POSTNUMMER
            mappedValue?.poststed shouldBe POSTSTED
            mappedValue?.land shouldBe Landkode2("NO")
        }

        @Test
        fun `should map vegadresse with CO`() {
            val coAdressenavn = "Hans Hansengata"
            val adresseBosted = createVegadresse()
            val personAdresse =
                HentPersonAdresse(
                    bostedsadresse =
                    listOf(
                        Bostedsadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            angittFlyttedato = date3,
                            vegadresse = adresseBosted,
                            coAdressenavn = coAdressenavn,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            mappedValue?.adresselinje1 shouldBe "c/o $coAdressenavn"
            mappedValue?.adresselinje2 shouldBe "$ADRESSENAVN $HUSNUMMER$HUSBOKSTAV"
            mappedValue?.adresselinje3 shouldBe null
            mappedValue?.bruksenhetsnummer shouldBe BRUKSENHETSNUMMER
            mappedValue?.postnummer shouldBe POSTNUMMER
            mappedValue?.poststed shouldBe POSTSTED
            mappedValue?.land shouldBe Landkode2("NO")
        }
    }

    @Nested
    inner class MatrikkeladresseMapper {
        @Test
        fun shouldMapMatrikkelAdresse() {
            val adresse = createMatrikkelAdresse()
            val personAdresse =
                HentPersonAdresse(
                    bostedsadresse =
                    listOf(
                        Bostedsadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            matrikkeladresse = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            mappedValue?.adresselinje1 shouldBe MATRIKKEL_TILLEGGSNAVN
            mappedValue?.adresselinje2 shouldBe null
            mappedValue?.adresselinje3 shouldBe null
            mappedValue?.postnummer shouldBe POSTNUMMER
            mappedValue?.poststed shouldBe POSTSTED
            mappedValue?.land shouldBe Landkode2("NO")
        }

        @Test
        fun `should map matrikkeladresse with CO`() {
            val coAdressenavn = "Hans Hansengata"
            val adresse = createMatrikkelAdresse()
            val personAdresse =
                HentPersonAdresse(
                    bostedsadresse =
                    listOf(
                        Bostedsadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            matrikkeladresse = adresse,
                            coAdressenavn = coAdressenavn,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            mappedValue?.adresselinje1 shouldBe coAdressenavn
            mappedValue?.adresselinje2 shouldBe MATRIKKEL_TILLEGGSNAVN
            mappedValue?.adresselinje3 shouldBe null
            mappedValue?.postnummer shouldBe POSTNUMMER
            mappedValue?.poststed shouldBe POSTSTED
            mappedValue?.land shouldBe Landkode2("NO")
        }
    }

    @Nested
    inner class PostboksAdresseMapper {
        @Test
        fun shouldMapPostboksAdresse() {
            val adresse = createPostboksAdresse()
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            postboksadresse = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            mappedValue?.adresselinje1 shouldBe POSTBOKS_EIER
            mappedValue?.adresselinje2 shouldBe "Postboks $POSTBOKS"
            mappedValue?.adresselinje3 shouldBe null
            mappedValue?.postnummer shouldBe POSTNUMMER
            mappedValue?.poststed shouldBe POSTSTED
            mappedValue?.land shouldBe Landkode2("NO")
        }

        @Test
        fun shouldMapPostboksAdresseWithoutPostbokseier() {
            val adresse = createPostboksAdresse(false)
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            postboksadresse = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            mappedValue?.adresselinje1 shouldBe "Postboks $POSTBOKS"
            mappedValue?.adresselinje2 shouldBe null
            mappedValue?.adresselinje3 shouldBe null
            mappedValue?.postnummer shouldBe POSTNUMMER
            mappedValue?.poststed shouldBe POSTSTED
            mappedValue?.land shouldBe Landkode2("NO")
        }
    }

    @Nested
    inner class FrittFormatAdresseMapper {
        @Test
        fun shouldMapNorskFrittFormatAdresse() {
            val adresse = createPostadresseIFrittFormat()
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            postadresseIFrittFormat = adresse,
                        ),
                    ),
                )
            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            assertAll(
                "adresse",
                { mappedValue?.adresselinje1 shouldBe "C/O Kari Hansen" },
                { mappedValue?.adresselinje2 shouldBe "Kirkegata 15 A" },
                { mappedValue?.adresselinje3 shouldBe "Gate 3" },
                { mappedValue?.postnummer shouldBe POSTNUMMER },
                { mappedValue?.poststed shouldBe POSTSTED },
                { mappedValue?.land shouldBe Landkode2("NO") },
            )
        }

        @Test
        fun shouldMapUtenlandskFrittFormatAdresse() {
            val adresse = createUtenlandskAdresseIFrittFormat()
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            utenlandskAdresseIFrittFormat = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            assertAll(
                "adresse",
                { mappedValue?.adresselinje1 shouldBe "Gate 1" },
                { mappedValue?.adresselinje2 shouldBe "Gate 2" },
                { mappedValue?.adresselinje3 shouldBe "Gate 3" },
                { mappedValue?.postnummer shouldBe null },
                { mappedValue?.poststed shouldBe "Karavella" },
                { mappedValue?.land shouldBe Landkode2("SE") },
            )
        }

        @Test
        fun shouldMapUtenlandskFrittFormatAdresseMedManglendeAdresselinje1() {
            val adresse = createUtenlandskAdresseIFrittFormat(adresselinje1 = null)
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            utenlandskAdresseIFrittFormat = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            assertAll(
                "adresse",
                { mappedValue?.adresselinje1 shouldBe "Gate 2" },
                { mappedValue?.adresselinje2 shouldBe "Gate 3" },
                { mappedValue?.adresselinje3 shouldBe null },
                { mappedValue?.postnummer shouldBe null },
                { mappedValue?.poststed shouldBe "Karavella" },
                { mappedValue?.land shouldBe Landkode2("SE") },
            )
        }

        @Test
        fun shouldMapUtenlandskFrittFormatAdresseMedManglendeAdresselinje1Og2() {
            val adresse = createUtenlandskAdresseIFrittFormat(adresselinje1 = "", adresselinje2 = null)
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            utenlandskAdresseIFrittFormat = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            assertAll(
                "adresse",
                { mappedValue?.adresselinje1 shouldBe "Gate 3" },
                { mappedValue?.adresselinje2 shouldBe null },
                { mappedValue?.adresselinje3 shouldBe null },
                { mappedValue?.postnummer shouldBe null },
                { mappedValue?.poststed shouldBe "Karavella" },
                { mappedValue?.land shouldBe Landkode2("SE") },
            )
        }

        @Test
        fun shouldMapUtenlandskFrittFormatAdresseMedManglendeAdresselinje2() {
            val adresse = createUtenlandskAdresseIFrittFormat(adresselinje2 = null)
            val personAdresse =
                HentPersonAdresse(
                    kontaktadresse =
                    listOf(
                        Kontaktadresse(
                            metadata = Metadata(Kilde.PDL.name),
                            gyldigFraOgMed = dateTime3,
                            type = KontaktadresseType.Innland,
                            utenlandskAdresseIFrittFormat = adresse,
                        ),
                    ),
                )

            val response = PersonAdresseResponse(personAdresse)
            val mappedValue = response.hentPostadresse()
            assertAll(
                "adresse",
                { mappedValue?.adresselinje1 shouldBe "Gate 1" },
                { mappedValue?.adresselinje2 shouldBe "Gate 3" },
                { mappedValue?.adresselinje3 shouldBe null },
                { mappedValue?.postnummer shouldBe null },
                { mappedValue?.poststed shouldBe "Karavella" },
                { mappedValue?.land shouldBe Landkode2("SE") },
            )
        }
    }

    private fun createKontaktadresseList(): List<Kontaktadresse> = listOf(
        Kontaktadresse(
            metadata = Metadata(Kilde.PDL.name),
            gyldigFraOgMed = dateTime1,
            type = KontaktadresseType.Innland,
            vegadresse = createVegadresse(),
        ),
        Kontaktadresse(
            metadata = Metadata(Kilde.FREG.name),
            gyldigFraOgMed = dateTime2,
            type = KontaktadresseType.Innland,
            vegadresse = createVegadresse(),
        ),
    )

    private fun createOppholdsadresseList(): List<Oppholdsadresse> = listOf(
        Oppholdsadresse(
            metadata = Metadata(Kilde.PDL.name),
            gyldigFraOgMed = dateTime1,
            vegadresse = createVegadresse(),
        ),
        Oppholdsadresse(
            metadata = Metadata(Kilde.FREG.name),
            gyldigFraOgMed = dateTime2,
            vegadresse = createVegadresse(),
        ),
    )

    private fun createBoststedsadresseList(): List<Bostedsadresse> = listOf(
        Bostedsadresse(
            metadata = Metadata(Kilde.PDL.name),
            angittFlyttedato = date1,
            vegadresse = createVegadresse(),
        ),
    )
}
