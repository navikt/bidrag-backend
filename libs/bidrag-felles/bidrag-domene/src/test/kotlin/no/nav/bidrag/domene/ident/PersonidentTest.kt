package no.nav.bidrag.domene.ident

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PersonidentTest {
    @Test
    fun `skal anonmisere personIdent`() {
        Personident("15507600333").toString() shouldBe "*5*0*6*0*3*"
        Personident("29422059278").toString() shouldBe "*9*2*0*9*7*"
        Personident("07420450952").toString() shouldBe "*7*2*4*0*5*"

        Personident("15507600333").toString() shouldBe "*5*0*6*0*3*"
        Personident("29422059278").toString() shouldBe "*9*2*0*9*7*"
    }

    @Test
    fun `skal tillate helsyntetiske nummer fra dolly`() {
        val listeAvBrukere =
            listOf(
                SyntetiskBruker("15507600333", "55507608360", "Mann", LocalDate.of(1976, 10, 15)),
                SyntetiskBruker("29422059278", "69422056629", "Kvinne", LocalDate.of(2020, 2, 29)),
                SyntetiskBruker("15507600333", "55507608360", "Mann", LocalDate.of(1976, 10, 15)),
                SyntetiskBruker("29422059278", "69422056629", "Kvinne", LocalDate.of(2020, 2, 29)),
                SyntetiskBruker("05440355678", "45440356293", "Kvinne", LocalDate.of(2003, 4, 5)),
                SyntetiskBruker("12429400544", "52429405181", "Mann", LocalDate.of(1994, 2, 12)),
                SyntetiskBruker("12505209719", "52505209540", "Mann", LocalDate.of(1952, 10, 12)),
                SyntetiskBruker("21483609245", "61483601467", "Kvinne", LocalDate.of(1936, 8, 21)),
                SyntetiskBruker("17912099997", "57912075186", "Mann", LocalDate.of(2020, 11, 17)),
                SyntetiskBruker("29822099635", "69822075096", "Kvinne", LocalDate.of(2020, 2, 29)),
                SyntetiskBruker("05840399895", "45840375084", "Kvinne", LocalDate.of(2003, 4, 5)),
                SyntetiskBruker("12829499914", "52829400197", "Mann", LocalDate.of(1994, 2, 12)),
                SyntetiskBruker("12905299938", "52905200100", "Mann", LocalDate.of(1952, 10, 12)),
                SyntetiskBruker("21883649874", "61883600222", "Kvinne", LocalDate.of(1936, 8, 21)),
            )

        listeAvBrukere.forEach {
            Personident(it.fnr).verdi shouldBe it.fnr
            Personident(it.dnr).verdi shouldBe it.dnr
            Personident(it.fnr).fødselsdato() shouldBe it.fødselsdato
            Personident(it.dnr).fødselsdato() shouldBe it.fødselsdato
            Personident(it.fnr).erDNummer() shouldBe false
            Personident(it.dnr).erDNummer() shouldBe true
        }
    }

    @Test
    fun `skal ikke tolke samhandlere som personidenter`() {
        Personident("80000000000").gyldig() shouldBe false
        Personident("80000000000").erDNummer() shouldBe false
        Personident("80000000000").erSkattSyntetisk() shouldBe false
        Personident("80000000000").erNAVSyntetisk() shouldBe false
        Personident("90000000000").gyldig() shouldBe false
        Personident("90000000000").erDNummer() shouldBe false
        Personident("90000000000").erSkattSyntetisk() shouldBe false
        Personident("90000000000").erNAVSyntetisk() shouldBe false
    }

    data class SyntetiskBruker(
        val fnr: String,
        val dnr: String,
        val kjønn: String,
        val fødselsdato: LocalDate,
    )
}
