package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.objektnummer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolle
import no.nav.bidrag.admin.produksjonsoppfølging.jira.JiraService
import no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.Oppfølging
import no.nav.bidrag.admin.produksjonsoppfølging.repository.RolleRepository
import no.nav.bidrag.admin.produksjonsoppfølging.utils.Entities
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.math.max

private val LOGGER = KotlinLogging.logger { }

@Service
class ObjektnummerService(
    private val objektnummerRepository: ObjektnummerRepository,
    private val rolleRepository: RolleRepository,
    private val jiraService: JiraService,
) : Oppfølging {
    override fun folgOpp() {
        objektnummerRepository
            .finnSakerMedGjenbrukteObjektnummer()
            ?.forEach { lagPatchForSak(it!!) }
    }

    private fun lagPatchForSak(saksnummer: String) {
        val jiraSummary = "Ugyldige objektnr i sak $saksnummer"
        val eksisterendeSaker = jiraService.finnFagsystemsaker(jiraSummary)

        if (eksisterendeSaker.isNotEmpty()) {
            LOGGER.info { "Sak $saksnummer har allerede oppfølgingssak: ${eksisterendeSaker.joinToString()}" }
            return
        }
        LOGGER.info { "Lager patch for sak $saksnummer" }

        val roller =
            Entities(rolleRepository.findBySaksnr(saksnummer), Rolle::id)
                .sorter(
                    compareBy<Rolle, String?>(nullsLast(naturalOrder())) { it.objektnr }
                        .thenBy(nullsFirst(naturalOrder())) { it.fnr }
                        .thenBy { it.id },
                )

        val barnListe = roller.subset(Rolle::erBarn)

        val barnTilOppføging = HashSet<String>()
        val objektnummerMapping = HashMap<Int, String>()

        barnListe.forEach {
            val objektnummerBruk = objektnummerMapping[it.objektnrSomInt()]
            if (objektnummerBruk != null && objektnummerBruk != it.fnr) {
                barnTilOppføging.add(it.fnr!!)
                barnTilOppføging.add(objektnummerBruk)
            }
            objektnummerMapping[it.objektnrSomInt()] = it.fnr!!
        }

        val beskrivelse =
            buildString {
                append("Roller i sak $saksnummer:\r\n")
                append("||Id||Type||FNR||Objektnr||Ukjent kode||RM||Født dato||Opprettet dato||\r\n")
                roller.forEach { rolle ->
                    append(
                        "|${rolle.id}|${rolle.type}|${rolle.fnr}|${rolle.objektnr ?: " "}|" +
                            "${rolle.ukjentPartKode ?: " "}|${rolle.rmRolleId ?: " "}|" +
                            "${rolle.fodtDato}|${rolle.opprettetDato}|\r\n",
                    )
                }
            }

        val out = ByteArrayOutputStream()
        val patch = PrintStream(out)

        patch.println("Foreslår følgende patch:")
        patch.println("{code:sql}")
        patch.println("SET SCHEMA BI464P\r\n")

        var ledigObjektnr: Int =
            max(
                3,
                roller
                    .stream()
                    .mapToInt(Rolle::objektnrSomInt)
                    .max()
                    .orElse(0) + 1,
            )
        for (barnFnr in barnTilOppføging) {
            barnListe.withProperty(Rolle::fnr) { it == barnFnr }

            val nyttObjektnr: String? = objektnummer(ledigObjektnr++)

            patch.println("-- Oppdaterer objektnr for barn med fnr $barnFnr")
            patch.println(
                (
                    "" +
                        "UPDATE t_rolle \r\n" +
                        "  SET objektnr='" +
                        nyttObjektnr +
                        "'\r\n" +
                        "  WHERE saksnr='" +
                        saksnummer +
                        "'\r\n" +
                        "    AND rolle_type='BA'\r\n" +
                        "    AND fnr='" +
                        barnFnr +
                        "';\r\n"
                    ),
            )
        }
        patch.println("{code}")
        patch.close()

        val issue = jiraService.opprettFagsystemsak(jiraSummary, beskrivelse, null)
        LOGGER.info { "Oppretter $issue for sak $saksnummer" }
        jiraService.leggInnKommentar(issue, out.toString())
    }

    fun objektnummer(objektnummerSomInt: Int): String? {
        if (objektnummerSomInt <= 0) {
            return null
        } else {
            var nr = objektnummerSomInt.toString()
            if (nr.length <= 1) {
                nr = "0$nr"
            }
            return nr
        }
    }
}
