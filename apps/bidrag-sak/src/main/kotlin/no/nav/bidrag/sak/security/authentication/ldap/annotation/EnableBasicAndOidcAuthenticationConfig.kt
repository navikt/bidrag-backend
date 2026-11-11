package no.nav.bidrag.sak.security.authentication.ldap.annotation

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.sak.security.authentication.ldap.LdapUserService
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@ComponentScan(includeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE)])
@Configuration
class EnableBasicAndOidcAuthenticationConfig(
    private val ldapUserService: LdapUserService,
) : WebMvcConfigurer {
    private val logger = KotlinLogging.logger {}

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(controllerInterceptor())
    }

    private fun controllerInterceptor(): BasicAuthenticationControllerHandlerInterceptor {
        logger.debug { "registering basic authentication controller handler interceptor" }
        return BasicAuthenticationControllerHandlerInterceptor(
            ldapUserService,
        )
    }
}
