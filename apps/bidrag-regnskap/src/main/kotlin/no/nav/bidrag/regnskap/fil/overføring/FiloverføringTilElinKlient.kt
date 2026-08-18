package no.nav.bidrag.regnskap.fil.overføring

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.bidrag.regnskap.config.FiloverføringTilElinConfig
import no.nav.bidrag.regnskap.persistence.bucket.GcpFilBucket
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger { }

@Service
class FiloverføringTilElinKlient(
    private val config: FiloverføringTilElinConfig,
    private val gcpFilBucket: GcpFilBucket,
    private val slackService: SlackService,
) {

    private val jSch = JSch().apply {
        addIdentity(
            config.username,
            config.privateKeyDecoded,
            null,
            null,
        )
    }

    fun lastOppFilTilFilsluse(filmappe: String, filnavn: String) {
        if (!config.skalOverforeFil) {
            LOGGER.info { "Miljøet er konfigurert til å ikke overføre fil til SFTP. Fil blir derfor kun lastet opp til bucket." }
            return
        }
        LOGGER.info { "Start oppkobling mot filsluse..." }
        var session: Session? = null
        val channel: ChannelSftp?
        try {
            session = jSch.getSession(config.username, config.host, config.port)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(15000)

            channel = session.openChannel(FiloverføringTilElinConfig.JSCH_CHANNEL_TYPE_SFTP) as ChannelSftp
            channel.connect()
            LOGGER.info { "Oppkobling mot filsluse var vellykket!" }
            LOGGER.info { "Starter opplasting av fil: $filnavn fra GCP-bucket..." }
            channel.cd(config.directory)
            channel.put(gcpFilBucket.hentFil(filmappe + filnavn), filnavn)
            LOGGER.info { "Fil: $filnavn har blitt lastet opp på filsluse!" }
        } catch (e: Exception) {
            slackService.sendMelding(
                ":Warning: Noe gikk galt ved overføring av ${filmappe + filnavn} til ELIN! :Warning:" +
                    "\n\nFeilmelding: ${e.message}" +
                    "\n${gcpMelding(filmappe + filnavn)}",
            )
            throw e
        } finally {
            session?.disconnect()
        }
    }

    fun gcpMelding(filnavn: String): String = gcpFilBucket.hentInfoOmFil(filnavn)
}
