package no.nav.bidrag.domene.enums.vedtak

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class BehandlingsrefKilde {
    BEHANDLING_ID, // Id fra bidrag-behandling
    BISYS_SØKNAD, // Søknadsid fra Bisys
    BISYS_KLAGE_REF_SØKNAD, // For klage: Bisys søknadsid til den opprinnelige søknaden det klages på
    ALDERSJUSTERING_BIDRAG,
    ALDERSJUSTERING_FORSKUDD,
    REVURDERING_FORSKUDD,
}
