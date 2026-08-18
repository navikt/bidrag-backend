package no.nav.bidrag.admin.config

import no.nav.bidrag.commons.service.slack.SlackService
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@Import(SlackService::class)
@EnableScheduling
@Configuration
class DefaultConfig
