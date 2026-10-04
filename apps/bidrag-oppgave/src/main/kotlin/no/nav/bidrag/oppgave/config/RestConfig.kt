package no.nav.bidrag.oppgave.config

import no.nav.bidrag.rest.GlobalRestControllerAdvice
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(GlobalRestControllerAdvice::class)
class RestConfig
