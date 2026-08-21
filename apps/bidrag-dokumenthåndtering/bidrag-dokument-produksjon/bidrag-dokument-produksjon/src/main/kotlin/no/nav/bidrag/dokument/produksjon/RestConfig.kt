package no.nav.bidrag.dokument.produksjon

import no.nav.bidrag.commons.service.AppContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(AppContext::class)
class RestConfig
