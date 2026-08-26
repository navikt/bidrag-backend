package no.nav.bidrag.commons.service.sjablon

import no.nav.bidrag.commons.service.retryTemplateSynchronous
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
@Import(SjablonConsumer::class)
class SjablonService(
    private val sjablonConsumer: SjablonConsumer,
) {
    fun hentSjablontall(): List<Sjablontall> =
        try {
            retryTemplateSynchronous(
                "SjablonService.hentSjablontall",
            ).execute<List<Sjablontall>, HttpClientErrorException> {
                sjablonConsumer.hentSjablontall().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablontall" }
            emptyList()
        }

    fun hentSjablonSamværsfradrag(): List<Samværsfradrag> =
        try {
            retryTemplateSynchronous(
                "SjablonService.hentSjablonSamværsfradrag",
            ).execute<List<Samværsfradrag>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonSamværsfradrag().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for samværsfradrag" }
            emptyList()
        }

    fun hentSjablonBidragsevne(): List<Bidragsevne> =
        try {
            retryTemplateSynchronous(
                "SjablonService.hentSjablonBidragsevne",
            ).execute<List<Bidragsevne>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonBidragsevne().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for bidragsevne" }
            emptyList()
        }

    fun hentSjablonTrinnvisSkattesats(): List<TrinnvisSkattesats> =
        try {
            retryTemplateSynchronous(
                "SjablonService.hentSjablonTrinnvisSkattesats",
            ).execute<List<TrinnvisSkattesats>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonTrinnvisSkattesats().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for trinnvis skattesats" }
            emptyList()
        }

    fun hentSjablonBarnetilsyn(): List<Barnetilsyn> =
        try {
            retryTemplateSynchronous(
                "SjablonProvider.hentSjablonBarnetilsyn",
            ).execute<List<Barnetilsyn>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonBarnetilsyn().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for barnetilsyn" }
            emptyList()
        }

    fun hentSjablonForbruksutgifter(): List<Forbruksutgifter> =
        try {
            retryTemplateSynchronous(
                "SjablonProvider.hentSjablonForbruksutgifter",
            ).execute<List<Forbruksutgifter>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonForbruksutgifter().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for forbruksutgifter" }
            emptyList()
        }

    fun hentSjablonMaksFradrag(): List<MaksFradrag> =
        try {
            retryTemplateSynchronous(
                "SjablonProvider.hentSjablonMaksFradrag",
            ).execute<List<MaksFradrag>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonMaksFradrag().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for maks fradrag" }
            emptyList()
        }

    fun hentSjablonMaksTilsyn(): List<MaksTilsyn> =
        try {
            retryTemplateSynchronous(
                "SjablonProvider.hentSjablonMaksTilsyn",
            ).execute<List<MaksTilsyn>, HttpClientErrorException> {
                sjablonConsumer.hentSjablonMaksTilsyn().body
            }
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for maks tilsyn" }
            emptyList()
        }
}
