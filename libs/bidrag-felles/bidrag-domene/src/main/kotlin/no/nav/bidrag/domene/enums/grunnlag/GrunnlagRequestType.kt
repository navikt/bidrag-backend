package no.nav.bidrag.domene.enums.grunnlag

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Brukes for å spesifisere hvilke grunnlag man ønsker at skal innhentes av tjenesten bidrag-grunnlag.
 * Må ikke forveksles med Grunnlagstype.
 */
@Schema(enumAsRef = true)
enum class GrunnlagRequestType {
    AINNTEKT,
    SKATTEGRUNNLAG,
    UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
    BARNETILLEGG,
    HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
    SIVILSTAND,
    KONTANTSTØTTE,
    BARNETILSYN,
    ARBEIDSFORHOLD,
    TILLEGGSSTØNAD,

    @Deprecated("OVERGANGSSTONAD skal utgå som egen type. Må koordineres med Bisys")
    OVERGANGSSTONAD,
}
