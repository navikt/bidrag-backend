package no.nav.bidrag.dokument.journalpost.model

private val FAGOMRADE_BIDRAG_ELLER_FARSKAP = setOf(Fagomrade.BIDRAG_DATABASE, Fagomrade.BIDRAG, Fagomrade.FARSKAP)

fun erForBrevlager(fagomrade: String?) = FAGOMRADE_BIDRAG_ELLER_FARSKAP.contains(fagomrade)

fun erForAnnetEnnBrevlager(fagomrade: String?) = !erForBrevlager(fagomrade)

fun fraDatabase(fagomrade: String?) = if (Fagomrade.BIDRAG_DATABASE == fagomrade) Fagomrade.BIDRAG else fagomrade

fun tilDatabase(fagomrade: String?) = if (Fagomrade.BIDRAG == fagomrade) Fagomrade.BIDRAG_DATABASE else fagomrade

fun erFor(
    fagomrade: String?,
    fagomradeFraDatabase: String?,
): Boolean {
    if (Fagomrade.BIDRAG == fagomrade) {
        return Fagomrade.BIDRAG_DATABASE == fagomradeFraDatabase
    }

    return fagomrade == fagomradeFraDatabase
}

fun erUlike(
    fagomradeFraDatabase: String?,
    fagomradeTilDatabase: String?,
): Boolean {
    val fagomradeSomSkrives = tilDatabase(fagomradeTilDatabase)

    return fagomradeSomSkrives != fagomradeFraDatabase
}
