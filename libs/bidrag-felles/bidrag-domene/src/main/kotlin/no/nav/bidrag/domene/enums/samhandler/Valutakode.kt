package no.nav.bidrag.domene.enums.samhandler

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(enumAsRef = true, name = "Valutakode")
enum class Valutakode(
    val visningsnavn: String,
    val utgåttDato: LocalDate? = null,
) {
    ALL("Albanske lek"),
    ANG("NL Antillene Gylden", LocalDate.of(2025, 4, 1)),
    ARS("Argentinsk peso"),
    AUD("Australske dollar"),
    BAM("Bosniske Mark"),
    BDT("Bangladeshi taka"),
    BGN("Bulgarsk lev", LocalDate.of(2026, 1, 1)),
    BRL("Brasilske reais"),
    BYN("Belarusiske nye rubler"),
    CAD("Canadiske dollar"),
    CHF("Sveitsiske Franc"),
    CNY("Kinesiske Yen"),
    COP("Kolombiansk peso"),
    CZK("Tsjekkiske koruna"),
    DKK("Danske kroner"),
    DZD("Algerisk dinar"),
    EEK("Estiske kroon", LocalDate.of(2011, 1, 1)),
    EUR("Euro"),
    GBP("Britiske Pund"),
    HKD("Hong Kong dollar"),
    HRK("Kroatiske kuna", LocalDate.of(2023, 1, 1)),
    HUF("Ungarske forint"),
    IDR("Indonesiske rupiah"),
    ILS("Ny israelsk shekel"),
    INR("Indiske Rupees"),
    ISK("Islandske kroner"),
    JPY("Japanske Yen"),
    KRW("Sørkoreanske won"),
    LTL("Litauiske litas", LocalDate.of(2015, 1, 1)),
    LVL("Latviske lat", LocalDate.of(2014, 1, 1)),
    MAD("Marokkansk dirham"),
    MMK("Myanmar kyat"),
    MXN("Meksikansk peso"),
    MYR("Malaysiske ringgit"),
    NOK("Norske kroner"),
    NZD("New Zealand dollar"),
    PHP("Filippinske peso"),
    PKR("Pakistanske rupi"),
    PLN("Polske zloty"),
    RON("Rumenske leu"),
    RSD("Serbiske dinar"),
    RUB("Russiske rubler"),
    SEK("Svenske kroner"),
    SGD("Singapore dollar"),
    THB("Thailandske bath"),
    TND("Tunisiske dinarer"),
    TRY("Tyrkiske lire"),
    TWD("Nye taiwanske dollar"),
    UAH("Ukrainsk hryvnia"),
    USD("Amerikanske dollar"),
    VND("Vietnamesisk dong "),
    ZAR("Sør-Afrika Rep. rand"),
    ;

    fun aktiv(dato: LocalDate = LocalDate.now()) = utgåttDato == null || dato.isBefore(utgåttDato)

    companion object {
        fun fraVisningsnavn(visningsnavn: String): Valutakode? = entries.firstOrNull { it.visningsnavn == visningsnavn }
    }
}
