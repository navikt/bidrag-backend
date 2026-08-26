package no.nav.bidrag.generer.testdata.samhandler

import no.nav.bidrag.domene.enums.samhandler.OffentligIdType
import no.nav.bidrag.domene.enums.samhandler.Områdekode
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.generer.testdata.konto.TestKontonummer

@Suppress("unused")
data class TestSamhandler(
    val samhandlerId: SamhandlerId? = null,
    val navn: String? = null,
    val offentligId: String? = null,
    val offentligIdType: OffentligIdType? = null,
    val områdekode: Områdekode? = null,
    val språk: String? = null,
    val adresse: String? = null,
    val kontonummer: TestKontonummer? = null,
    val kontaktperson: String? = null,
    val kontaktEpost: String? = null,
    val kontaktTelefon: String? = null,
    val notat: String? = null,
    val erOpphørt: Boolean? = null,
)
