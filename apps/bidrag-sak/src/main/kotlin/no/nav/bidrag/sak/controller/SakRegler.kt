package no.nav.bidrag.sak.controller

/**
 * Regler for opprettelse og endring av sak, til Springdoc-beskrivelsen av endepunktene.
 * Samme regler står i README for bidrag-sak.
 */
object SakRegler {
    const val ROLLEREGLER =
        "**Regler for saksroller**\n" +
            "- Alle roller med fødselsnummer i forespørselen (BP, BM, BA, RM og FR) må finnes i bidrag-person. " +
            "En RM som er samhandler, må finnes som samhandler.\n" +
            "- Fødselsnummer kan ikke være tom streng.\n" +
            "- Reell mottaker kan ikke ha tom ident.\n" +
            "- Minst én av rollene BP, BM eller BA må være knyttet til en kjent person.\n" +
            "- En person kan bare ha én rolle i saken, med unntak av RM og FR.\n" +
            "- Samme barn kan ikke sendes inn to ganger i én forespørsel.\n" +
            "- Saken kan ha maks én BM og én BP.\n" +
            "- RM kan bare registreres på barn.\n" +
            "- Barn fra og med 18 år må ha RM. Er fødselsdatoen ukjent etter oppslag, kreves ikke RM. " +
            "Feiler oppslaget, feiler forespørselen.\n" +
            "- Verge styres av `reellMottaker.verge`. Mangler reellMottaker, brukes `mottagerErVerge`, som er deprecated. " +
            "Verge lagres både på barnets rolle og på RM-rollen, fordi Bisys leser den fra RM-rollen. Verge krever reell mottaker og må være en person.\n" +
            "- Landkoden må finnes i kodeverket."

    const val LAGRINGSREGLER =
        "**Ved hver lagring**\n" +
            "- bidrag-sak bestemmer eierfogd fra arbeidsfordelingen i Norg og returnerer den i svaret. " +
            "Adressebeskyttelse (kode 6/7) hentes fra PDL og skjerming fra skjermingsregisteret, for alle nåværende og tidligere parter. " +
            "Norg får den strengeste av dem sammen med geografisk tilknytning og velger enheten. " +
            "Har saken arbeidsfordeling INH, brukes settekontoret til enheten, unntatt når en part har adressebeskyttelse eller skjerming. " +
            "Ved rolleendring skjer dette bare når toggelen `arbeidsflyt.flytt_oppgaver_ved_enhetsbytte` er på, slik at oppgavene flyttes med.\n" +
            "- bidrag-sak setter `ansatt` på saken: true når en nåværende eller tidligere part er skjermet i skjermingsregisteret, ellers false. " +
            "Bisys krever utvidede rettigheter for saker med flagget.\n" +
            "- `inhabilitet` settes bare i det gamle rollebildet i Bisys. Ny løsning endrer ikke flagget."

    const val OPPRETT_REGLER =
        "**Ved opprettelse**\n" +
            "- Har samme BP og BM en sak med arbeidsfordeling EEN fra før, returneres den saken, og det opprettes ingen ny. " +
            "Mangler saksbehandleren tilgang til den saken, svarer endepunktet 403 uten data.\n" +
            "- Forespørselen har ikke `eierfogd`, `ansatt` eller `inhabilitet`. Sendes feltene, ignoreres de."

    const val ENDRE_REGLER =
        "**Ved endring**\n" +
            "- Saksbehandleren må ha tilgang til saken. Ellers svarer endepunktet 403.\n" +
            "- Kjente BP-, BM- og BA-roller kan ikke fjernes eller endres. RM kan endres.\n" +
            "- Bare roller i forespørselen valideres. Lagrede roller som ikke er med, sjekkes ikke på nytt, " +
            "heller ikke om de finnes i bidrag-person eller om myndige barn mangler RM.\n" +
            "- FR kan ikke endres. FR-roller i forespørselen ignoreres.\n" +
            "- bidrag-sak bruker gjeldende ident fra bidrag-person for alle roller unntatt FR, både i forespørselen og for lagrede roller. " +
            "Gjeldende ident er folkeregisteridenten, eller NPID hvis personen ikke har folkeregisterident. " +
            "Identbytter på lagrede roller lagres i rollehistorikken som `ENDRE_FNR`. Historisk og gjeldende ident regnes som samme person.\n" +
            "- Forespørselen har ikke `ansatt` eller `inhabilitet`. Sendes feltene, ignoreres de."

    private const val AVSNITT = "\n\n"

    const val OPPRETT = OPPRETT_REGLER + AVSNITT + ROLLEREGLER + AVSNITT + LAGRINGSREGLER
    const val ENDRE = ENDRE_REGLER + AVSNITT + ROLLEREGLER + AVSNITT + LAGRINGSREGLER
}
