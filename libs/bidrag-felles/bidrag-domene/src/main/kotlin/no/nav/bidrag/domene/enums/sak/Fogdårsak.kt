package no.nav.bidrag.domene.enums.sak

enum class Fogdårsak(
    val beskrivelse: String,
    val gyldig: Boolean,
    val tilgangstype: Tilgangstype,
    val tilgangsrolle: String,
) {
    ADRE("Adresseendring", true, Tilgangstype.EIER, "Batch"),
    AUTO("Bidragsevnesprekk", true, Tilgangstype.MIDL, "Alle"),
    BRUS("Brukerstøtte", false, Tilgangstype.MIDL, "Ingen"),
    DISK("Diskresjon", true, Tilgangstype.EIER, "Batch"),
    EIER("Annet", true, Tilgangstype.EIER, "Alle"),
    EIUT("Utlandssak", true, Tilgangstype.EIER, "Alle"),
    ERST("Erstatning", true, Tilgangstype.MIDL, "Alle"),
    KLIN("Klageinstans", true, Tilgangstype.MIDL, "Alle"),
    MAAN("Manuell annet", true, Tilgangstype.MIDL, "Alle"),
    MAKO("Ko-fogd", true, Tilgangstype.MIDL, "Alle"),
    MAUT("Utlandssak", true, Tilgangstype.MIDL, "Alle"),
    MOT("Motregning", true, Tilgangstype.MIDL, "Alle"),
    OORG("Omorganisering", true, Tilgangstype.EIER, "Batch"),
}
