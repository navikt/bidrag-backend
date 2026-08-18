package no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.porten

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.admin.produksjonsoppfølging.jira.JiraService
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.JiraSak
import no.nav.bidrag.admin.produksjonsoppfølging.oppfølginger.Oppfølging
import no.nav.bidrag.commons.service.slack.SlackService
import org.springframework.stereotype.Service
import java.util.regex.Matcher

private val LOGGER = KotlinLogging.logger { }

@Service
class PortenService(
    private val slackService: SlackService,
    private val jiraService: JiraService,
) : Oppfølging {
    companion object {
        private val LOG_PRE: String? =
            Matcher.quoteReplacement(
                "https://logs.az.nav.no/app/data-explorer/discover?security_tenant=navlogs#?_g=(time:(from:now-7d,to:now))&_q=(filters:!(),query:(language:kuery,query:%22",
            )
        private val LOG_POST: String? = Matcher.quoteReplacement("%22))")
    }

    override fun folgOpp() {
        jiraService.finnÅpnePortenSakerUtenSlackLabel().forEach { varsleSak(it) }
    }

    private fun varsleSak(jiraSak: JiraSak) {
        LOGGER.info { "Behandler sak ${jiraSak.issue} med tittel ${jiraSak.summary}" }

        try {
            jiraService.addLabel(jiraSak.issue!!, "Slack")
            val key: String? = jiraSak.issue
            val link = "https://jira.adeo.no/browse/${jiraSak.issue}"
            val melding = ":left_speech_bubble: *<$link|$key> mottatt*"

            val summary: String? =
                jiraSak.summary
                    ?.replace("([0-9]{6}\\s*)[0-9]{5}", "$1xxxxx")
                    ?.replace("([0-9a-zA-Z]{3}-[0-9a-zA-Z]{4})", "<$LOG_PRE$1$LOG_POST|$1>")

            slackService.sendMelding(melding = melding, markdownTekst = summary)
        } catch (e: Exception) {
            LOGGER.error(e) { "Feil ved varsling av sak ${jiraSak.issue}" }
        }
    }
}
