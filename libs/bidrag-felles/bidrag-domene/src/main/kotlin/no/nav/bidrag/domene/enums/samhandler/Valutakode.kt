package no.nav.bidrag.domene.enums.samhandler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Valutakode")
enum class Valutakode(
    val visningsnavn: String,
) {
    ALL("Albanske lek"),
    ANG("NL Antillene Gylden"),
    AUD("Australske dollar"),
    BAM("Bosniske Mark"),
    BGN("Bulgarsk lev"),
    BRL("Brasilske reais"),
    CAD("Canadiske dollar"),
    CHF("Sveitsiske Franc"),
    CNY("Kinesiske Yen"),
    CZK("Tsjekkiske koruna"),
    DKK("Danske kroner"),
    EEK("Estiske kroon"),
    EUR("Euro"),
    GBP("Britiske Pund"),
    HKD("Hong Kong dollar"),
    HRK("Kroatiske kuna"),
    HUF("Ungarske forint"),
    INR("Indiske Rupees"),
    ISK("Islandske kroner"),
    JPY("Japanske Yen"),
    LTL("Litauiske litas"),
    LVL("Latviske lat"),
    MAD("Marokkansk dirham"),
    NOK("Norske kroner"),
    NZD("New Zealand dollar"),
    PKR("Pakistanske rupi"),
    PLN("Polske zloty"),
    RON("Rumenske leu"),
    RSD("Serbiske dinar"),
    SEK("Svenske kroner"),
    THB("Thailandske bath"),
    TND("Tunisiske dinarer"),
    TRY("Tyrkiske lire"),
    UAH("Ukrainsk hryvnia"),
    USD("Amerikanske dollar"),
    VND("Vietnamesisk dong "),
    ZAR("Sør-Afrika Rep. rand"),
    PHP("Filippinske peso"),
    ;

    companion object {
        fun fraVisningsnavn(visningsnavn: String): Valutakode? = entries.firstOrNull { it.visningsnavn == visningsnavn }
    }
}
