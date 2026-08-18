package no.nav.bidrag.person.dto

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.person.query.Adressebeskyttelse
import no.nav.bidrag.person.query.GeografiskTilknytningResponse
import no.nav.bidrag.person.query.HentGeografiskTilknytning
import no.nav.bidrag.person.query.HentPersonDiskresjon
import no.nav.bidrag.person.query.OppholdAnnetSted
import no.nav.bidrag.person.query.OppholdsadresseCommon
import no.nav.bidrag.person.query.PersonResponse.HentIdent
import no.nav.bidrag.person.query.PersonResponse.HentIdenter
import no.nav.bidrag.transport.person.Identgruppe
import org.junit.jupiter.api.Test

class PersonGeografiskQueryTest {
    @Test
    fun `skal mappe PDL respons til geografisktilknytndto`() {
        val adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG)
        val oppholdsadresse = OppholdsadresseCommon(OppholdAnnetSted.MILITAER)
        val hentPerson = HentPersonDiskresjon(listOf(adressebeskyttelse), listOf(oppholdsadresse))
        val hentIdenter = createHentIdenterResponse()
        val personGeoResponse =
            GeografiskTilknytningResponse(createGeografiskTilknytningResponse(), hentPerson, hentIdenter)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()

        tilknytningDto.ident shouldBe PERSON_ID
        tilknytningDto.aktørId shouldBe AKTOR_ID
        tilknytningDto.geografiskTilknytning shouldBe GT_KOMMUNE
        tilknytningDto.diskresjonskode shouldBe Diskresjonskode.SPSF
    }

    @Test
    fun `skal mappe PDL respons med adressebeskyttelse STRENGT_FORTROLIG_UTLAND til Diskresjon`() {
        val adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND)
        val oppholdsadresse = OppholdsadresseCommon(OppholdAnnetSted.MILITAER)
        val hentPerson = HentPersonDiskresjon(listOf(adressebeskyttelse), listOf(oppholdsadresse))
        val hentIdenter = createHentIdenterResponse()
        val personGeoResponse =
            GeografiskTilknytningResponse(createGeografiskTilknytningResponse(), hentPerson, hentIdenter)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()
        tilknytningDto.diskresjonskode shouldBe Diskresjonskode.P19
    }

    @Test
    fun `skal mappe PDL respons med adressebeskyttelse FORTROLIG til Diskresjon`() {
        val personGeoResponse = createPersonGeoResponseWith(Gradering.FORTROLIG, OppholdAnnetSted.MILITAER)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()
        tilknytningDto.diskresjonskode shouldBe Diskresjonskode.SPFO
    }

    @Test
    fun `skal mappe PDL respons med adressebeskyttelse UGRADERT til Diskresjon`() {
        val personGeoResponse = createPersonGeoResponseWith(Gradering.UGRADERT, OppholdAnnetSted.MILITAER)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()
        tilknytningDto.diskresjonskode shouldBe Diskresjonskode.MILI
    }

    @Test
    fun `skal mappe PDL respons med oppholdAnnetSted PENDLER til Diskresjon`() {
        val personGeoResponse = createPersonGeoResponseWith(null, OppholdAnnetSted.PENDLER)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()
        tilknytningDto.diskresjonskode shouldBe Diskresjonskode.PEND
    }

    @Test
    fun `skal mappe PDL respons med ingen diskresjon til null Diskresjon`() {
        val personGeoResponse = createPersonGeoResponseWith(null, null)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()
        tilknytningDto.diskresjonskode shouldBe null
    }

    @Test
    fun `skal mappe PDL respons med gt BYDEL`() {
        val personGeoResponse = createPersonGeoResponseWith(null, OppholdAnnetSted.PENDLER)
        personGeoResponse.hentGeografiskTilknytning = HentGeografiskTilknytning(null, GT_BYDEL, "BYDEL", null)
        val tilknytningDto = personGeoResponse.mapToGeografiskTilknytningDto()
        tilknytningDto.geografiskTilknytning shouldBe GT_BYDEL
    }

    private fun createPersonGeoResponseWith(gradering: Gradering?, oppholdAnnetSted: OppholdAnnetSted?): GeografiskTilknytningResponse {
        val adressebeskyttelse = if (gradering == null) listOf() else listOf(Adressebeskyttelse(gradering))
        val oppholdsadresse =
            if (oppholdAnnetSted == null) listOf() else listOf(OppholdsadresseCommon(oppholdAnnetSted))
        val hentPerson = HentPersonDiskresjon(adressebeskyttelse, oppholdsadresse)
        val hentIdenter = createHentIdenterResponse()
        return GeografiskTilknytningResponse(createGeografiskTilknytningResponse(), hentPerson, hentIdenter)
    }

    private fun createGeografiskTilknytningResponse(): HentGeografiskTilknytning = HentGeografiskTilknytning(GT_KOMMUNE, null, "KOMMUNE", null)

    private fun createHentIdenterResponse(): HentIdenter {
        val ident = HentIdent(PERSON_ID.verdi, false, Identgruppe.NPID.name)
        val aktoerId = HentIdent(AKTOR_ID, false, Identgruppe.AKTORID.name)
        return HentIdenter(listOf(ident, aktoerId))
    }

    companion object {
        private val PERSON_ID = Personident(genererFødselsnummer())
        private val AKTOR_ID = "423424"
        private val GT_KOMMUNE = "3005"
        private val GT_BYDEL = "300512"
    }
}
