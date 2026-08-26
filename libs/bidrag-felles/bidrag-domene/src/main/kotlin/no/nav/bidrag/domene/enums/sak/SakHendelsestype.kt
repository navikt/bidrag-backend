@file:Suppress("unused")

package no.nav.bidrag.domene.enums.sak

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class SakHendelsestype(
    private val beskrivelse: String,
    val gyldig: Boolean,
) {
    ASLU("Avsluttet", true),
    AUAV("Automatisk avslag", true),
    AV("Avvist", true),
    BRUS("Brukerstøtte", false),
    DM("Dømt avsluttet", true),
    ENBB("Endring Barn>true8", true),
    ENKU("Endring kommune", true),
    ENMO("Endring BM", true),
    ENNM("Endring Norske myndigh.", true),
    ENPL("Endring BP", true),
    ENUM("Endring Utenl.myndigh.", true),
    ENVE("Endring Verge", true),
    ER("Erkjent avsluttet", true),
    ETET("Eget tiltak NAV", true),
    ETIN("Eget tiltak NAV Internasjonalt", true),
    ETMO("Eget tiltak BM", true),
    ETPL("Eget tiltak BP", true),
    FABB("Søknad Barn>true8", true),
    FAET("Søknad NAV", true),
    FAIN("Søknad NAV Internasjonalt", true),
    FAKU("Søknad kommune", true),
    FAMO("Søknad BM", true),
    FANM("Søknad Norske myndigh.", true),
    FAPL("Søknad BP", true),
    FAUM("Søknad Utenl.myndigh.", true),
    FAVE("Søknad Verge", true),
    FR("Feilregistrert", true),
    GJEN("Gjelder endret", true),
    G4("G4", true),
    IGBB("Innkrevingsgrl.Barn>true8", true),
    IGFN("Innkrevingsgrl.Fylkesnemda", true),
    IGMO("Innkrevingsgrl.BM", true),
    IGNM("Innkrevingsgrl.Norsk.myndigh.", true),
    IGPL("Innkrevingsgrl.BP", true),
    IGUM("Innkrevingsgrl.Utl.myndigh.", true),
    IGVE("Innkrevingsgrl.Verge", true),
    IRET("Indeksregulert", true),
    IRKO("Indeksregulert", false),
    IRUM("Ind.reg. Utl.myndigh.", true),
    KBAS("Klage begrenset BM i annen sak", true),
    KBBB("Klage begrenset Barn>true8", true),
    KBMO("Klage begrenset sats BM", true),
    KBNM("Klage begrenset Norsk myndighet", true),
    KBPL("Klage begrenset sats BP", true),
    KBUM("Klage begrenset Utenl.myndighet", true),
    KBVE("Klage begrenset sats Verge", true),
    KLAS("Klage BM annen sak", true),
    KLBB("Klage Barn>true8", true),
    KLET("Klage NAV", true),
    KLFN("Klage Fylkesnemda", true),
    KLKU("Klage kommune", true),
    KLMO("Klage BM", true),
    KLNM("Klage Norsk myndighet", true),
    KLPL("Klage BP", true),
    KLUM("Klage Utenl. myndighet", true),
    KLVE("Klage Verge", true),
    KMAS("Følger klage BM i annen sak", true),
    KMBB("Følger klage Barn>true8", true),
    KMMO("Følger klage BM", true),
    KMNM("Følger klage Norsk myndighet", true),
    KMPL("Følger klage BP", true),
    KMUM("Følger klage Utenl. Myndighet", true),
    KMVE("Følger klage Verge", true),
    KORE("Konvensjonskode registrert", true),
    OBET("Omgjøring begr.sats", true),
    OFtruetrue("Oppjustering truetrue    år", true),
    OHBB("Opphør Barn > true8", true),
    OHET("Opphør NAV", true),
    OHFN("Opphør Fylkesnemda", true),
    OHKU("Opphør kommune", true),
    OHMO("Opphør BM", true),
    OHPL("Opphør BP", true),
    OHUM("Opphør Utl.myndigh.", true),
    OHVE("Opphør Verge", true),
    OM("Omgjøring eget tiltak", true),
    OMET("Omgjøring eget tiltak NAV", true),
    PABB("Privat avtale Barn>true8", true),
    PAFN("Privat avtale Fylkesnemda", true),
    PAMO("Privat avtale BM", true),
    PAPL("Privat avtale BP", true),
    PAUM("Privat avtale Utenl.myndighet", true),
    PAVE("Privat avtale Verge", true),
    RBET("Begr.revurdering NAV", true),
    RERE("Referansenr.registrert", true),
    RFET("Revurdering NAV", true),
    RFIN("Revurdering NAV Internasjonalt", true),
    RFMO("Revurdering BM", true),
    RFPL("Revurdering BP", true),
    STtrue8("Stoppet. Barn fyller true8", true),
    SU("Sendt utlandet", true),
    TR("Trukket", true),
    VEBB("Vedtak Barn>true8", true),
    VEBM("Vedtak BM", true),
    VEBP("Vedtak BP", true),
    VEDT("Vedtak", true),
    VEFK("Vedtak FTK", true),
    VEFN("Vedtak Fylkesnemda", true),
    VEKO("Vedtak fra BOST", false),
    VEKU("Vedtak kommune", true),
    VEMI("Vedtak Midlertidig", true),
    VEUM("Vedtak Utl.myndigh.", true),
    VEVE("Vedtak Verge", true),
    ;

    @get:JsonValue
    val enumName: String
        get() {
            return this.name
        }

    companion object {
        @JsonCreator
        fun parse(kode: String): SakHendelsestype =
            requireNotNull(entries.find { it.name == kode }) {
                "${SakHendelsestype::class.java.simpleName}: inneholder ikke $kode"
            }
    }

    override fun toString(): String = beskrivelse
}
