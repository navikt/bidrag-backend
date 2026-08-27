package no.nav.bidrag.commons.service.sjablon

import no.nav.bidrag.commons.service.AppContext
import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(value = [SjablonConsumer::class, SjablonService::class, AppContext::class])
annotation class EnableSjablonProvider
