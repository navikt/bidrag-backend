package no.nav.bidrag.commons.service.slack

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.composition.BlockCompositions.markdownText
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Tjeneste for sending, oppdatering og tråding av meldinger i Slack.
 *
 * Bruker Slack sitt Java SDK for å kommunisere med Slack API-et.
 * Returnerer en [SlackMelding] ved sending av meldinger, som gir funksjonalitet
 * for å oppdatere den opprinnelige meldingen eller svare i tråden.
 *
 * @param oauthToken Bot OAuth Token for autentisering (hentes fra BIDRAG_BOT_SLACK_OAUTH_TOKEN).
 * @param channel ID på kanalen meldingen skal sendes til (hentes fra SLACK_CHANNEL_ID).
 */
@Service
class SlackService(
    @param:Value($$"${BIDRAG_BOT_SLACK_OAUTH_TOKEN}") private val oauthToken: String,
    @param:Value($$"${SLACK_CHANNEL_ID}") private val channel: String,
) {
    internal val client: MethodsClient by lazy { Slack.getInstance().methods(oauthToken) }

    companion object {
        internal val LOGGER = KotlinLogging.logger { }
    }

    fun sendMelding(
        melding: String,
        threadTs: String? = null,
        markdownTekst: String? = null,
    ): SlackMelding =
        try {
            val response =
                client.chatPostMessage { req ->
                    req.channel(channel).threadTs(threadTs).text(melding).also {
                        if (markdownTekst != null) {
                            it.blocks(
                                listOf(
                                    section { s -> s.text(markdownText(melding)) },
                                    section { s -> s.text(markdownText(markdownTekst)) },
                                ),
                            )
                        }
                    }
                }

            if (response.isOk) {
                LOGGER.debug { "Slack melding sendt: $melding" }
            } else {
                LOGGER.error { "Feil ved sending av slackmelding: ${response.error}" }
            }
            SlackMelding(slackService = this, ts = response.ts, threadTs = threadTs ?: response.ts, channel = response.channel ?: channel)
        } catch (e: Exception) {
            LOGGER.error(e) { "Uventet feil ved sending av slackmelding" }
            SlackMelding(slackService = this, ts = null, threadTs = threadTs, channel = channel)
        }
}

class SlackMelding(
    private val slackService: SlackService,
    private val ts: String?,
    private val threadTs: String? = ts,
    private val channel: String?,
) {
    fun oppdaterMelding(melding: String) {
        if (ts == null) {
            SlackService.LOGGER.warn { "Ingen melding å oppdatere..." }
            return
        }
        try {
            val response =
                slackService.client.chatUpdate {
                    it
                        .channel(channel)
                        .ts(ts)
                        .text(melding)
                }
            if (response.isOk) {
                SlackService.LOGGER.trace { "Slack melding oppdatert: $melding" }
            } else {
                SlackService.LOGGER.error { "Feil ved oppdatering av slackmelding: ${response.error}" }
            }
        } catch (e: Exception) {
            SlackService.LOGGER.error(e) { "Uventet feil ved oppdatering av slackmelding" }
        }
    }

    fun svarITråd(melding: String): SlackMelding {
        if (ts == null) {
            SlackService.LOGGER.trace { "Ingen melding å svare på..." }
            return this
        }
        return slackService.sendMelding(melding = melding, threadTs = threadTs)
    }
}
