package no.nav.bidrag.person.dto

data class KrrPersonKontaktinformasjonRespons(val personer: Map<String, Kontaktinfo>?)

data class Kontaktinfo(val spraak: String?)
