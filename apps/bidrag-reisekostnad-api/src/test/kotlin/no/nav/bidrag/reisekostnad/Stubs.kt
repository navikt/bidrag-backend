package no.nav.bidrag.reisekostnad

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import java.time.LocalDate

fun getBidragDokumentRequestPatternBuilder(forespørselId: Int?): RequestPatternBuilder? {
    val verify = WireMock.postRequestedFor(
        WireMock.urlEqualTo("/bidrag-dokument/journalpost/JOARK"),
    )
    verify.withRequestBody(ContainsPattern(String.format("\"referanseId\":\"REISEKOSTNAD_%s\"", forespørselId)))
    return verify
}

fun verifiserDokumentArkivertForForespørsel(forespørselId: Int?) {
    WireMock.verify(getBidragDokumentRequestPatternBuilder(forespørselId))
}

fun verifiserDokumentArkivertForForespørselAntallGanger(antallGanger: Int) {
    WireMock.verify(
        antallGanger,
        WireMock.postRequestedFor(
            WireMock.urlEqualTo("/bidrag-dokument/journalpost/JOARK"),
        ),
    )
}

fun verifiserDokumentIkkeArkivertForForespørsel(forespørselId: Int?) {
    WireMock.verify(0, getBidragDokumentRequestPatternBuilder(forespørselId))
}

// Testpersoner som ikke inngår i den delte Testperson-klassen, men som brukes i WireMock-fixturene under wiremock-maler/.
// Identene genereres på samme måte som Testperson sine, slik at ingen fnr-lignende verdier er hardkodet i JSON-filer.
val RÅTASS_IDENT: String = genererFødselsnummer(LocalDate.now().minusYears(40), null)
val STORSTEIN_IDENT: String = genererFødselsnummer(LocalDate.now().minusYears(13), null)
val MYNDIG_IDENT: String = genererFødselsnummer(LocalDate.now().minusYears(18), null)
val MELLOMSTEIN_IDENT: String = genererFødselsnummer(LocalDate.now().minusYears(15), null)
val MIDDELSTEIN_IDENT: String = genererFødselsnummer(LocalDate.now().minusYears(15).minusDays(1), null)

// Kobler placeholder-tokenene brukt i wiremock-maler/*.json til de faktiske (genererte) identene.
private val identtokenTilVerdi: Map<String, String> = mapOf(
    "GRÅTASS_IDENT" to Testperson.testpersonGråtass.ident,
    "STRENG_IDENT" to Testperson.testpersonStreng.ident,
    "SMÅSTEIN_IDENT" to Testperson.testpersonBarn10.ident,
    "GRUS_IDENT" to Testperson.testpersonBarn16.ident,
    "UTENFOR_IDENT" to Testperson.testpersonIkkeFunnet.ident,
    "DISKOS_IDENT" to Testperson.testpersonHarDiskresjon.ident,
    "TORDIVEL_IDENT" to Testperson.testpersonHarMotpartMedDiskresjon.ident,
    "KAKTUS_IDENT" to Testperson.testpersonHarBarnMedDiskresjon.ident,
    "STEINDØD_IDENT" to Testperson.testpersonErDød.ident,
    "ALBUESKJELL_IDENT" to Testperson.testpersonHarDødtBarn.ident,
    "BUNKERS_IDENT" to Testperson.testpersonDødMotpart.ident,
    "FEIL_IDENT" to Testperson.testpersonServerfeil.ident,
    "RÅTASS_IDENT" to RÅTASS_IDENT,
    "STORSTEIN_IDENT" to STORSTEIN_IDENT,
    "MYNDIG_IDENT" to MYNDIG_IDENT,
    "MELLOMSTEIN_IDENT" to MELLOMSTEIN_IDENT,
    "MIDDELSTEIN_IDENT" to MIDDELSTEIN_IDENT,
)

private val personstubmaler: List<String> = listOf(
    "bidrag-person-motpart-500.json",
    "bidrag-person-motpart-person-ikke-funnet.json",
    "bidrag-person-relasjon-gråtass.json",
    "bidrag-person-relasjon-råtass.json",
    "bidrag-person-relasjon-streng.json",
    "diskresjon/bidrag-person-motpart-29år-har-diskresjon.json",
    "diskresjon/bidrag-person-motpart-44år-motpart-har-diskresjon.json",
    "diskresjon/bidrag-person-motpart-48år-barn-har-diskresjon.json",
    "død/bidrag-person-motpart-35år-er-død.json",
    "død/bidrag-person-motpart-41år-død-motpart.json",
    "død/bidrag-person-motpart-53år-dødt-barn.json",
    "personinfo/bidrag-person-info-barn-grus-16år.json",
    "personinfo/bidrag-person-info-barn-mellomstein-15år.json",
    "personinfo/bidrag-person-info-barn-middelstein-15år.json",
    "personinfo/bidrag-person-info-barn-myndig-18år.json",
    "personinfo/bidrag-person-info-barn-småstein-10år.json",
    "personinfo/bidrag-person-info-barn-storstein-13år.json",
    "personinfo/bidrag-person-info-gråtass-40år.json",
    "personinfo/bidrag-person-info-råtass-40år.json",
    "personinfo/bidrag-person-info-streng-38år.json",
)

private fun lesOgErstattIdenterITestfil(filsti: String): String {
    val ressurs = object {}.javaClass.getResourceAsStream("/wiremock-maler/$filsti")
        ?: throw IllegalStateException("Fant ikke wiremock-mal på classpath: wiremock-maler/$filsti")
    var innhold = ressurs.bufferedReader(Charsets.UTF_8).use { it.readText() }
    identtokenTilVerdi.forEach { (token, verdi) -> innhold = innhold.replace(token, verdi) }
    return innhold
}

// Registrerer alle statiske person-/relasjonsstubber for WireMock, med identer generert av
// genererFødselsnummer (via Testperson og de øvrige testpersonene over) satt inn i stedet for
// placeholder-tokenene i wiremock-maler-filene.
fun registrerAllePersonstubber(wireMockServer: WireMockServer) {
    personstubmaler.forEach { filsti ->
        wireMockServer.addStubMapping(StubMapping.buildFrom(lesOgErstattIdenterITestfil(filsti)))
    }
}
