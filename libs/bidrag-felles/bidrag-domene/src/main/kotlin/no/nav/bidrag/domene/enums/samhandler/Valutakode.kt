package no.nav.bidrag.domene.enums.samhandler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Valutakode")
enum class Valutakode(
    val visningsnavn: String,
) {
    ALL("Albanske lek"),
    ANG("NL Antillene Gylden"),
    ARS("Argentinsk peso"),
    AUD("Australske dollar"),
    BAM("Bosniske Mark"),
    BDT("Bangladeshi taka"),
    BGN("Bulgarsk lev"),
    BRL("Brasilske reais"),
    BYN("Belarusiske nye rubler"),
    CAD("Canadiske dollar"),
    CHF("Sveitsiske Franc"),
    CNY("Kinesiske Yen"),
    COP("Kolombiansk peso"),
    CZK("Tsjekkiske koruna"),
    DKK("Danske kroner"),
    DZD("Algerisk dinar"),
    EEK("Estiske kroon"),
    EUR("Euro"),
    GBP("Britiske Pund"),
    HKD("Hong Kong dollar"),
    HRK("Kroatiske kuna"),
    HUF("Ungarske forint"),
    IDR("Indonesiske rupiah"),
    ILS("Ny israelsk shekel"),
    INR("Indiske Rupees"),
    ISK("Islandske kroner"),
    JPY("Japanske Yen"),
    KRW("Sørkoreanske won"),
    LTL("Litauiske litas"),
    LVL("Latviske lat"),
    MAD("Marokkansk dirham"),
    MMK("Myanmar kyat"),
    MXN("Myanmar kyat"),
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

    companion object {
        fun fraVisningsnavn(visningsnavn: String): Valutakode? = entries.firstOrNull { it.visningsnavn == visningsnavn }
    }
}
