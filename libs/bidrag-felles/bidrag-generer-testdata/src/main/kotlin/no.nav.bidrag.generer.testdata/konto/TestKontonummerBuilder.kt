package no.nav.bidrag.generer.testdata.konto

import no.nav.bidrag.domene.enums.diverse.LandkoderIso3
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import kotlin.random.Random

@Suppress("unused")
class TestKontonummerBuilder {
    private var bankNummer: Int? = null
    private var kontotype: Int? = null
    private var norskKontonummer: Boolean = true
    private var iban: String? = null
    private var swift: String? = null
    private var banknavn: String? = null
    private var landkodeBank: LandkoderIso3? = null
    private var bankCode: String? = null
    private var valutakode: Valutakode? = null

    fun bankNummer(bankNummer: Int): TestKontonummerBuilder {
        this.bankNummer = bankNummer
        return this
    }

    fun kontotype(kontotype: Int): TestKontonummerBuilder {
        this.kontotype = kontotype
        return this
    }

    fun norskKontonummer(norskKontonummer: Boolean): TestKontonummerBuilder {
        this.norskKontonummer = norskKontonummer
        return this
    }

    fun iban(iban: String?): TestKontonummerBuilder {
        this.iban = iban
        return this
    }

    fun swift(swift: String?): TestKontonummerBuilder {
        this.swift = swift
        return this
    }

    fun banknavn(banknavn: String?): TestKontonummerBuilder {
        this.banknavn = banknavn
        return this
    }

    fun landkodeBank(landkodeBank: LandkoderIso3?): TestKontonummerBuilder {
        this.landkodeBank = landkodeBank
        return this
    }

    fun bankCode(bankCode: String?): TestKontonummerBuilder {
        this.bankCode = bankCode
        return this
    }

    fun valutakode(valutakode: Valutakode?): TestKontonummerBuilder {
        this.valutakode = valutakode
        return this
    }

    fun opprett(): TestKontonummer =
        if (norskKontonummer) {
            TestKontonummer(
                norskKontonummer =
                    NorskKontonummerBuilder()
                        .bankNummer(bankNummer ?: Random.nextInt(10000))
                        .kontotype(kontotype ?: Random.nextInt(100))
                        .randomKundenummer()
                        .toString(),
            )
        } else {
            val landkodeBank = landkodeBank ?: genererLandkodeIso3()
            val bankCode = bankCode ?: genererBankcode()
            TestKontonummer(
                iban = iban ?: genererIban(landkodeBank, bankCode),
                swift = swift ?: genererSwift(landkodeBank, bankCode),
                bankNavn = banknavn ?: "BANK OF ${landkodeBank.visningsnavn}",
                landkodeBank = landkodeBank,
                bankCode = bankCode,
                valutakode = valutakode ?: genererValutakode(),
            )
        }
}

fun genererKontonummer(): TestKontonummerBuilder = TestKontonummerBuilder()

fun genererLandkodeIso3(): LandkoderIso3 = LandkoderIso3.entries.random()

fun genererValutakode(): Valutakode = Valutakode.entries.random()

fun genererIban(
    landkode: LandkoderIso3? = LandkoderIso3.entries.random(),
    bankCode: String? = genererBankcode(),
): String {
    // Simplifisert iban generator. Lager ikke en gyldig checksum.
    val landkodeIso2 = landkode?.visningsnavn?.take(2)
    val kontrollsiffer = Random.nextInt(10, 100)
    val bankCode = bankCode
    val kontonummerDel1 = (1..4).map { Random.nextInt(0, 10) }.joinToString("")
    val kontonummerDel2 = (1..3).map { Random.nextInt(0, 10) }.joinToString("")
    return "$landkodeIso2 $kontrollsiffer $bankCode $kontonummerDel1 $kontonummerDel2"
}

fun genererBankcode(): String = (1..4).map { ('A'..'Z').random() }.joinToString("")

fun genererSwift(
    landkode: LandkoderIso3? = LandkoderIso3.entries.random(),
    bankCode: String? = genererBankcode(),
): String {
    val landkodeIso2 = landkode?.visningsnavn?.take(2)
    val chars = ('A'..'Z') + ('0'..'9')
    val lokasjonskode = (1..2).map { chars.random() }.joinToString("")
    val avdelingskode = if (Random.nextBoolean()) "XXX" else ""

    return "$bankCode$landkodeIso2$lokasjonskode$avdelingskode"
}
