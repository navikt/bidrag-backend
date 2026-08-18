package no.nav.bidrag.person.dto

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.enums.person.Kjønn
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.person.query.Adressebeskyttelse
import no.nav.bidrag.person.query.Dødsfall
import no.nav.bidrag.person.query.Endring
import no.nav.bidrag.person.query.Endringstype
import no.nav.bidrag.person.query.Folkeregistermetadata
import no.nav.bidrag.person.query.Fødselsdato
import no.nav.bidrag.person.query.Kilde
import no.nav.bidrag.person.query.Navn
import no.nav.bidrag.person.query.OppholdAnnetSted
import no.nav.bidrag.person.query.OppholdsadresseCommon
import no.nav.bidrag.person.query.PersonResponse
import no.nav.bidrag.person.query.PersonResponse.HentIdent
import no.nav.bidrag.person.query.PersonResponse.HentIdenter
import no.nav.bidrag.person.query.PersonResponse.HentPerson
import no.nav.bidrag.person.query.PersonResponse.HentPersonKjoenn
import no.nav.bidrag.transport.person.Identgruppe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.bidrag.person.query.Metadata as PdlMetadata

class PersonQueryTest {

    @Test
    fun `skal bruke nyeste navn dersom både Pdl og Freg returnerer aktive navn`() {
        // gitt
        val aktivtNavnFraPDL = oppretteNavn(kilde = Kilde.PDL)
        val aktivtNavnFraFreg = oppretteNavn(registrert = LocalDateTime.now().minusDays(2)).copy(fornavn = "Børre")
        val personResponse = PersonResponse(hentPerson = HentPerson(navn = listOf(aktivtNavnFraPDL, aktivtNavnFraFreg)), hentIdenter = HentIdenter())

        // hvis
        val personDto = personResponse.mapToPersonDto()

        // så
        personDto.fornavn shouldBe aktivtNavnFraPDL.fornavn
    }

    @Test
    fun `skal mappe PDL respons til person dto`() {
        val personResponse = createPersonResponseWithDiskresjon(Gradering.STRENGT_FORTROLIG, OppholdAnnetSted.MILITAER)
        val personDto = personResponse.mapToPersonDto()

        personDto.aktørId shouldBe AKTOR_ID
        personDto.ident.verdi shouldBe PERSON_ID
        personDto.navn shouldBe "Etternavn, Fornavn Mellomnavn"
        personDto.diskresjonskode shouldBe Diskresjonskode.SPSF
        personDto.dødsdato shouldBe LocalDate.of(2019, 1, 2)
    }

    @Test
    fun `skal mappe PDL respons til person dto med forkortet navn`() {
        val fødselsdato = Fødselsdato(LocalDate.of(2019, 1, 2), 2020)
        val navn = Navn(FIRSTNAME, MIDDLENAME, LASTNAME)
        val kjoenn = HentPersonKjoenn(Kjønn.KVINNE)
        val hentPerson = HentPerson(listOf(navn), listOf(kjoenn), listOf(), listOf(), listOf(), listOf(fødselsdato))
        val ident = HentIdent(PERSON_ID, false, Identgruppe.FOLKEREGISTERIDENT.name)
        val aktoerId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        val hentIdenter = HentIdenter(listOf(ident, aktoerId))
        val personResponse = PersonResponse(hentPerson, hentIdenter)
        val personDto = personResponse.mapToPersonDto()

        personDto.aktørId shouldBe AKTOR_ID
        personDto.ident.verdi shouldBe PERSON_ID
        personDto.navn shouldBe "Etternavn, Fornavn Mellomnavn"
        personDto.kortnavn shouldBe "Fornavn Etternavn"
        personDto.diskresjonskode shouldBe null
        personDto.dødsdato shouldBe null
    }

    @Test
    fun `skal mappe PDL respons til person dto kortnavn til fornavn etternavn hvis forkortet navn mangler`() {
        val fødselsdato = Fødselsdato(LocalDate.of(2019, 1, 2), 2020)
        val navn = Navn(FIRSTNAME, MIDDLENAME, LASTNAME)
        val kjoenn = HentPersonKjoenn(Kjønn.KVINNE)
        val hentPerson = HentPerson(listOf(navn), listOf(kjoenn), listOf(), listOf(), listOf(), listOf(fødselsdato))
        val ident = HentIdent(PERSON_ID, false, Identgruppe.FOLKEREGISTERIDENT.name)
        val aktoerId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        val hentIdenter = HentIdenter(listOf(ident, aktoerId))
        val personResponse = PersonResponse(hentPerson, hentIdenter)
        val personDto = personResponse.mapToPersonDto()

        personDto.aktørId shouldBe AKTOR_ID
        personDto.ident.verdi shouldBe PERSON_ID
        personDto.navn shouldBe "Etternavn, Fornavn Mellomnavn"
        personDto.kortnavn shouldBe String.format("%s %s", FIRSTNAME, LASTNAME)
        personDto.kortnavn shouldBe String.format("%s %s", FIRSTNAME, LASTNAME)
        personDto.diskresjonskode shouldBe null
        personDto.dødsdato shouldBe null
    }

