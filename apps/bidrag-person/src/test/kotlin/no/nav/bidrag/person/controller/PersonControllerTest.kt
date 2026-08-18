package no.nav.bidrag.person.controller

import com.fasterxml.jackson.annotation.JsonInclude
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.consumer.KrrConsumer
import no.nav.bidrag.person.query.HentSivilstand
import no.nav.bidrag.transport.person.ForelderBarnRelasjonDto
import no.nav.bidrag.transport.person.Fødselsdatoer
import no.nav.bidrag.transport.person.GeografiskTilknytningDto
import no.nav.bidrag.transport.person.Graderingsinfo
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.HusstandsmedlemmerDto
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.MotpartBarnRelasjon
import no.nav.bidrag.transport.person.MotpartBarnRelasjonDto
import no.nav.bidrag.transport.person.NavnFødselDødDto
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonidentDto
import no.nav.bidrag.transport.person.SivilstandPdlHistorikkDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.boot.resttestclient.postForEntity
import org.springframework.cache.CacheManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

internal class PersonControllerTest : AbstractControllerTest() {
    @Autowired
    @Qualifier("pdl")
    private val cacheManager: CacheManager? = null

    @Autowired
    private lateinit var krrConsumer: KrrConsumer

    @BeforeEach
    fun clearCache() {
        WireMock.resetAllScenarios()
        cacheManager!!.cacheNames.forEach {
            cacheManager.getCache(it)?.clear()
        }
    }

