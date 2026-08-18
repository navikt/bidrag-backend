package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.vedtakoverføring

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.Oppfølging
import no.nav.bidrag.commons.service.slack.SlackService
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger { }

@Service
class VedtakoverføringOppfølgingService(
    private val slackService: SlackService,
    private val vedtakoverføringRepository: VedtakoverføringRepository,
) : Oppfølging {
    private var varslede: Set<Int?> = HashSet()

    override fun folgOpp() {
        val feiledeVedtaksoverføringer = vedtakoverføringRepository.finnOverføringerMedStatusFeilet()

        val saksnrListe: String? =
            feiledeVedtaksoverføringer
                ?.asSequence()
                ?.filterNotNull()
                ?.filter { !varslede.contains(it.id) }
                ?.map { it.saksnr + " (" + it.id + "): " + forkort(it.notat) }
                ?.sorted()
                ?.joinToString(separator = "\n")

        val feiledeVedtak = feiledeVedtaksoverføringer?.map { it?.id }?.toSet()
        if (!saksnrListe.isNullOrEmpty()) {
            LOGGER.info { "Nye feilede vedtaksoverføringer:\n$saksnrListe" }

            slackService.sendMelding(
                melding = "Nye feilede vedtaksoverføringer (" + feiledeVedtak?.size + " totalt)",
                markdownTekst = saksnrListe,
            )
        }
        this.varslede = feiledeVedtak ?: varslede
    }

    private fun forkort(
        tekst: String?,
        antallTegn: Int = 40,
    ): String? {
        if (tekst == null || tekst.length <= antallTegn) {
            return tekst
        }
        return tekst.substring(0, antallTegn - 1) + "…"
    }
}
