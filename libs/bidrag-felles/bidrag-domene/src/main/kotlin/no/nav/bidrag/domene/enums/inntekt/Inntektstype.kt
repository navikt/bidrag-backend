package no.nav.bidrag.domene.enums.inntekt

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Inntektstype {
    AAP, // Ytelse fra offentlig
    DAGPENGER, // Ytelse fra offentlig
    FORELDREPENGER, // Ytelse fra offentlig
    INTRODUKSJONSSTØNAD, // Ytelse fra offentlig
    KVALIFISERINGSSTØNAD, // Ytelse fra offentlig
    OVERGANGSSTØNAD, // Ytelse fra offentlig
    PENSJON, // Ytelse fra offentlig
    SYKEPENGER, // Ytelse fra offentlig
    KONTANTSTØTTE, // Ytelse fra offentlig
    SMÅBARNSTILLEGG, // Ytelse fra offentlig
    UTVIDET_BARNETRYGD, // Ytelse fra offentlig
    KAPITALINNTEKT,
    LØNNSINNTEKT,
    NÆRINGSINNTEKT,
    BARNETILSYN, // Ytelse fra offentlig

    // BARNETILLEGG
    @JsonAlias("PENSJON")
    BARNETILLEGG_PENSJON,
    BARNETILLEGG_UFØRETRYGD,
    BARNETILLEGG_DAGPENGER,
    BARNETILLEGG_KVALIFISERINGSSTØNAD,
    BARNETILLEGG_AAP,
    BARNETILLEGG_DNB,
    BARNETILLEGG_NORDEA,
    BARNETILLEGG_STOREBRAND,
    BARNETILLEGG_KLP,
    BARNETILLEGG_SPK,
    BARNETILLEGG_TILTAKSPENGER,

    // Barnetillegg hentet fra Bisys
    BARNETILLEGG_SUMMERT,

    ;

    companion object {
        /**
         * Extension function som returnerer en liste av inntektrapporteringer som inneholder den aktuelle inntekttypen.
         * @return Liste av type InntektRapportering
         */
        fun Inntektstype.inngårIInntektRapporteringer() = Inntektsrapportering.entries.filter { this in it.inneholderInntektstypeListe }
    }
}
