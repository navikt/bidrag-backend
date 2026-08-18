package no.nav.bidrag.person.service

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkConstructor
import io.mockk.verify
import no.nav.bidrag.commons.service.KodeverkProvider
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.enums.person.Kjønn
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.person.consumer.KontoregisterConsumer
import no.nav.bidrag.person.consumer.KrrConsumer
import no.nav.bidrag.person.consumer.PDLConsumer
import no.nav.bidrag.person.consumer.SkjermingConsumer
import no.nav.bidrag.person.query.Adressebeskyttelse
import no.nav.bidrag.person.query.Dødsfall
import no.nav.bidrag.person.query.Fødselsdato
import no.nav.bidrag.person.query.HentNavnFødselsdatoDødsfall
import no.nav.bidrag.person.query.HentPersonAdresse
import no.nav.bidrag.person.query.HentPersonBostedsadresse
import no.nav.bidrag.person.query.HentPersonBostedsadresseResponse
import no.nav.bidrag.person.query.Kilde
import no.nav.bidrag.person.query.Navn
import no.nav.bidrag.person.query.NavnFødselsdatoDødsfallResponse
import no.nav.bidrag.person.query.Oppholdsadresse
import no.nav.bidrag.person.query.PersonAdresseResponse
import no.nav.bidrag.person.query.PersonResponse
import no.nav.bidrag.person.query.PersonResponse.HentIdent
import no.nav.bidrag.person.query.PersonResponse.HentIdenter
import no.nav.bidrag.person.query.PersonResponse.HentPerson
import no.nav.bidrag.person.query.PersonResponse.HentPersonKjoenn
import no.nav.bidrag.person.testdata.createBostedsadresse
import no.nav.bidrag.person.testdata.createHentPersonAdresse
import no.nav.bidrag.person.testdata.createKontaktadresseInnland
import no.nav.bidrag.person.testdata.createOppholdsadresse
import no.nav.bidrag.person.testdata.lagHentPersonBostedsadresse
import no.nav.bidrag.person.testdata.lagHusstandsmedlemBolig2
import no.nav.bidrag.person.testdata.lagHusstandsmedlemMedDødsdatoBolig1
import no.nav.bidrag.person.testdata.lagHusstandsmedlemMedDødsdatoBolig2
import no.nav.bidrag.person.testdata.mockKodeverkResponse
import no.nav.bidrag.transport.person.ForelderBarnRelasjon
import no.nav.bidrag.transport.person.KontonummerDto
import no.nav.bidrag.transport.person.MetadataDto
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import java.time.LocalDate
import java.time.LocalDateTime

internal class PersonServiceTest {
    private val pdlConsumerMock: PDLConsumer = mockk(relaxed = true)

    private val krrConsumer: KrrConsumer = mockk(relaxed = true)

    private val kontoregisterConsumer: KontoregisterConsumer = mockk(relaxed = true)

    private val skjermingConsumer: SkjermingConsumer = mockk(relaxed = true)

    private val personService: PersonService =
        PersonService(krrConsumer, kontoregisterConsumer, pdlConsumerMock, skjermingConsumer)

