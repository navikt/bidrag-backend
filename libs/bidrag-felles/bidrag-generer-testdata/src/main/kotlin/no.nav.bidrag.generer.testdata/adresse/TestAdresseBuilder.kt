package no.nav.bidrag.generer.testdata.adresse

import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.diverse.LandkoderIso3
import kotlin.random.Random

@Suppress("unused")
class TestAdresseBuilder {
    private var adresseformat: Adresseformat? = null
    private var adresselinje1: String? = null
    private var adresselinje2: String? = null
    private var adresselinje3: String? = null
    private var postnummer: String? = null
    private var poststed: String? = null
    private var bolignummer: String? = null
    private var land: LandkoderIso3? = LandkoderIso3.NOR

    fun format(adresseformat: Adresseformat?): TestAdresseBuilder {
        this.adresseformat = adresseformat
        return this
    }

    fun adresselinje1(adresselinje1: String?): TestAdresseBuilder {
        this.adresselinje1 = adresselinje1
        return this
    }

    fun adresselinje2(adresselinje2: String?): TestAdresseBuilder {
        this.adresselinje2 = adresselinje2
        return this
    }

    fun adresselinje3(adresselinje3: String?): TestAdresseBuilder {
        this.adresselinje3 = adresselinje3
        return this
    }

    fun bolignummer(bolignummer: String?): TestAdresseBuilder {
        this.bolignummer = bolignummer
        return this
    }

    fun postnummerOgSted(
        postnummer: String?,
        poststed: String?,
    ): TestAdresseBuilder {
        this.postnummer = postnummer
        this.poststed = poststed
        return this
    }

    fun land(land: LandkoderIso3?): TestAdresseBuilder {
        this.land = land
        return this
    }

    fun tilknytning(type: Adressetype?): AdressetilknytningBuilder = AdressetilknytningBuilder(null, this, type)

    fun somBosted(): AdressetilknytningBuilder = AdressetilknytningBuilder(null, this, Adressetype.BOSTEDSADRESSE)

    fun somOppholdsadresse(): AdressetilknytningBuilder = AdressetilknytningBuilder(null, this, Adressetype.OPPHOLDSADRESSE)

    fun somKontaktadresse(): AdressetilknytningBuilder = AdressetilknytningBuilder(null, this, Adressetype.KONTAKTADRESSE)

    fun opprett(): TestAdresse {
        var adresselinje1 = this.adresselinje1
        var adresselinje2 = this.adresselinje2
        var postnummer = this.postnummer
        var poststed = this.poststed

        if (adresselinje1 == null) {
            adresselinje1 =
                if (this.isNorskAdresse) {
                    genererEnkelAdresse()
                } else {
                    Random.nextLong(2000).toString() + " Weebfoot Street"
                }
        }

        if (adresselinje2 == null && postnummer == null && poststed == null) {
            if (this.isNorskAdresse) {
                val postSted = genererPoststed()
                postnummer = postSted.postnummer
                poststed = postSted.poststed
            } else {
                adresselinje2 = "Duckburg, Calisota"
            }
        }

        return TestAdresse(
            adresseformat,
            adresselinje1,
            adresselinje2,
            adresselinje3,
            postnummer,
            poststed,
            bolignummer,
            land,
        )
    }

    private val isNorskAdresse: Boolean
        get() =
            LandkoderIso3.NOR == land &&
                (Adresseformat.UTENLANDSK_ADRESSE != adresseformat) && (Adresseformat.UTENLANDSK_ADRESSE_I_FRITT_FORMAT != adresseformat)
}

fun genererAdresse(): TestAdresseBuilder = TestAdresseBuilder()
