package no.nav.bidrag.commons.service.forsendelse

import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(value = [FellesForsendelseMapper::class, FellesForsendelseService::class])
annotation class EnableForsendelseService