    companion object {
        private val PERSONID = Personident(genererFødselsnummer())

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

    @Nested
    internal inner class HentPersondata {
        @Test
        fun `Skal konvertere OIDC token og få SAML token tilbake og deretter kalle person og få persondata tilbake`() {
            val hentPerson = HentPersonAdresse()
            hentPerson.oppholdsadresse.sortedWith(
                compareByDescending<Oppholdsadresse> { it.isMasterPDL() }.thenByDescending { it.gyldigFraOgMed },
            )
                .firstOrNull()

            every { pdlConsumerMock.hentPersonInfo(PERSONID) } returns
                PersonResponse(
                    HentPerson(
                        navn = listOf(Navn("Sylfest", null, "Strutle")),
                        kjoenn = listOf(HentPersonKjoenn(Kjønn.MANN)),
                        adressebeskyttelse = listOf(Adressebeskyttelse(Gradering.FORTROLIG)),
                        oppholdsadresse = listOf(),
                        doedsfall = listOf(Dødsfall(LocalDate.now())),
                        foedselsdato = listOf(),
                    ),
                    HentIdenter(listOf(HentIdent(PERSONID.verdi, false, "FOLKEREGISTERIDENT"))),
                )

            val response = personService.hentPersonInfo(PERSONID)

            response shouldNotBe null
            response.ident shouldBe PERSONID
            response.navn shouldBe "Strutle, Sylfest"
            response.dødsdato shouldBe LocalDate.now()
            response.diskresjonskode shouldBe Diskresjonskode.SPFO
        }
    }

    @Nested
    internal inner class HentPersondetaljer {
        @Test
        fun `skal hente kontoinformasjon for person med norsk kontonummer`() {
            // gitt
            val testident = Personident("123")
            val personensKontonummer = "97862541234"
            val opprettetTidspunkt = LocalDateTime.now()

            every { kontoregisterConsumer.hentKontonummer(testident) } returns
                KontonummerDto(
                    norskKontonr = personensKontonummer,
                    metadata = MetadataDto(opprettetTidspunkt, "NAV", "FREG"),
                )

            // hvis
            val persondetaljer = personService.hentPersondetaljer(testident)

            // så
            persondetaljer.kontonummer?.norskKontonr shouldBe personensKontonummer
            persondetaljer.kontonummer?.metadata?.gyldigFom shouldBe opprettetTidspunkt
        }

        @Test
        fun `skal hente kontoinformasjon for person med utenlandsk kontonummer`() {
            // gitt
            val testident = Personident("123")

            val internasjonaltBankkontonummer = "SE0580085145487169130520"
            val bankadresse1 = "Lykkliga gatan 5"
            val opprettetTidspunkt = LocalDateTime.now()

            every { kontoregisterConsumer.hentKontonummer(testident) } returns
                KontonummerDto(
                    iban = internasjonaltBankkontonummer,
                    bankadresse1 = bankadresse1,
                    metadata = MetadataDto(opprettetTidspunkt, "NAV", "FREG"),
                )

            // hvis
            val persondetaljer = personService.hentPersondetaljer(testident)

            // så
            persondetaljer.kontonummer?.iban shouldBe internasjonaltBankkontonummer
            persondetaljer.kontonummer?.bankadresse1 shouldBe bankadresse1
            persondetaljer.kontonummer?.metadata?.gyldigFom shouldBe opprettetTidspunkt
        }
    }

    @Nested
    internal inner class HentPersonPostadresse {
        @Test
        fun skalReturnereBostedsadresseSomPostadresseHvisNyereEnnKontaktadresse() {
            // given
            val bostedsadresse = createBostedsadresse(Kilde.FREG, LocalDateTime.now().minusYears(5))
            val kontaktadresse = createKontaktadresseInnland(Kilde.FREG, LocalDateTime.now().minusYears(10))
            val hentPersonAdresse = createHentPersonAdresse(bostedsadresse, kontaktadresse)
            every { pdlConsumerMock.hentPersonAdresse(PERSONID) } returns PersonAdresseResponse(hentPersonAdresse)

            // when
            val respons = personService.hentPersonPostadresse(PERSONID)

            // then
            val adresse =
                bostedsadresse.vegadresse!!.adressenavn.toString() + " " +
                    bostedsadresse.vegadresse!!.husnummer + bostedsadresse.vegadresse!!.husbokstav
            respons?.adresselinje1 shouldBe adresse
        }
    }

    @Nested
    internal inner class HentPersonAdresseTest {
        @Test
        fun skalReturnereAlleTilgjengeligeAdressetyperForPerson() {
            // given
            val bostedsadresse = createBostedsadresse(Kilde.FREG, LocalDateTime.now().minusYears(5))
            val kontaktadresse = createKontaktadresseInnland(Kilde.FREG, LocalDateTime.now().minusYears(10))
            val oppholdsadresse = createOppholdsadresse(Kilde.FREG, LocalDateTime.now().minusYears(10))
            val hentPersonAdresse = createHentPersonAdresse(bostedsadresse, kontaktadresse, oppholdsadresse)
            every { pdlConsumerMock.hentPersonAdresse(PERSONID) } returns PersonAdresseResponse(hentPersonAdresse)

            // when
            val respons = personService.hentPersonAdresser(PERSONID)

            // then

            respons.size shouldBe 3
            respons.any { it.adressetype == Adressetype.BOSTEDSADRESSE } shouldBe true
            respons.any { it.adressetype == Adressetype.OPPHOLDSADRESSE } shouldBe true
            respons.any { it.adressetype == Adressetype.KONTAKTADRESSE } shouldBe true
        }
    }

    @Nested
    internal inner class HentPersonSpråk {
        @Test
        fun skalReturnereKode200HvisSpraakErTilgjengelig() {
            // given
            every { krrConsumer.hentPersonSpraak(PERSONID) } returns "NB"

            // when
            val respons = personService.hentPersonSpraak(PERSONID)

            // then

            respons shouldNotBe null
            respons shouldBe "NB"
        }

        @Test
        fun skalReturnereKode204HvisSpraakMangler() {
            // given
            every { krrConsumer.hentPersonSpraak(PERSONID) } returns null

            // when
            val respons = personService.hentPersonSpraak(PERSONID)

            // then
            respons shouldBe null
        }
    }

    @Nested
    internal inner class HentHusstandsmedlemmerTest {
        @Test
        fun testAtPerioderForHusstandsmedlemBegrensesAvBMsPerioder() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) } returns lagHusstandsmedlemBolig2()
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[1]) } returns lagHusstandsmedlemBolig2()

            // when
            val respons = personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 2
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-09-04")
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2023-11-12")
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-11-29")
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigTilOgMed shouldBe null
        }

        @Test
        fun testAtPerioderForHusstandsmedlemBegrensesAvHusstandsmedlemmetsDødsdato() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)
            every {
                pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0])
            } returns lagHusstandsmedlemMedDødsdatoBolig1()
            every {
                pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[1])
            } returns lagHusstandsmedlemMedDødsdatoBolig2()

            // when
            val respons = personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 2
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-09-04")
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2023-11-12")
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-11-29")
            // Dødsdato
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2024-04-24")
        }

        @Test
        fun testAtMedNullIAlleDatoer() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        null,
                        null,
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        null,
                        null,
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) } returns lagHusstandsmedlemBolig2()

            // when
            val respons = personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 1
            respons.husstandListe[0].gyldigFraOgMed shouldBe null
            respons.husstandListe[0].gyldigTilOgMed shouldBe null
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe null
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe null
        }

        @Test
        fun testAtMedNullIAlleBMsDatoer() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        null,
                        null,
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) } returns lagHusstandsmedlemBolig2()

            // when
            val respons = personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 1
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-09-04")
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2023-11-29")
        }

        @Test
        fun testFørPeriode() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2020-09-04T00:00"),
                        LocalDateTime.parse("2020-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2020-09-03"),
                        LocalDateTime.parse("2020-11-29T00:00"),
                        LocalDateTime.parse("2021-04-12T00:00"),
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) } returns lagHusstandsmedlemBolig2()
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[1]) } returns lagHusstandsmedlemBolig2()

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

            // when
            val respons = personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 2
            respons.husstandListe[0].husstandsmedlemListe.size shouldBe 0
            respons.husstandListe[1].husstandsmedlemListe.size shouldBe 0
        }

        @Test
        fun `skal kun hente første side en gang når antall husstandsmedlemmer er under sidegrense`() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            val husstandsmedlemsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(husstandsmedlemsBostedsadresser)
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) } returns lagHusstandsmedlemBolig2()

            // when
            personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then
            verify(exactly = 1) { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) }
        }

        @Test
        fun `skal cache bostedsadresser for husstandsmedlem innen samme request`() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val husstandsmedlemsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(husstandsmedlemsBostedsadresser)
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[0]) } returns lagHusstandsmedlemBolig2()
            every { pdlConsumerMock.hentHusstandsmedlemmer(1, 50, bMsBostedsadresser.bostedsadresse[1]) } returns lagHusstandsmedlemBolig2()

            // when
            personService.hentHusstandsmedlemmer(Personident("123"), LocalDate.parse("2023-01-01"))

            // then
            verify(exactly = 1) { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) }
        }
    }

    @Nested
    internal inner class HentHusstandsmedlemskapEgneBarnTest {
        @Test
        fun testAtPerioderForHusstandsmedlemBegrensesAvBMsPerioder() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)
            every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
                ForelderBarnRelasjon(
                    minRolleForPerson = Familierelasjon.MOR,
                    relatertPersonsIdent = Personident("234"),
                    relatertPersonsRolle = Familierelasjon.BARN,
                ),
            )

            every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
                NavnFødselsdatoDødsfallResponse(
                    hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                        navn = listOf(Navn("Sylfest", null, "Strutle")),
                        foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                        doedsfall = emptyList(),
                    ),
                )

            // when
            val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2023-01-01"))

            // then
            respons.husstandListe.size shouldBe 2
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-09-04")
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2023-11-12")
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-11-29")
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigTilOgMed shouldBe null
        }

        @Test
        fun testAtPerioderForHusstandsmedlemskapBarnBegrensesAvHusstandsmedlemmetsDødsdato() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

            every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
                ForelderBarnRelasjon(
                    minRolleForPerson = Familierelasjon.MOR,
                    relatertPersonsIdent = Personident("234"),
                    relatertPersonsRolle = Familierelasjon.BARN,
                ),
            )

            every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
                NavnFødselsdatoDødsfallResponse(
                    hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                        navn = listOf(Navn("Sylfest", null, "Strutle")),
                        foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                        doedsfall = listOf(Dødsfall(LocalDate.parse("2024-04-24"))),
                    ),
                )

            // when
            val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 2
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-09-04")
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2023-11-12")
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-11-29")
            // Dødsdato
            respons.husstandListe[1].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2024-04-24")
        }

        @Test
        fun testAtMedNullIAlleDatoer() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        null,
                        null,
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        null,
                        null,
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

            every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
                ForelderBarnRelasjon(
                    minRolleForPerson = Familierelasjon.MOR,
                    relatertPersonsIdent = Personident("234"),
                    relatertPersonsRolle = Familierelasjon.BARN,
                ),
            )

            every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
                NavnFødselsdatoDødsfallResponse(
                    hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                        navn = listOf(Navn("Sylfest", null, "Strutle")),
                        foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                        doedsfall = emptyList(),
                    ),
                )

            // when
            val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 1
            respons.husstandListe[0].gyldigFraOgMed shouldBe null
            respons.husstandListe[0].gyldigTilOgMed shouldBe null
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe null
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe null
        }

        @Test
        fun testAtMedNullIAlleBMsDatoer() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        null,
                        null,
                        null,
                        "Bolig 1",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-11-29T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

            every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
                ForelderBarnRelasjon(
                    minRolleForPerson = Familierelasjon.MOR,
                    relatertPersonsIdent = Personident("234"),
                    relatertPersonsRolle = Familierelasjon.BARN,
                ),
            )

            every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
                NavnFødselsdatoDødsfallResponse(
                    hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                        navn = listOf(Navn("Sylfest", null, "Strutle")),
                        foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                        doedsfall = emptyList(),
                    ),
                )

            // when
            val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 1
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2023-09-04")
            respons.husstandListe[0].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2023-11-29")
        }

        @Test
        fun testFørPeriode() {
            // given
            val bMsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2023-09-04T00:00"),
                        LocalDateTime.parse("2023-11-12T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-11-03"),
                        LocalDateTime.parse("2023-11-13T00:00"),
                        null,
                        "Bolig 2",
                    ),
                ),
            )

            val barnetsBostedsadresser = HentPersonBostedsadresse(
                listOf(
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2023-09-03"),
                        LocalDateTime.parse("2020-09-04T00:00"),
                        LocalDateTime.parse("2020-11-29T00:00"),
                        "Bolig 1",
                    ),
                    lagHentPersonBostedsadresse(
                        LocalDate.parse("2020-09-03"),
                        LocalDateTime.parse("2020-11-29T00:00"),
                        LocalDateTime.parse("2021-04-12T00:00"),
                        "Bolig 2",
                    ),
                ),
            )

            every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
                ForelderBarnRelasjon(
                    minRolleForPerson = Familierelasjon.MOR,
                    relatertPersonsIdent = Personident("234"),
                    relatertPersonsRolle = Familierelasjon.BARN,
                ),
            )

            every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
                NavnFødselsdatoDødsfallResponse(
                    hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                        navn = listOf(Navn("Sylfest", null, "Strutle")),
                        foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                        doedsfall = emptyList(),
                    ),
                )

            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
            every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

            // when
            val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2023-01-01"))

            // then

            respons.husstandListe.size shouldBe 2
            respons.husstandListe[0].husstandsmedlemListe.size shouldBe 0
            respons.husstandListe[1].husstandsmedlemListe.size shouldBe 0
        }
    }

    @Test
    fun testEtterPeriode() {
        // given
        val bMsBostedsadresser = HentPersonBostedsadresse(
            listOf(
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2020-09-03"),
                    LocalDateTime.parse("2020-09-04T00:00"),
                    LocalDateTime.parse("2020-11-12T00:00"),
                    "Bolig 1",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2020-11-03"),
                    LocalDateTime.parse("2020-11-13T00:00"),
                    LocalDateTime.parse("2021-08-10T00:00"),
                    "Bolig 2",
                ),
            ),
        )

        val barnetsBostedsadresser = HentPersonBostedsadresse(
            listOf(
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2022-09-03"),
                    LocalDateTime.parse("2022-09-04T00:00"),
                    LocalDateTime.parse("2022-11-29T00:00"),
                    "Bolig 1",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2023-09-03"),
                    LocalDateTime.parse("2023-11-29T00:00"),
                    null,
                    "Bolig 2",
                ),
            ),
        )

        every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
            ForelderBarnRelasjon(
                minRolleForPerson = Familierelasjon.MOR,
                relatertPersonsIdent = Personident("234"),
                relatertPersonsRolle = Familierelasjon.BARN,
            ),
        )

        every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
            NavnFødselsdatoDødsfallResponse(
                hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                    navn = listOf(Navn("Sylfest", null, "Strutle")),
                    foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                    doedsfall = emptyList(),
                ),
            )

        every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
        every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

        val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2017-01-01"))

        respons.husstandListe.size shouldBe 2
        respons.husstandListe[0].husstandsmedlemListe.size shouldBe 0
        respons.husstandListe[1].husstandsmedlemListe.size shouldBe 0
    }

    @Test
    fun testDelvisOverlapp() {
        // given
        val bMsBostedsadresser = HentPersonBostedsadresse(
            listOf(
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2020-09-03"),
                    LocalDateTime.parse("2020-09-04T00:00"),
                    LocalDateTime.parse("2020-11-12T00:00"),
                    "Bolig 1",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2020-11-03"),
                    LocalDateTime.parse("2020-11-13T00:00"),
                    LocalDateTime.parse("2021-08-10T00:00"),
                    "Bolig 2",
                ),
            ),
        )

        val barnetsBostedsadresser = HentPersonBostedsadresse(
            listOf(
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2019-04-03"),
                    LocalDateTime.parse("2019-04-04T00:00"),
                    LocalDateTime.parse("2019-11-29T00:00"),
                    "Bolig 1",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2021-02-03"),
                    LocalDateTime.parse("2021-02-04T00:00"),
                    null,
                    "Bolig 2",
                ),
            ),
        )

        every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
            ForelderBarnRelasjon(
                minRolleForPerson = Familierelasjon.MOR,
                relatertPersonsIdent = Personident("234"),
                relatertPersonsRolle = Familierelasjon.BARN,
            ),
        )

        every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
            NavnFødselsdatoDødsfallResponse(
                hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                    navn = listOf(Navn("Sylfest", null, "Strutle")),
                    foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                    doedsfall = emptyList(),
                ),
            )
        every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
        every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

        val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2017-01-01"))

        respons.husstandListe.size shouldBe 2
        respons.husstandListe[0].husstandsmedlemListe.size shouldBe 0
        respons.husstandListe[1].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2021-02-04")
        respons.husstandListe[1].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2021-08-10")
    }

    @Test
    fun testAtAdresserSomErFørPeriodeFraFiltreresBort() {
        // given
        val bMsBostedsadresser = HentPersonBostedsadresse(
            listOf(
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2017-09-03"),
                    LocalDateTime.parse("2017-09-04T00:00"),
                    LocalDateTime.parse("2020-09-03T00:00"),
                    "Bolig 1",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2020-09-03"),
                    LocalDateTime.parse("2020-09-04T00:00"),
                    LocalDateTime.parse("2020-11-12T00:00"),
                    "Bolig 2",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2020-11-03"),
                    LocalDateTime.parse("2020-11-13T00:00"),
                    LocalDateTime.parse("2021-08-10T00:00"),
                    "Bolig 3",
                ),
            ),
        )

        val barnetsBostedsadresser = HentPersonBostedsadresse(
            listOf(
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2019-04-03"),
                    LocalDateTime.parse("2019-04-04T00:00"),
                    LocalDateTime.parse("2019-11-29T00:00"),
                    "Bolig 1",
                ),
                lagHentPersonBostedsadresse(
                    LocalDate.parse("2021-02-03"),
                    LocalDateTime.parse("2021-02-04T00:00"),
                    null,
                    "Bolig 3",
                ),
            ),
        )

        every { pdlConsumerMock.hentForelderBarnRelasjoner(Personident("123")) } returns listOf(
            ForelderBarnRelasjon(
                minRolleForPerson = Familierelasjon.MOR,
                relatertPersonsIdent = Personident("234"),
                relatertPersonsRolle = Familierelasjon.BARN,
            ),
        )

        every { pdlConsumerMock.hentNavnFødselsdatoDødsfall(Personident("234")) } returns
            NavnFødselsdatoDødsfallResponse(
                hentNavnFødselsdatoDødsfall = HentNavnFødselsdatoDødsfall(
                    navn = listOf(Navn("Sylfest", null, "Strutle")),
                    foedselsdato = listOf(Fødselsdato(LocalDate.parse("2020-09-03"), 2020)),
                    doedsfall = emptyList(),
                ),
            )

        every { pdlConsumerMock.hentPersonBostedsadresse(Personident("123")) } returns HentPersonBostedsadresseResponse(bMsBostedsadresser)
        every { pdlConsumerMock.hentPersonBostedsadresse(Personident("234")) } returns HentPersonBostedsadresseResponse(barnetsBostedsadresser)

        val respons = personService.hentHusstandsmedlemskapEgneBarn(Personident("123"), LocalDate.parse("2020-09-04"))

        respons.husstandListe.size shouldBe 2
        respons.husstandListe[0].husstandsmedlemListe.size shouldBe 0
        respons.husstandListe[1].husstandsmedlemListe.size shouldBe 1

        respons.husstandListe[1].husstandsmedlemListe[0].gyldigFraOgMed shouldBe LocalDate.parse("2021-02-04")
        respons.husstandListe[1].husstandsmedlemListe[0].gyldigTilOgMed shouldBe LocalDate.parse("2021-08-10")
    }
}
