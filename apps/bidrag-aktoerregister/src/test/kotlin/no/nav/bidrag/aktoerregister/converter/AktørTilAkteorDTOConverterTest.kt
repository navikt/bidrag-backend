package no.nav.bidrag.aktoerregister.converter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.aktoerregister.dto.enumer.Gradering
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.entities.Dødsbo
import no.nav.bidrag.aktoerregister.persistence.entities.TidligereIdenter
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AktørTilAkteorDTOConverterTest {
    private val aktoerTilAktoerDTOConverter = AktørTilAktoerDTOConverter()

    @Test
    fun skalKonverteSelvOmFlesteFelterErNull() {
        val aktør = Aktør(aktørIdent = genererFødselsnummer(), aktørType = "PERSONNUMMER")
        val aktoerDTO = aktoerTilAktoerDTOConverter.convert(aktør)
        AssertionsForClassTypes.assertThat(aktoerDTO).isNotNull
    }

    @Test
    fun skalKonvertereAktoerTilAktoerDTO() {
        val aktoerIdent = genererFødselsnummer()
        val tidligereAktoerIdent = genererFødselsnummer()
        val aktoerType = Identtype.PERSONNUMMER
        val offentligId = "6"
        val offentligType = "OffentligType"
        val norskKontonr = "123456789"
        val iban = "12345"
        val swift = "54321"
        val bankNavn = "Bank of Navn"
        val bankCode = "1899"
        val bankLandkode = "NOR"
        val valutaKode = "NOK"
        val etternavn = "Etternavn"
        val fornavn = "Fornavn"
        val gradering = Gradering.FORTROLIG
        val sprakkode = "NOR"
        val foddato = LocalDate.now().minusYears(30)
        val doddato = LocalDate.now()
        val kontaktperson = "Kontakt"
        val adresselinje1 = "Test gate 10"
        val adresselinje2 = "Test gate 20"
        val adresselinje3 = "Test gate 30"
        val leilighetsnummer = "100"
        val postnr = "0001"
        val poststed = "Oslo"
        val aktør = Aktør(
            aktørIdent = aktoerIdent,
            aktørType = aktoerType.name,
            offentligId = offentligId,
            offentligIdType = offentligType,
            norskKontonr = norskKontonr,
            iban = iban,
            swift = swift,
            bankNavn = bankNavn,
            bankCode = bankCode,
            bankLandkode = bankLandkode,
            valutaKode = valutaKode,
            etternavn = etternavn,
            fornavn = fornavn,
            tidligereIdenter = mutableSetOf(TidligereIdenter(tidligereAktoerIdent = tidligereAktoerIdent, identtype = aktoerType.name)),
            gradering = gradering.name,
            språkkode = sprakkode,
            fødtDato = foddato,
            dødDato = doddato,
            dødsbo = Dødsbo(kontaktperson = kontaktperson, adresselinje1 = adresselinje1),
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            adresselinje3 = adresselinje3,
            leilighetsnummer = leilighetsnummer,
            postnr = postnr,
            poststed = poststed,
        )

        val aktoerDTO = aktoerTilAktoerDTOConverter.convert(aktør)

        aktoerDTO shouldNotBe null
        aktoerDTO.aktoerId shouldNotBe null
        aktoerDTO.aktoerId.aktoerId shouldBe aktoerIdent
        aktoerDTO.aktoerId.identtype shouldBe aktoerType
        aktoerDTO.offentligId shouldBe offentligId
        aktoerDTO.offentligIdType shouldBe offentligType
        aktoerDTO.kontonummer?.norskKontonr shouldBe norskKontonr
        aktoerDTO.kontonummer?.iban shouldBe iban
        aktoerDTO.kontonummer?.swift shouldBe swift
        aktoerDTO.kontonummer?.bankNavn shouldBe bankNavn
        aktoerDTO.kontonummer?.bankCode shouldBe bankCode
        aktoerDTO.kontonummer?.bankLandkode shouldBe bankLandkode
        aktoerDTO.kontonummer?.valutaKode shouldBe valutaKode
        aktoerDTO.navn?.etternavn shouldBe etternavn
        aktoerDTO.adresse?.adresselinje1 shouldBe adresselinje1
        aktoerDTO.adresse?.adresselinje2 shouldBe adresselinje2
        aktoerDTO.adresse?.adresselinje3 shouldBe adresselinje3
        aktoerDTO.adresse?.leilighetsnummer shouldBe leilighetsnummer
        aktoerDTO.adresse?.postnr shouldBe postnr
        aktoerDTO.adresse?.poststed shouldBe poststed
        aktoerDTO.navn?.etternavn shouldBe etternavn
        aktoerDTO.navn?.fornavn shouldBe fornavn
        aktoerDTO.sprakkode shouldBe sprakkode
        aktoerDTO.gradering shouldBe gradering
        aktoerDTO.fodtDato shouldBe foddato.toString()
        aktoerDTO.dodDato shouldBe doddato.toString()
        aktoerDTO.tidligereIdenter!![0].aktoerId shouldBe tidligereAktoerIdent
        aktoerDTO.tidligereIdenter!![0].identtype shouldBe aktoerType
        aktoerDTO.dodsbo?.kontaktperson shouldBe kontaktperson
        aktoerDTO.dodsbo?.adresse?.adresselinje1 shouldBe adresselinje1
    }
}
