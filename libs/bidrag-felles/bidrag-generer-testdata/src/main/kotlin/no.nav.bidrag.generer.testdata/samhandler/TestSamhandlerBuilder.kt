package no.nav.bidrag.generer.testdata.samhandler

import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.generer.testdata.adresse.genererEnkelAdresseMedPoststed
import no.nav.bidrag.generer.testdata.konto.TestKontonummer
import no.nav.bidrag.generer.testdata.konto.genererKontonummer
import no.nav.bidrag.generer.testdata.navn.genererEtternavn
import no.nav.bidrag.generer.testdata.navn.genererFornavn
import kotlin.random.Random

@Suppress("unused")
class TestSamhandlerBuilder {
    private var samhandlerId: SamhandlerId? = null
    private var navn: String? = null
    private var offentligId: String? = null
    private var offentligIdType: OffentligIdType? = null
    private var områdekode: Områdekode? = null
    private var språk: String? = null
    private var adresse: String? = null
    private var kontonummer: TestKontonummer? = null
    private var kontaktperson: String? = null
    private var kontaktEpost: String? = null
    private var kontaktTelefon: String? = null
    private var notat: String? = null
    private var erOpphørt: Boolean? = false

    fun medSamhandlerId(samhandlerId: SamhandlerId): TestSamhandlerBuilder {
        this.samhandlerId = samhandlerId
        return this
    }

    fun medNavn(navn: String): TestSamhandlerBuilder {
        this.navn = navn
        return this
    }

    fun medOffentligId(offentligId: String?): TestSamhandlerBuilder {
        this.offentligId = offentligId
        return this
    }

    fun medOffentligIdType(offentligIdType: OffentligIdType?): TestSamhandlerBuilder {
        this.offentligIdType = offentligIdType
        return this
    }

    fun medOmrådekode(områdekode: Områdekode?): TestSamhandlerBuilder {
        this.områdekode = områdekode
        return this
    }

    fun medSpråk(språk: String?): TestSamhandlerBuilder {
        this.språk = språk
        return this
    }

    fun medAdresse(adresse: String?): TestSamhandlerBuilder {
        this.adresse = adresse
        return this
    }

    fun medKontonummer(kontonummer: TestKontonummer?): TestSamhandlerBuilder {
        this.kontonummer = kontonummer
        return this
    }

    fun medKontaktperson(kontaktperson: String?): TestSamhandlerBuilder {
        this.kontaktperson = kontaktperson
        return this
    }

    fun medKontaktEpost(kontaktEpost: String?): TestSamhandlerBuilder {
        this.kontaktEpost = kontaktEpost
        return this
    }

    fun medKontaktTelefon(kontaktTelefon: String?): TestSamhandlerBuilder {
        this.kontaktTelefon = kontaktTelefon
        return this
    }

    fun medNotat(notat: String?): TestSamhandlerBuilder {
        this.notat = notat
        return this
    }

    fun medErOpphørt(erOpphørt: Boolean?): TestSamhandlerBuilder {
        this.erOpphørt = erOpphørt
        return this
    }

    fun opprett(): TestSamhandler {
        val kontaktFornavn = genererFornavn()
        val kontaktEtternavn = genererEtternavn()
        val testSamhandler =
            TestSamhandler(
                samhandlerId = samhandlerId ?: genererSamhandlerId(),
                navn = navn ?: (genererFornavn() + " " + genererEtternavn()),
                offentligId = offentligId,
                offentligIdType = offentligIdType ?: OffentligIdType.entries.random(),
                områdekode = områdekode ?: Områdekode.entries.random(),
                språk = språk ?: genererSpråk().name,
                adresse = adresse ?: genererEnkelAdresseMedPoststed(),
                kontonummer = kontonummer ?: genererKontonummer().opprett(),
                kontaktperson = kontaktperson ?: ("$kontaktFornavn $kontaktEtternavn"),
                kontaktEpost = kontaktEpost ?: ("$kontaktFornavn@$kontaktEtternavn.no"),
                kontaktTelefon = kontaktTelefon ?: genererTelefonnummer(),
                notat = notat,
                erOpphørt = erOpphørt ?: false,
            )
        return testSamhandler
    }
}

@Suppress("unused")
fun genererSamhandler(): TestSamhandlerBuilder = TestSamhandlerBuilder()

fun genererSamhandlerId(): SamhandlerId {
    val randomSuffix = Random.nextInt(0, 10_000_000)
    return SamhandlerId("85${randomSuffix.toString().padStart(9, '0')}")
}

fun genererSpråk(): Språk = Språk.entries.random()

fun genererTelefonnummer(): String = Random.nextInt(10_000_000, 100_000_000).toString()
