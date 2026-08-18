package no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api

import java.time.LocalDate

data class HentEnhetsregisterRequest(val organisasjonsnummer: String, val gyldigDato: String? = LocalDate.now().toString())
