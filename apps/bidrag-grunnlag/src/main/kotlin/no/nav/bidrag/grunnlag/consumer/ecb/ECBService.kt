package no.nav.bidrag.grunnlag.consumer.ecb

import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.Valutakurs
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.exchangeRateForCurrency
import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.ValutakursTransformationException
import no.nav.bidrag.grunnlag.consumer.valutakurser.ECBConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.toExchangeRates
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
class ECBService(
    private val ecbConsumer: ECBConsumer,
) {
    private val logger: Logger = LoggerFactory.getLogger(ECBService::class.java)

    /**
     * @param utenlandskValuta valutaen vi skal konvertere til NOK
     * @param kursDato datoen vi skal hente valutakurser for
     * @return Henter valutakurs for *utenlandskValuta* -> EUR og NOK -> EUR på *kursDato*, og returnerer en beregnet kurs for *utenlandskValuta* -> NOK.
     */
    @Throws(ECBServiceException::class)
    fun hentValutakurs(
        utenlandskValuta: String,
        kursDato: LocalDate,
    ): Valutakurs {
        logger.info("Henter valutakurs for ${utenlandskValuta.saner()} på $kursDato")
        try {
            val respons = ecbConsumer.hentValutakurs(Frequency.Monthly, listOfNotNull(ECBConstants.NOK, utenlandskValuta.takeUnless { it == ECBConstants.EUR }), kursDato)
            val valutakurser = when (respons) {
                is RestResponse.Success -> respons.body.toExchangeRates()
                is RestResponse.Failure -> throw ECBServiceException("ECB-svaret feilet med status ${respons.statusCode.value()}: ${respons.message}", respons.restClientException)
            }
            validateExchangeRates(utenlandskValuta, kursDato, valutakurser)
            val valutakursNOK = valutakurser.exchangeRateForCurrency(ECBConstants.NOK)!!
            val kurs =
                if (utenlandskValuta == ECBConstants.EUR) {
                    valutakursNOK.kurs
                } else {
                    val valutakursUtenlandskValuta = valutakurser.exchangeRateForCurrency(utenlandskValuta)!!
                    beregnValutakursINOK(valutakursUtenlandskValuta.kurs, valutakursNOK.kurs)
                }
            return Valutakurs(utenlandskValuta, kurs, kursDato)
        } catch (e: ValutakursTransformationException) {
            throw ECBServiceException(e.message, e)
        }
    }

    private fun beregnValutakursINOK(
        valutakursUtenlandskValuta: BigDecimal,
        valutakursNOK: BigDecimal,
    ) = valutakursNOK.divide(valutakursUtenlandskValuta, 10, RoundingMode.HALF_UP)

    private fun validateExchangeRates(
        currency: String,
        exchangeRateDate: LocalDate,
        exchangeRates: List<Valutakurs>,
    ) {
        val expectedSize = if (currency != ECBConstants.EUR) 2 else 1
        val currencies =
            if (currency != ECBConstants.EUR) listOf(currency, ECBConstants.NOK) else listOf(ECBConstants.NOK)

        if (!isValid(exchangeRates, currencies, exchangeRateDate, expectedSize)) {
            throwValidationException(currency, exchangeRateDate)
        }
    }

    private fun isValid(
        exchangeRates: List<Valutakurs>,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
        expectedSize: Int,
    ) = exchangeRates.size == expectedSize &&
        exchangeRates.all { YearMonth.from(it.kursDato) == YearMonth.from(exchangeRateDate) && it.kurs.signum() > 0 } &&
        exchangeRates.map { it.valuta }.toSet() == currencies.toSet()

    private fun throwValidationException(
        currency: String,
        exchangeRateDate: LocalDate,
    ): Unit = throw ECBServiceException("Fant ikke nødvendige valutakurser for valutakursdato $exchangeRateDate for å bestemme valutakursen $currency - NOK")
}

object ECBConstants {
    const val NOK = "NOK"
    const val EUR = "EUR"
}

const val ALFANUMERISKE_TEGN = "a-zæøåA-ZÆØÅ0-9"

fun String.saner(): String = Regex("[^$ALFANUMERISKE_TEGN]*").replace(this, "")