    @Test
    fun `skal mappe PDL respons til person dto kortnavn til fornavn etternavn hvis forkortet navn er tom`() {
        val fødselsdato = Fødselsdato(LocalDate.of(2019, 1, 2), 2020)
        val navn = Navn(FIRSTNAME, MIDDLENAME, LASTNAME)
        val kjoenn = HentPersonKjoenn(Kjønn.KVINNE)
        val hentPerson = HentPerson(listOf(navn), listOf(kjoenn), listOf(), listOf(), listOf(), listOf(fødselsdato))
        val ident = HentIdent(PERSON_ID, false, Identgruppe.FOLKEREGISTERIDENT.name)
        val aktoerId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        val hentIdenter = HentIdenter(listOf(ident, aktoerId))
        val personResponse = PersonResponse(hentPerson, hentIdenter)
        val personDto = personResponse.mapToPersonDto()

        personDto.aktørId shouldBe AKTOR_ID
        personDto.ident.verdi shouldBe PERSON_ID
        personDto.navn shouldBe "Etternavn, Fornavn Mellomnavn"
        personDto.kortNavn shouldBe "$FIRSTNAME $LASTNAME"
        personDto.kortnavn shouldBe "$FIRSTNAME $LASTNAME"
        personDto.diskresjonskode shouldBe null
        personDto.dødsdato shouldBe null
    }

    @Test
    fun `skal mappe PDL respons til person dto med tom diskresjon`() {
        val personResponse = createPersonResponseWithDiskresjon(null, null)
        val personDto = personResponse.mapToPersonDto()
        personDto.diskresjonskode shouldBe null
    }

    @Test
    fun `skal mappe PDL respons til person dto med SPFO diskresjon`() {
        val personResponse = createPersonResponseWithDiskresjon(Gradering.FORTROLIG, null)
        val personDto = personResponse.mapToPersonDto()
        personDto.diskresjonskode shouldBe Diskresjonskode.SPFO
    }

    @Test
    fun `skal mappe PDL respons til person dto med MILI diskresjon`() {
        val personResponse = createPersonResponseWithDiskresjon(null, OppholdAnnetSted.MILITAER)
        val personDto = personResponse.mapToPersonDto()
        personDto.diskresjonskode shouldBe Diskresjonskode.MILI
    }

    @Test
    fun `skal mappe PDL respons med person NPID`() {
        val personResponse = createPersonResponseWithDiskresjon(null, null)
        val ident = HentIdent(PERSON_NPR_ID, false, Identgruppe.NPID.name)
        val aktørId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        val hentIdenter = HentIdenter(listOf(ident, aktørId))
        personResponse.hentIdenter = hentIdenter
        val personDto = personResponse.mapToPersonDto()
        personDto.ident.verdi shouldBe PERSON_NPR_ID
        personDto.aktørId shouldBe AKTOR_ID
    }

    @Test
    fun `skal mappe PDL respons med person fnr hvis inneholder NPID og FNR`() {
        val personResponse = createPersonResponseWithDiskresjon(null, null)
        val ident = HentIdent(PERSON_ID, false, Identgruppe.FOLKEREGISTERIDENT.name)
        val identNP = HentIdent(PERSON_NPR_ID, false, Identgruppe.NPID.name)
        val aktoerId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        val hentIdenter = HentIdenter(listOf(ident, aktoerId, identNP))
        personResponse.hentIdenter = hentIdenter
        val personDto = personResponse.mapToPersonDto()
        personDto.ident.verdi shouldBe PERSON_ID
        personDto.aktørId shouldBe AKTOR_ID
    }

