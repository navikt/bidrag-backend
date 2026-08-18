package no.nav.bidrag.sak.security.authentication.ldap.annotation

import no.nav.bidrag.sak.security.authentication.ldap.LdapUserService
import no.nav.security.token.support.spring.EnableJwtTokenValidationConfiguration
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotationMetadata
import org.springframework.web.servlet.config.annotation.InterceptorRegistry

@ComponentScan(includeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE)])
@Configuration
class EnableBasicAndOidcAuthenticationConfig(
    private val ldapUserService: LdapUserService,
    environment: Environment,
) : EnableJwtTokenValidationConfiguration(environment) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var enableBasicAuthentication: AnnotationAttributes

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(controllerInterceptor())
    }

    override fun setImportMetadata(meta: AnnotationMetadata) {
        enableBasicAuthentication =
            AnnotationAttributes.fromMap(
                meta.getAnnotationAttributes(EnableBasicAndOidcAuthentication::class.java.name, false),
            ) ?: throw IllegalArgumentException(
                "@EnableBasicAuthentication is not present on importing class ${meta.className}",
            )
    }

    private fun controllerInterceptor(): BasicAuthenticationControllerHandlerInterceptor {
        logger.debug("registering basic authentication controller handler interceptor")
        return BasicAuthenticationControllerHandlerInterceptor(
            ldapUserService,
        )
    }
}
