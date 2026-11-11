package no.nav.bidrag.sak.security.authentication.ldap.annotation

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(EnableBasicAndOidcAuthenticationConfig::class)
@EnableJwtTokenValidation
annotation class EnableBasicAndOidcAuthentication(
    @get:AliasFor(annotation = EnableJwtTokenValidation::class, attribute = "ignore")
    val ignore: Array<String> = ["org.springframework"],
)