    @Test
    fun `skal returnere person-data`() {
        stubPDLEndpoint("pdl/pdl_response.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<PersonDto>(
                "http://localhost:$port/bidrag-person/informasjon/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val person = responseEntity.body

        person?.ident?.verdi shouldBe PERSON_FNR
        person?.aktørId shouldBe PERSON_AKTORID
        person?.navn shouldBe "SKILPADDE, BLÅ"
        person?.dødsdato shouldBe LocalDate.parse("2021-09-29")
        person?.diskresjonskode shouldBe Diskresjonskode.SPSF
    }

    @Test
    fun `skal returnere person-data med NPID`() {
        stubPDLEndpoint("pdl/pdl_response_npid.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<PersonDto>(
                "http://localhost:$port/bidrag-person/informasjon/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val person = responseEntity.body

        person?.ident?.verdi shouldBe PERSON_NPID
        person?.aktørId shouldBe PERSON_AKTORID
        person?.navn shouldBe "SKILPADDE, BLÅ"
        person?.dødsdato shouldBe LocalDate.parse("2021-09-29")
        person?.diskresjonskode shouldBe Diskresjonskode.SPSF
    }

    @Test
    fun `skal returnere 404 NOT FOUND for hentPersonData gjennom PDL`() {
        stubPDLEndpoint("pdl/pdl_response_not_found.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<PersonDto>(
                "http://localhost:$port/bidrag-person/informasjon/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `skal returnere 403 FORBIDDEN for hentPersonData gjennom PDL`() {
        stubPDLEndpoint("pdl/pdl_response_no_access.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<PersonDto>(
                "http://localhost:$port/bidrag-person/informasjon/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `skal returnere geografisk tilknytningsdata `() {
        stubPDLEndpoint("pdl/pdl_response_geo_diskresjon.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<GeografiskTilknytningDto>(
                "http://localhost:$port/bidrag-person/geografisktilknytning/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val geografiskTilknytning = responseEntity.body

        geografiskTilknytning?.ident?.verdi shouldBe PERSON_FNR
        geografiskTilknytning?.aktørId shouldBe PERSON_AKTORID
        geografiskTilknytning?.geografiskTilknytning shouldBe "3005"
        geografiskTilknytning?.diskresjonskode shouldBe Diskresjonskode.SPSF
    }

    @Test
    fun `skal returnere tom geografisk tilknytningsdata `() {
        stubPDLEndpoint("pdl/pdl_response_geo_empty.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<GeografiskTilknytningDto>(
                "http://localhost:$port/bidrag-person/geografisktilknytning/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val geografiskTilknytning = responseEntity.body

        geografiskTilknytning?.ident?.verdi shouldBe PERSON_FNR
        geografiskTilknytning?.aktørId shouldBe PERSON_AKTORID
        geografiskTilknytning?.geografiskTilknytning shouldBe null
        geografiskTilknytning?.diskresjonskode shouldBe null
    }

    @Test
    fun `Skal returnere 500 Internal Server Error når get kall til service hentGeografiskTilknytningData feiler`() {
        stubPDLEndpoint("pdl/pdl_response_geo_diskresjon.json", HttpStatus.INTERNAL_SERVER_ERROR)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<GeografiskTilknytningDto>(
                "http://localhost:$port/bidrag-person/geografisktilknytning/$PERSON_FNR",
            )

        responseEntity.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        responseEntity.headers shouldNotBe null
        responseEntity.body shouldBe null
    }

    @Test
    fun `Skal returnere 500 Internal Server Error når get kall til service hentSivilstand feiler`() {
        stubPDLEndpoint("pdl/pdl_response_sivilstand.json", HttpStatus.INTERNAL_SERVER_ERROR)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<SivilstandPdlHistorikkDto>(
                "http://localhost:$port/bidrag-person/geografisktilknytning/$PERSON_FNR",
            )

        responseEntity.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        responseEntity.headers shouldNotBe null
        responseEntity.body shouldBe null
    }

    @Test
    fun `skal returnere forelder barn-relasjonsdata`() {
        stubPDLEndpoint("pdl/pdl_response_forelder_barn_relasjon.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<ForelderBarnRelasjonDto>(
                "http://localhost:$port/bidrag-person/forelderbarnrelasjon/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val forelderBarnRelasjon = responseEntity.body?.forelderBarnRelasjon!![0]

        forelderBarnRelasjon.relatertPersonsIdent?.verdi shouldBe "12345678901"
        forelderBarnRelasjon.relatertPersonsRolle shouldBe Familierelasjon.BARN
        forelderBarnRelasjon.minRolleForPerson shouldBe Familierelasjon.FAR
    }

    @Test
    fun `skal returnere informasjon om navn, fødselsdato og fødselsår for angitt person, dato for eventuell død returneres også`() {
        stubPDLEndpoint("pdl/pdl_response_navn_fødselsdato_og_dødsfall.json")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<NavnFødselDødDto>(
                "http://localhost:$port/bidrag-person/navnfoedseldoed/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val navnFødselsdatoDødsfall = responseEntity.body

        navnFødselsdatoDødsfall?.fødselsdato shouldBe LocalDate.parse("2000-10-17")
        navnFødselsdatoDødsfall?.fødselsår shouldBe 2000
        navnFødselsdatoDødsfall?.dødsdato shouldBe LocalDate.parse("2022-01-17")
        navnFødselsdatoDødsfall?.navn shouldBe "Blå Mellomfornøyd Skilpadde"
    }

    @Test
    fun `skal returnere periodisert informasjon om husstand for angitt person med tilhørende medlemmer`() {
        stubPDLEndpoint("pdl/pdl_response_hent_bostedsadresse.json", null, "1")
        stubPDLEndpoint("pdl/pdl_response_husstandsmedlem.json", "1", "2")
        stubPDLEndpoint("pdl/pdl_response_hent_bostedsadresse.json", "2", "3")
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<HusstandsmedlemmerDto>(
                "http://localhost:$port/bidrag-person/husstandsmedlemmer/$PERSON_FNR",
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val husstand = responseEntity.body?.husstandListe!![0]
        val husstandsmedlem = responseEntity.body?.husstandListe!![0].husstandsmedlemListe[0]

        husstand.adressenavn shouldBe "Oredalsåsen"
        husstand.gyldigFraOgMed shouldBe LocalDate.parse("1977-05-18")
        husstand.gyldigTilOgMed shouldBe null
        husstand.bruksenhetsnummer shouldBe "H203"

        husstandsmedlem.personId.verdi shouldBe PERSON_FNR
        husstandsmedlem.navn shouldBe "Blå Mellomfornøyd Skilpadde"
        husstandsmedlem.fødselsdato shouldBe LocalDate.parse("2000-10-17")
        husstandsmedlem.dødsdato shouldBe LocalDate.parse("2022-01-17")
    }

    @Test
    fun `Skal hente spraak hvis saksbehandler har tilgang`() {
        stubPDLEndpoint("pdl/pdl_response.json")
        every { krrConsumer.hentPersonSpraak(Personident(PERSON_FNR)) } returns "NB"
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<String>(
                "http://localhost:$port/bidrag-person/spraak",
                httpEntity(PERSON_FNR),
            )
        responseEntity shouldNotBe null
    }

    @Test
    fun `Skal returnere statuskode 403 uten annet innhold dersom saksbehandler mangler tilgang`() {
        stubPDLEndpoint("pdl/pdl_response_no_access.json")
//        every { krrConsumer.hentPersonSpraak(any()) } returns "NB"
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<String>(
                "http://localhost:$port/bidrag-person/spraak",
                httpEntity(PERSON_FNR),
            )

        responseEntity shouldNotBe null
        responseEntity.statusCode shouldBe HttpStatus.FORBIDDEN
        responseEntity.body shouldBe null
    }

    @Test
    fun `skal returnere motpart-barn relasjon`() {
        stubPDLEndpoint("pdl/motpart-barn-relasjon/med-mor-og-far/pdl_response_person_far.json", null, "1")
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-og-far/pdl_response_forelder_barn_relasjon_far.json",
            "1",
            "2",
        )
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-og-far/pdl_response_forelder_barn_relasjon_barn.json",
            "2",
            "3",
        )
        stubPDLEndpoint("pdl/motpart-barn-relasjon/med-mor-og-far/pdl_response_person_mor.json", "3", "4")
        stubPDLEndpoint("pdl/motpart-barn-relasjon/med-mor-og-far/pdl_response_person_barn.json", "4", null)
        val ident = "07456732334"
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<MotpartBarnRelasjonDto>(
                "http://localhost:$port/bidrag-person/motpartbarnrelasjon",
                httpEntity(ident),
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val relasjon = responseEntity.body?.personensMotpartBarnRelasjon?.get(0)

        relasjon?.forelderrolleMotpart shouldBe Familierelasjon.MOR
        relasjon?.motpart?.ident?.verdi shouldBe "21508703020"
        relasjon?.fellesBarn?.size shouldBe 1
    }

    @Test
    fun `skal returnere kun liste av barn når motpart er ukjent`() {
        stubPDLEndpoint("pdl/motpart-barn-relasjon/ukjent-far/pdl_response_person_mor.json", null, "1")
        stubPDLEndpoint("pdl/motpart-barn-relasjon/ukjent-far/pdl_response_forelder_barn_relasjon_mor.json", "1", "2")
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/ukjent-far/pdl_response_forelder_barn_relasjon_barn_ukjent_far.json",
            "2",
            "3",
        )
        stubPDLEndpoint("pdl/motpart-barn-relasjon/ukjent-far/pdl_response_person_barn_ukjent_far.json", "3", null)
        val ident = "09449020216"
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<MotpartBarnRelasjonDto>(
                "http://localhost:$port/bidrag-person/motpartbarnrelasjon",
                httpEntity(ident),
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val relasjon = responseEntity.body?.personensMotpartBarnRelasjon?.get(0)

        relasjon?.forelderrolleMotpart shouldBe Familierelasjon.FAR
        relasjon?.motpart shouldBe null
        relasjon?.fellesBarn?.size shouldBe 1
    }

    @Test
    fun `skal returnere både FAR og MOR i forelderrolleMotpart dersom det er en person med både mor og far som forelderrolle`() {
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_person_mor.json",
            null,
            "1",
        )
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_forelder_barn_relasjon_mor.json",
            "1",
            "2",
        )
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_forelder_barn_relasjon_barn.json",
            "2",
            "3",
        )
        stubPDLEndpoint("pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_person_far.json", "3", "4")
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_person_barn.json",
            "4",
            "5",
        )
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_forelder_barn_relasjon_barn2.json",
            "5",
            "6",
        )
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_person_mor2.json",
            "6",
            "7",
        )
        stubPDLEndpoint(
            "pdl/motpart-barn-relasjon/med-mor-som-mor-og-far-rolle/pdl_response_person_barn2.json",
            "7",
            null,
        )
        val ident = "21508703021"
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<MotpartBarnRelasjonDto>(
                "http://localhost:$port/bidrag-person/motpartbarnrelasjon",
                httpEntity(ident),
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
        val muligMotpartBarnRelasjoner = responseEntity.body?.personensMotpartBarnRelasjon
        val forelderrolleMotpart =
            muligMotpartBarnRelasjoner?.map(MotpartBarnRelasjon::forelderrolleMotpart)?.sorted()

        muligMotpartBarnRelasjoner?.size shouldBe 2
        forelderrolleMotpart?.get(0) shouldBe Familierelasjon.FAR
        forelderrolleMotpart?.get(1) shouldBe Familierelasjon.MOR
    }

    @Nested
    internal inner class HentPersonAdresse {
        @Test
        fun `skal returnere person adresse`() {
            stubPDLEndpoint("pdl/pdl_adresse_respons_bosted.json")
            val responseEntity =
                httpHeaderTestRestTemplate.getForEntity<PersonAdresseDto>(
                    "http://localhost:$port/bidrag-person/adresse/$PERSON_FNR",
                )
            val adresse = responseEntity.body

            adresse?.adresselinje1 shouldBe "Oredalsåsen 48"
            adresse?.adresselinje2 shouldBe null
            adresse?.adresselinje3 shouldBe null
            adresse?.bruksenhetsnummer shouldBe "H0102"
            adresse?.land?.verdi shouldBe "NO"
            adresse?.postnummer shouldBe "1613"
            adresse?.poststed shouldBe "FREDRIKSTAD"
        }

        @Test
        fun `get Skal returnere person adresse utlandsk`() {
            stubPDLEndpoint("pdl/pdl_adresse_respons_utland.json")
            val responseEntity =
                httpHeaderTestRestTemplate.getForEntity<PersonAdresseDto>(
                    "http://localhost:$port/bidrag-person/adresse/$PERSON_FNR",
                )
            val adresse = responseEntity.body

            adresse?.adresselinje1 shouldBe "BIGGO 2, 34 C, DADDA"
            adresse?.adresselinje2 shouldBe "2323 DANDA RO"
            adresse?.adresselinje3 shouldBe null
            adresse?.land?.verdi shouldBe "AO"
            adresse?.postnummer shouldBe null
            adresse?.poststed shouldBe null
        }

        @Test
        fun `Skal returnere bosteds og kontaktadresse`() {
            // given
            stubPDLEndpoint("pdl/pdl_adresse_respons_bosted_og_kontakt.json")
            val personident = "$PERSON_FNR"

            // when
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<Array<PersonAdresseDto>>(
                    "http://localhost:$port/bidrag-person/adresse",
                    httpEntity(personident),
                )
            val adresser = responseEntity.body

            // then

            adresser shouldNotBe null
            adresser?.size shouldBe 2
            adresser?.get(0)?.adresselinje1 shouldBe "Bostedsåsen 48"
            adresser?.get(0)?.adresselinje2 shouldBe null
            adresser?.get(0)?.adresselinje3 shouldBe null
            adresser?.get(0)?.land?.verdi shouldBe "NO"
            adresser?.get(0)?.postnummer shouldBe "1613"
            adresser?.get(0)?.poststed shouldBe "FREDRIKSTAD"
            adresser?.get(1)?.adresselinje1 shouldBe "Kontaktåsen 48"
            adresser?.get(1)?.adresselinje2 shouldBe null
            adresser?.get(1)?.adresselinje3 shouldBe null
            adresser?.get(1)?.land?.verdi shouldBe "NO"
            adresser?.get(1)?.postnummer shouldBe "8613"
            adresser?.get(1)?.poststed shouldBe "MO I RANA"
        }

        @Test
        fun `Skal returnere utlandsk postadresse`() {
            stubPDLEndpoint("pdl/pdl_adresse_respons_utland.json")
            val personident = "$PERSON_FNR"
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<PersonAdresseDto>(
                    "http://localhost:$port/bidrag-person/adresse/post",
                    httpEntity(personident),
                )
            val adresse = responseEntity.body

            adresse shouldNotBe null
            adresse?.adressetype shouldBe Adressetype.KONTAKTADRESSE
            adresse?.adresselinje1 shouldBe "BIGGO 2, 34 C, DADDA"
            adresse?.adresselinje2 shouldBe "2323 DANDA RO"
            adresse?.adresselinje3 shouldBe null
            adresse?.land?.verdi shouldBe "AO"
            adresse?.postnummer shouldBe null
            adresse?.poststed shouldBe null
        }

        @Test
        fun `Endepunkt for henting av postadresse skal gi BAD_REQUEST dersom personident er null`() {
            stubPDLEndpoint("pdl/pdl_adresse_respons_utland.json")
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            val personRequest = PersonTestRequest()
            val entity = HttpEntity(personRequest, headers)
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<PersonAdresseDto>(
                    "http://localhost:$port/bidrag-person/adresse/post",
                    entity,
                )
            responseEntity.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `Endepunkt for henting av adresser skal gi BAD_REQUEST dersom personident er null`() {
            stubPDLEndpoint("pdl/pdl_adresse_respons_utland.json")
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            val personRequest = PersonTestRequest()
            val entity = HttpEntity(personRequest, headers)
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<PersonAdresseDto>(
                    "http://localhost:$port/bidrag-person/adresse",
                    entity,
                )
            responseEntity.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        @Test
        fun `Endepunkt for henting av adresser skal returnere landkode hvis landkode er ukjent`() {
            stubPDLEndpoint("pdl/pdl_adresse_respons_utland_null_landkode.json")
            val personident = "$PERSON_FNR"
            val responseEntity =
                httpHeaderTestRestTemplate.postForEntity<PersonAdresseDto>(
                    "http://localhost:$port/bidrag-person/adresse/post",
                    httpEntity(personident),
                )
            val landkode = responseEntity.body
            landkode?.land?.verdi shouldBe "UJ"
            landkode?.land3?.verdi shouldBe "XXA"
            responseEntity.statusCode shouldBe HttpStatus.OK
        }
    }

    @Nested
    internal inner class HentePersonidenter {
        @Test
        fun `skal hente nåværende og historiske identer for person`() {
            // gitt
            val gjeldendeFolkeregisterident = PERSON_FNR_2
            val hentePersonidenterRequest = HentePersonidenterRequest(gjeldendeFolkeregisterident)

            stubPDLEndpoint("pdl/pdl_hent_identer_respons.json")
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            val entitet = HttpEntity(hentePersonidenterRequest, headers)

            // hvis
            val respons =
                httpHeaderTestRestTemplate.postForEntity<Array<PersonidentDto>>(
                    "http://localhost:$port/bidrag-person/personidenter",
                    entitet,
                )

            // så
            respons.statusCode shouldBe HttpStatus.OK

            val returnerteIdenter = respons.body
            returnerteIdenter?.size shouldBe 5
            returnerteIdenter?.filter { it.historisk }?.size shouldBe 3
            returnerteIdenter?.filter { it.gruppe == Identgruppe.FOLKEREGISTERIDENT }?.size shouldBe 3
            returnerteIdenter?.filter { it.gruppe == Identgruppe.AKTORID }?.size shouldBe 2
            returnerteIdenter?.filter { it.ident == gjeldendeFolkeregisterident }?.size shouldBe 1
        }
    }

    @Test
    fun `skal returnere geografisk tilknytningsdata`() {
        stubPDLEndpoint("pdl/pdl_response_geo_diskresjon.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<GeografiskTilknytningDto>(
                "${baseURL()}/geografisktilknytning",
                httpEntity(PERSON_FNR),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        val geografiskTilknytning = responseEntity.body
        geografiskTilknytning?.ident?.verdi shouldBe PERSON_FNR
        geografiskTilknytning?.aktørId shouldBe PERSON_AKTORID
        geografiskTilknytning?.geografiskTilknytning shouldBe "3005"
        geografiskTilknytning?.diskresjonskode shouldBe Diskresjonskode.SPSF
    }

    @Test
    fun `skal returnere geografisk tilknytningsdata for person bosatt i utlandet`() {
        stubPDLEndpoint("pdl/pdl_response_geo_utland.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<GeografiskTilknytningDto>(
                "${baseURL()}/geografisktilknytning",
                httpEntity(PERSON_FNR),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        val geografiskTilknytning = responseEntity.body
        geografiskTilknytning?.ident?.verdi shouldBe PERSON_FNR
        geografiskTilknytning?.aktørId shouldBe PERSON_AKTORID
        geografiskTilknytning?.geografiskTilknytning shouldBe "SWE"
        geografiskTilknytning?.erUtland shouldBe true
    }

    @Test
    fun `skal returnere tom geografisk tilknytningsdata`() {
        stubPDLEndpoint("pdl/pdl_response_geo_empty.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<GeografiskTilknytningDto>(
                "${baseURL()}/geografisktilknytning",
                httpEntity(PERSON_FNR),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        val geografiskTilknytning = responseEntity.body
        geografiskTilknytning?.ident?.verdi shouldBe PERSON_FNR
        geografiskTilknytning?.aktørId shouldBe PERSON_AKTORID
        geografiskTilknytning?.geografiskTilknytning shouldBe null
        geografiskTilknytning?.diskresjonskode shouldBe null
    }

    @Test
    fun `Skal returnere 500 Internal Server Error når kall til service hentGeografiskTilknytningData feiler`() {
        stubPDLEndpoint("pdl/pdl_response_geo_diskresjon.json", HttpStatus.INTERNAL_SERVER_ERROR)

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<GeografiskTilknytningDto>(
                "${baseURL()}/geografisktilknytning",
                httpEntity("$PERSON_FNR"),
            )

        responseEntity.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        responseEntity.headers shouldNotBe null
        responseEntity.body shouldBe null
    }

    @Test
    fun `skal returnere SivilstandPdlDto`() {
        stubPDLEndpoint("pdl/pdl_response_sivilstand.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<SivilstandPdlHistorikkDto>(
                "${baseURL()}/sivilstand",
                httpEntity("$PERSON_FNR"),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        val sivilstand1 = responseEntity.body?.sivilstandPdlDto?.first()
        val sivilstand2 = responseEntity.body?.sivilstandPdlDto?.get(1)
        sivilstand1?.type shouldBe SivilstandskodePDL.GIFT
        sivilstand1?.gyldigFom shouldBe LocalDate.of(2021, 9, 29)
        sivilstand1?.bekreftelsesdato shouldBe LocalDate.of(2021, 9, 29)
        sivilstand2?.type shouldBe SivilstandskodePDL.SKILT
        sivilstand2?.gyldigFom shouldBe LocalDate.of(2022, 1, 17)
        sivilstand2?.bekreftelsesdato shouldBe LocalDate.of(2022, 1, 17)
    }

    @Test
    fun `Skal returnere 500 Internal Server Error når kall til service hentSivilstand feiler`() {
        stubPDLEndpoint("pdl/pdl_response_sivilstand.json", HttpStatus.INTERNAL_SERVER_ERROR)

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<HentSivilstand>(
                "${baseURL()}/geografisktilknytning",
                httpEntity("$PERSON_FNR"),
            )

        responseEntity.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        responseEntity.headers shouldNotBe null
        responseEntity.body shouldBe null
    }

    @Test
    fun `skal returnere ForelderBarnRelasjonDto`() {
        stubPDLEndpoint("pdl/pdl_response_forelder_barn_relasjon.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<ForelderBarnRelasjonDto>(
                "${baseURL()}/forelderbarnrelasjon",
                httpEntity("$PERSON_FNR"),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        val forelderBarnRelasjon = responseEntity.body?.forelderBarnRelasjon?.first()
        forelderBarnRelasjon?.relatertPersonsIdent?.verdi shouldBe "12345678901"
        forelderBarnRelasjon?.relatertPersonsRolle shouldBe Familierelasjon.BARN
        forelderBarnRelasjon?.minRolleForPerson shouldBe Familierelasjon.FAR
    }

    @Test
    fun `skal returnere informasjon om navn, fødselsdato, fødselsår og dato for eventuell død for angitt person`() {
        stubPDLEndpoint("pdl/pdl_response_navn_fødselsdato_og_dødsfall.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<NavnFødselDødDto>(
                "${baseURL()}/navnfoedseldoed",
                httpEntity("$PERSON_FNR"),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        val navnFoedselDoedsfall = responseEntity.body
        navnFoedselDoedsfall?.fødselsdato shouldBe LocalDate.of(2000, 10, 17)
        navnFoedselDoedsfall?.fødselsår shouldBe 2000
        navnFoedselDoedsfall?.dødsdato shouldBe LocalDate.of(2022, 1, 17)
        navnFoedselDoedsfall?.navn shouldBe "Blå Mellomfornøyd Skilpadde"
    }

    @Test
    fun `skal returnere map med identer og fødselsdato`() {
        stubPDLEndpoint("pdl/pd_personFødselsdatoResponse.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<Fødselsdatoer>(
                "${baseURL()}/fodselsdatoer",
                HttpEntity(listOf("$PERSON_FNR")),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        responseEntity.body?.identerTilDatoer?.size shouldBe 3
    }

    @Test
    fun `hentGraderinger skal returnere 200 OK`() {
        stubPDLEndpoint("pdl/pd_personGraderingResponse.json")

        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<Graderingsinfo>(
                "${baseURL()}/graderingsinfo",
                HttpEntity(listOf("$PERSON_FNR")),
            )

        responseEntity.statusCode shouldBe HttpStatus.OK
        responseEntity.body?.identerTilGradering?.size shouldBe 4
    }

    @Test
    fun `hentPersonPost skal returnere 200 OK`() {
        stubPDLEndpoint("pdl/pdl_response.json")
        val responseEntity =
            httpHeaderTestRestTemplate.postForEntity<PersonDto>(
                "${baseURL()}/informasjon",
                httpEntity("$PERSON_FNR"),
            )
        responseEntity.statusCode shouldBe HttpStatus.OK
    }

    fun httpEntity(personident: String) = HttpEntity(PersonRequest(personident))

    internal inner class PersonTestRequest(val ident: String? = null, val verdi: String? = ident)
}

@Validated
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonRequest(
    @NotNull
    @Schema(description = "Identen til personen")
    var ident: String,
)
