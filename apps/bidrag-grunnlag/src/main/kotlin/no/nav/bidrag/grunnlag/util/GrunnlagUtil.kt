package no.nav.bidrag.grunnlag.util

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.apache.commons.lang3.StringUtils
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import java.text.SimpleDateFormat

open class GrunnlagUtil {

    companion object {
        private const val BIDRAG_FILTER = "BidragA-Inntekt"
        private const val FORSKUDD_FILTER = "BidragsforskuddA-Inntekt"
        private const val BIDRAG_FORMAAL = "Bidrag"
        private const val FORSKUDD_FORMAAL = "Bidragsforskudd"
        const val JANUAR2015 = "2015-01"

        fun finnFilter(formaal: String): String = if (formaal == Formål.FORSKUDD.toString()) FORSKUDD_FILTER else BIDRAG_FILTER

        fun finnFormaal(formaal: String): String = if (formaal == Formål.FORSKUDD.toString()) FORSKUDD_FORMAAL else BIDRAG_FORMAAL

        // BNR / NPID har måned 21-32
        fun erBnrEllerNpid(fnr: String): Boolean {
            if (!erGyldigFnr(fnr)) {
                return false
            }
            val month = fnr.substring(2, 4).toInt()
            return month in 21..32
        }

        private fun erGyldigFnr(fnr: String) = fnr.length == 11 && StringUtils.isNumeric(fnr)

        fun evaluerFeilmelding(melding: String?, grunnlagstype: GrunnlagRequestType): String {
            if (melding == null) return ""

            if (melding.startsWith("Message: 401")) return "Ikke autorisert"

            return when (grunnlagstype) {
                GrunnlagRequestType.BARNETILSYN,
                GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                GrunnlagRequestType.KONTANTSTØTTE,
                -> {
                    val regex = Regex("\"melding\":\"(.*?)\"")
                    val matchResult = regex.find(melding)
                    matchResult?.groupValues?.get(1) ?: melding.take(100)
                }

                GrunnlagRequestType.SKATTEGRUNNLAG -> {
                    var regex = Regex("\"melding\":\"(.*?)\"")
                    var matchResult = regex.find(melding)
                    matchResult?.groupValues?.get(1) ?: run {
                        regex = Regex("\"message\":\"(.*?)\"")
                        matchResult = regex.find(melding)
                        matchResult?.groupValues?.get(1) ?: melding.take(100)
                    }
                }

                GrunnlagRequestType.AINNTEKT -> {
                    val regex = Regex("\"message\":\"(.*?)\"")
                    val matchResult = regex.find(melding)
                    matchResult?.groupValues?.get(1) ?: melding.take(100)
                }

                GrunnlagRequestType.ARBEIDSFORHOLD -> {
                    var regex = Regex("\"meldinger\":\\[\"(.*?)\"]")
                    var matchResult = regex.find(melding)
                    matchResult?.groupValues?.get(1) ?: run {
                        regex = Regex("\"error\":\"(.*?)\"")
                        matchResult = regex.find(melding)
                        matchResult?.groupValues?.get(1) ?: melding.take(100)
                    }
                }

                GrunnlagRequestType.BARNETILLEGG -> {
                    var regex = Regex("\"feil\":\"(.*?)\"")
                    var matchResult = regex.find(melding)
                    matchResult?.groupValues?.get(1) ?: run {
                        regex = Regex("\"(.*?)\"")
                        matchResult = regex.find(melding)
                        matchResult?.groupValues?.get(1) ?: melding.take(100)
                    }
                }

                GrunnlagRequestType.SIVILSTAND,
                GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN,
                -> {
                    val regex = Regex("\"Warning\":\\[\"(.*?)\"]")
                    val matchResult = regex.find(melding)
                    matchResult?.groupValues?.get(1) ?: melding.take(100)
                }

                else -> melding
            }
        }

        fun evaluerFeiltype(melding: String?, httpStatuskode: HttpStatusCode): HentGrunnlagFeiltype = if (melding?.contains("Fant ikke person") == true) {
            HentGrunnlagFeiltype.FUNKSJONELL_FEIL
        } else if ((httpStatuskode.is5xxServerError) || (httpStatuskode == HttpStatus.UNAUTHORIZED) || (httpStatuskode == HttpStatus.FORBIDDEN)) {
            HentGrunnlagFeiltype.TEKNISK_FEIL
        } else {
            HentGrunnlagFeiltype.FUNKSJONELL_FEIL
        }

        fun <T> any(type: Class<T>): T = Mockito.any(type)

        fun <T> any(): T = Mockito.any()

        fun tilJson(json: Any): String {
            commonObjectmapper.registerKotlinModule()
            commonObjectmapper.writerWithDefaultPrettyPrinter()
            commonObjectmapper.registerModule(JavaTimeModule())
            commonObjectmapper.dateFormat = SimpleDateFormat("yyyy-MM-dd")
            return commonObjectmapper.writeValueAsString(json)
        }
    }
}