    @Test
    fun `skal mappe person navn med stor forbokstav`() {
        val personResponse =
            PersonResponse(
                hentPerson = HentPerson(navn = listOf(Navn("MIKEAL", "MELLOMSEN", "ETTERSEN"))),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.fornavn shouldBe "Mikeal"
        personDto.mellomnavn shouldBe "Mellomsen"
        personDto.etternavn shouldBe "Ettersen"
    }

    @Test
    fun `skal mappe person kortnavn`() {
        val personResponse =
            PersonResponse(
                hentPerson = HentPerson(navn = listOf(Navn("MIKEAL", "MELLOMSEN", "ETTERSEN"))),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.kortnavn shouldBe "Mikeal Ettersen"
    }

    @Test
    fun `skal mappe person visningsnavn`() {
        val personResponse =
            PersonResponse(
                hentPerson = HentPerson(navn = listOf(Navn("MIKEAL", null, "ETTERSEN"))),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.visningsnavn shouldBe "Mikeal Ettersen"
    }

    @Test
    fun `skal mappe person visningsnavn med mellomnavn`() {
        val personResponse =
            PersonResponse(
                hentPerson = HentPerson(navn = listOf(Navn("MIKEAL-KALLE", "Mekane", "Ettersen"))),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.visningsnavn shouldBe "Mikeal-Kalle Mekane Ettersen"
    }

    @Test
    fun `skal mappe person visningsnavn med mellomnavn hvis lengre enn 100 tegn`() {
        val personResponse =
            PersonResponse(
                hentPerson =
                HentPerson(
                    navn =
                    listOf(
                        Navn(
                            "MIKEAL-KALLE",
                            "Mekane SAMUEL JACKSON RULLERENDE VÅTMOPP Mekane SAMUEL JACKSON RULLERENDE VÅTMOPP",
                            "Ettersen",
                        ),
                    ),
                ),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.visningsnavn shouldBe "Mikeal-Kalle M. Ettersen"
    }

    @Test
    fun `skal mappe person visningsnavn hvis navn mangler`() {
        val personResponse =
            PersonResponse(
                hentPerson = HentPerson(navn = listOf()),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.visningsnavn shouldBe ""
    }

    @Test
    fun `skal mappe person visningsnavn med flere mellomnavn`() {
        val personResponse =
            PersonResponse(
                hentPerson = HentPerson(navn = listOf(Navn("MIKEAL-KALLE", "Mekane SAMUEL JACKSON", "Ettersen"))),
                hentIdenter = HentIdenter(),
            )
        val personDto = personResponse.mapToPersonDto()

        personDto.visningsnavn shouldBe "Mikeal-Kalle Mekane Samuel Jackson Ettersen"
    }

    private fun createPersonResponseWithDiskresjon(gradering: Gradering?, oppholdAnnetSted: OppholdAnnetSted?): PersonResponse {
        val adressebeskyttelse = if (gradering == null) listOf() else listOf(Adressebeskyttelse(gradering))
        val oppholdsadresse =
            if (oppholdAnnetSted == null) listOf() else listOf(OppholdsadresseCommon(oppholdAnnetSted))
        val dødsfall = Dødsfall(LocalDate.of(2019, 1, 2))
        val fødselsdato = Fødselsdato(LocalDate.of(2019, 1, 2), 2020)
        val navn = Navn(FIRSTNAME, MIDDLENAME, LASTNAME)
        val kjoenn = HentPersonKjoenn(Kjønn.MANN)
        val hentPerson =
            HentPerson(
                listOf(navn),
                listOf(kjoenn),
                adressebeskyttelse,
                oppholdsadresse,
                listOf(dødsfall),
                listOf(fødselsdato),
            )
        val ident = HentIdent(PERSON_ID, false, Identgruppe.FOLKEREGISTERIDENT.name)
        val aktoerId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        val hentIdenter = HentIdenter(listOf(ident, aktoerId))
        return PersonResponse(hentPerson, hentIdenter)
    }

    companion object {
        private const val AKTOR_ID = "423424"
        private val PERSON_ID = genererFødselsnummer()
        private const val PERSON_NPR_ID = "555534343"
        private val FIRSTNAME = "Fornavn"
        private val LASTNAME = "Etternavn"
        private val MIDDLENAME = "Mellomnavn"
    }

    private fun oppretteNavn(kilde: Kilde = Kilde.FREG, registrert: LocalDateTime = LocalDateTime.now()): Navn = Navn(
        "Riverstone",
        "Glacier",
        "Nielsen",
        metadata = PdlMetadata(
            master = kilde.name,
            endringer = setOf(
                Endring(registrert = registrert, type = Endringstype.OPPRETT),
            ),
        ),
        folkeregistermetadata = when (kilde) {
            Kilde.FREG -> Folkeregistermetadata(ajourholdstidspunkt = registrert)
            Kilde.PDL -> null
        },
    )
}
