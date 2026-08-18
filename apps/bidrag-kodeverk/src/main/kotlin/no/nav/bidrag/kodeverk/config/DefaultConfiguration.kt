package no.nav.bidrag.kodeverk.config

import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.MdcFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import

@EnableAspectJAutoProxy
@Configuration
@EnableOAuth2Client(cacheEnabled = true)
@Import(DefaultCorsFilter::class, MdcFilter::class)
class DefaultConfiguration
