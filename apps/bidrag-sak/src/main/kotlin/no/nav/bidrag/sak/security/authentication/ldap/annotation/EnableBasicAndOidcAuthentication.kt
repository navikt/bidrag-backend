package no.nav.bidrag.sak.security.authentication.ldap.annotation

import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(EnableBasicAndOidcAuthenticationConfig::class)
annotation class EnableBasicAndOidcAuthentication(
    val ignore: Array<String> = ["org.springframework"],
)
