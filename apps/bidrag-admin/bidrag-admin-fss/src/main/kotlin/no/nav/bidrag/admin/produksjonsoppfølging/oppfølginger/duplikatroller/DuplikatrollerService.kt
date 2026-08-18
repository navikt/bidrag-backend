package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.duplikatroller

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.admin.produksjonsoppfølging.domene.Beløp
import no.nav.bidrag.admin.produksjonsoppfølging.domene.Rolle
import no.nav.bidrag.admin.produksjonsoppfølging.domene.Sak
import no.nav.bidrag.admin.produksjonsoppfølging.domene.Søknadslinje
import no.nav.bidrag.admin.produksjonsoppfølging.jira.JiraService
import no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.Oppfølging
import no.nav.bidrag.admin.produksjonsoppfølging.repository.BeløpRepository
import no.nav.bidrag.admin.produksjonsoppfølging.repository.RolleRepository
import no.nav.bidrag.admin.produksjonsoppfølging.repository.SøknadslinjeRepository
import no.nav.bidrag.admin.produksjonsoppfølging.utils.Entities
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.PrintStream

private val LOGGER = KotlinLogging.logger { }

@Service
class DuplikatrollerService(
    private val duplikatrollerRepository: DuplikatrollerRepository,
    private val rolleRepository: RolleRepository,
    private val søknadslinjeRepository: SøknadslinjeRepository,
    private val beløpRepository: BeløpRepository,
    private val jiraService: JiraService,
) : Oppfølging {
    override fun folgOpp() {
        duplikatrollerRepository
            .finnSakerMedDuplikateRoller()
            .forEach { createPatchForSak(it) }
    }

    fun createPatchForSak(saksnummer: String) {
        val jiraTittel = "Duplikate roller i sak $saksnummer"

        val eksisterendeSaker = jiraService.finnFagsystemsaker(jiraTittel)

        if (eksisterendeSaker.isNotEmpty()) {
            LOGGER.info { "Sak $saksnummer har allerede oppfølginssak: ${eksisterendeSaker.joinToString(", ")}" }
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
        val søknadslinjer = Entities(søknadslinjeRepository.findBySaksnr(saksnummer), Søknadslinje::id)
        val beløp = Entities(beløpRepository.findBySaksnr(saksnummer), Beløp::id)

        val sak = Sak(saksnummer, roller, søknadslinjer, beløp)

        val beskrivelse =
            buildString {
                append("Roller i sak " + sak.saksnr + ":\r\n")
                append("||Id||Type||FNR||Objektnr||Ukjent kode||RM||Født dato||Opprettet dato||\r\n")
                sak.roller.forEach { rolle ->
                    append(
                        "|${rolle.id}|${rolle.type}|${rolle.fnr}|${rolle.objektnr ?: " "}|${rolle.ukjentPartKode ?: " "}|${rolle.rmRolleId ?: " "}|${rolle.fodtDato}|${rolle.opprettetDato}|\r\n",
                    )
                }
            }

        val issue = jiraService.opprettFagsystemsak(jiraTittel, beskrivelse, null)
        val out = ByteArrayOutputStream()
        val patch = PrintStream(out)

        patch.println("Foreslår følgende patch:")
        patch.println("{code:sql}")

        createPatchForBp(patch, sak)
        createPatchForBm(patch, sak)
        createPatchForBarn(patch, sak)

        patch.println("{code}")

        patch.close()
        val kommentar: String = out.toString()

        jiraService.leggInnKommentar(issue, kommentar)
    }

    private fun createPatchForBp(
        patch: PrintStream,
        sak: Sak,
    ) {
        val bpRoller: Entities<Rolle, Long> = sak.roller.subset(Rolle::erBidragspliktig)

        if (bpRoller.size > 1) {
            patch.println("-- === BP ===")
            createPatchForRoller(patch, bpRoller, sak)
        }
    }

    private fun createPatchForBm(
        patch: PrintStream,
        sak: Sak,
    ) {
        val bmRoller: Entities<Rolle, Long> = sak.roller.subset(Rolle::erBidragsmottaker)

        if (bmRoller.size > 1) {
            patch.println("-- === BM ===")
            createPatchForRoller(patch, bmRoller, sak)
        }
    }

    private fun createPatchForBarn(
        patch: PrintStream,
        sak: Sak,
    ) {
        val barnRoller: Entities<Rolle, Long> = sak.roller.subset(Rolle::erBarn)

        val fnrListe: MutableSet<String?> = barnRoller.map { it.fnr }.toMutableSet()

        for (fnr in fnrListe) {
            val duplikaterForBarn: Entities<Rolle, Long> =
                barnRoller.withProperty(Rolle::fnr) { it == fnr }
            if (duplikaterForBarn.size > 1) {
                patch.println("-- === Barn med ident $fnr ===")
                createPatchForRoller(patch, duplikaterForBarn, sak)
            }
        }
    }

    private fun createPatchForRoller(
        patch: PrintStream,
        roller: Entities<Rolle, Long>,
        sak: Sak,
    ) {
        val første: Rolle? = roller.withLowestId()
        val siste: Rolle? = roller.withHighestId()

        val duplikater = roller.without(første)

        patch.println("-- Benytt rolle med lavest id: " + første?.id)
        patch.println()
        if ((første?.fnr == null || første.fnr!!.trim().isEmpty()) &&
            siste?.fnr != null && !siste.fnr!!.trim().isEmpty()
        ) {
            patch.println("-- Rolle er ikke lenger ukjent. Oppdaterer første rad.")
            patch.println("UPDATE t_rolle")
            patch.println("  SET fnr='" + siste.fnr + "',")
            patch.println("    ukj_part_kode=NULL,")
            patch.println("    fodt_dato='" + siste.fodtDato + "'")
            patch.println("  WHERE saksnr='" + sak.saksnr + "'")
            patch.println("    AND rolle_id=" + første?.id + ";")
            patch.println("-- 1? row updated")
            patch.println()
        }

        val expectedSoknadslinjer =
            sak.søknadslinjer.withProperty(Søknadslinje::rolle) { it in duplikater.idList() }.size
        patch.println("-- Oppdaterer søknadslinjer")
        patch.println("UPDATE t_soknad_linje")
        patch.println("  SET rolle_id=" + første?.id)
        patch.println("  WHERE saksnr='" + sak.saksnr + "'")
        patch.println(
            "    AND rolle_id IN (" + sqlIn(duplikater.idList()) +
                ");",
        )
        patch.println("-- $expectedSoknadslinjer? rows updated")
        patch.println()

        val expectedBelop: Int =
            sak.beløp.withProperty(Beløp::rolle) { it in duplikater.idList() }.size
        patch.println("-- Oppdaterer beløp")
        patch.println("UPDATE t_belop")
        patch.println("  SET rolle_id=" + første?.id)
        patch.println("  WHERE saksnr='" + sak.saksnr + "'")
        patch.println(
            "    AND rolle_id IN (" + sqlIn(duplikater.idList()) + ");",
        )
        patch.println("-- $expectedBelop? rows updated")
        patch.println()

        patch.println("-- Sletter duplikate rolle-linjer")
        patch.println("DELETE FROM t_rolle")
        patch.println("  WHERE saksnr='" + sak.saksnr + "'")
        patch.println(
            "    AND rolle_id IN (" + sqlIn(duplikater.idList()) + ");",
        )
        patch.println("-- " + duplikater.size + "? rows deleted")
        patch.println()
    }

    fun sqlIn(rows: Collection<*>): String = rows.joinToString(", ") { obj: Any? -> obj.toString() }
}
