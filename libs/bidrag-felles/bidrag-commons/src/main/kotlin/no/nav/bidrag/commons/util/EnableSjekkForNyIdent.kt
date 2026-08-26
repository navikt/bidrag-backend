package no.nav.bidrag.commons.util

import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(value = [IdentConsumer::class, SjekkForNyIdentAspect::class, IdentUtils::class])
annotation class EnableSjekkForNyIdent
