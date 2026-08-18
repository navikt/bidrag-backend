package no.nav.bidrag.grunnlag.util

import no.nav.bidrag.transport.felles.commonObjectmapper

fun <T> toJsonString(entity: T): String = commonObjectmapper.findAndRegisterModules().writeValueAsString(entity)
