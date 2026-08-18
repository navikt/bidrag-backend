package no.nav.bidrag.aktoerregister.service

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.bidrag.aktoerregister.dto.enumer.Hendelsestype
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.entities.Dødsbo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AktørendringstrackerTest {

    private val tracker = Aktørendringstracker()

    @Test
    fun `skal ikke sette noe om alt er likt`() {
        val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name)
        val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name)

        val endringer = tracker.utledEndringer(aktør, nyAktør)

        endringer shouldHaveSize 0
    }

    @Nested
    inner class Kontonummer {

        @Test
        fun `skal sette kontonummerOppdatering norskKontonr`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, norskKontonr = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, norskKontonr = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering iban`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, iban = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, iban = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering swift`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, swift = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, swift = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering bankNavn`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, bankNavn = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, bankNavn = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering bankLandkode`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, bankLandkode = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, bankLandkode = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering bankCode`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, bankCode = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, bankCode = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering valutaKode`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, valutaKode = null)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, valutaKode = "12345")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }
    }

    @Nested
    inner class Ident {

        @Test
        fun `skal sette identOppdatering`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name)
            val nyAktør = Aktør(aktørIdent = "1234", aktørType = Identtype.PERSONNUMMER.name)

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.IDENT_OPPDATERING
        }
    }

    @Nested
    inner class Navn {

        @Test
        fun `skal sette navnOppdatering fornavn`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, fornavn = "Per")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, fornavn = "Emil")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.NAVN_OPPDATERING
        }

        @Test
        fun `skal sette navnOppdatering etternavn`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, etternavn = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, etternavn = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.NAVN_OPPDATERING
        }
    }

    @Nested
    inner class Adresse {

        @Test
        fun `skal sette adresseOppdatering adresselinje1`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje1 = "Adress")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje1 = "Adressen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering adresselinje2`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje2 = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje2 = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering adresselinje3`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje3 = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje3 = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering leilighetsnummer`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, leilighetsnummer = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, leilighetsnummer = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering postnr`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, postnr = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, postnr = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering poststed`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, poststed = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, poststed = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering land`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, land = "Persen")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, land = "Emilsen")

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }
    }

    @Nested
    inner class Dato {

        @Test
        fun `skal sette fødtDatoOppdatering`() {
            val fødtDato = LocalDate.now()
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, fødtDato = fødtDato)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, fødtDato = fødtDato.minusMonths(1))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.FODT_DATO_OPPDATERING
        }

        @Test
        fun `skal sette dødDatoOppdatering`() {
            val dødDato = LocalDate.now()
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødDato = dødDato)
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødDato = dødDato.minusMonths(1))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DOD_DATO_OPPDATERING
        }
    }

    @Nested
    inner class Gradering {

        @Test
        fun `skal sette graderingOppdatering`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, gradering = "Super streng")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, gradering = null)

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.GRADERING_OPPDATERING
        }
    }

    @Nested
    inner class Språk {

        @Test
        fun `skal sette språkOppdatering`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, språkkode = "NB")
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, språkkode = null)

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.SPRAK_OPPDATERING
        }
    }

    @Nested
    inner class DødsboOppdatering {

        @Test
        fun `skal sette dødsboOppdatering kontaktperson`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(kontaktperson = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(kontaktperson = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering adresselinje1`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(adresselinje1 = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(adresselinje1 = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering adresselinje2`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(adresselinje2 = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(adresselinje2 = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering adresselinje3`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(adresselinje3 = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(adresselinje3 = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering leilighetsnummer`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(leilighetsnummer = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(leilighetsnummer = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering postnr`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(postnr = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(postnr = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering poststed`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(poststed = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(poststed = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering land`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(land = "Person"))
            val nyAktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødsbo = Dødsbo(land = "Person2"))

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 1
            endringer shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal ikke sette dødsboOppdatering når ingen endring`() {
            val aktør = Aktør(
                aktørIdent = "123",
                aktørType = Identtype.PERSONNUMMER.name,
                dødsbo = Dødsbo(
                    kontaktperson = "Person",
                    adresselinje1 = "Adresse",
                    poststed = "Poststed",
                    postnr = "Postnr",
                    land = "Person",
                ),
            )
            val nyAktør = Aktør(
                aktørIdent = "123",
                aktørType = Identtype.PERSONNUMMER.name,
                dødsbo = Dødsbo(
                    kontaktperson = "Person",
                    adresselinje1 = "Adresse",
                    poststed = "Poststed",
                    postnr = "Postnr",
                    land = "Person",
                ),
            )

            val endringer = tracker.utledEndringer(aktør, nyAktør)

            endringer shouldHaveSize 0
        }
    }

    @Nested
    inner class TrackNyAktør {

        @Test
        fun `skal alltid sette identOppdatering for ny aktør`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name)

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
        }

        @Test
        fun `skal sette navnOppdatering når fornavn finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, fornavn = "Per")

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.NAVN_OPPDATERING
        }

        @Test
        fun `skal sette kontonummerOppdatering når kontonummer finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, norskKontonr = "12345")

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.KONTONUMMER_OPPDATERING
        }

        @Test
        fun `skal sette adresseOppdatering når adresse finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, adresselinje1 = "Gate 1")

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.ADRESSE_OPPDATERING
        }

        @Test
        fun `skal sette fødtDatoOppdatering når fødtDato finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, fødtDato = LocalDate.now())

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.FODT_DATO_OPPDATERING
        }

        @Test
        fun `skal sette dødDatoOppdatering når dødDato finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, dødDato = LocalDate.now())

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.DOD_DATO_OPPDATERING
        }

        @Test
        fun `skal sette graderingOppdatering når gradering finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, gradering = "STRENGT_FORTROLIG")

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.GRADERING_OPPDATERING
        }

        @Test
        fun `skal sette dødsboOppdatering når dødsbo finnes`() {
            val aktør = Aktør(
                aktørIdent = "123",
                aktørType = Identtype.PERSONNUMMER.name,
                dødsbo = Dødsbo(kontaktperson = "Person"),
            )

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.DODSBO_OPPDATERING
        }

        @Test
        fun `skal sette språkOppdatering når språkkode finnes`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name, språkkode = "NB")

            val felter = tracker.trackNyAktør(aktør)

            felter shouldContain Hendelsestype.IDENT_OPPDATERING
            felter shouldContain Hendelsestype.SPRAK_OPPDATERING
        }

        @Test
        fun `skal kun sette identOppdatering for minimal aktør`() {
            val aktør = Aktør(aktørIdent = "123", aktørType = Identtype.PERSONNUMMER.name)

            val felter = tracker.trackNyAktør(aktør)

            felter shouldHaveSize 1
            felter shouldContain Hendelsestype.IDENT_OPPDATERING
        }
    }
}
