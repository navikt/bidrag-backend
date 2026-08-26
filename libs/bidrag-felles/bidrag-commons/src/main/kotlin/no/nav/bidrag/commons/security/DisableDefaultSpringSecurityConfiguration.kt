package no.nav.bidrag.commons.security

import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.WebSecurityConfigurer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@ConditionalOnDefaultWebSecurity
@EnableWebSecurity
class DisableDefaultSpringSecurityConfiguration : WebSecurityConfigurer<WebSecurity> {
    override fun init(builder: WebSecurity) {
    }

    override fun configure(web: WebSecurity) {
        web.ignoring().requestMatchers("/**")
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http.build()
}
