package no.nav.bidrag.commons.unleash

import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(value = [UnleashFeaturesProvider::class, UnleashProperties::class])
annotation class EnableUnleashFeatures
