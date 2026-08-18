package no.nav.bidrag.admin.utils

import no.nav.bidrag.admin.dto.EndringsLoggDto

fun List<EndringsLoggDto>.sorterEtterDato(): List<EndringsLoggDto> = this.sortedByDescending { it.dato }
