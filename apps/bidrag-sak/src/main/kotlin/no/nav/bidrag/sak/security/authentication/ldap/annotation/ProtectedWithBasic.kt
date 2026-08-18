package no.nav.bidrag.sak.security.authentication.ldap.annotation

import no.nav.security.token.support.core.api.Protected

@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Protected
annotation class ProtectedWithBasic(
    val groups: Array<String>,
)
