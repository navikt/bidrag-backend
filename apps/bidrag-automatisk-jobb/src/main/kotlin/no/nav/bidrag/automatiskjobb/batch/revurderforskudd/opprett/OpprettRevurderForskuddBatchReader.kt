package no.nav.bidrag.automatiskjobb.batch.revurderforskudd.opprett

import no.nav.bidrag.automatiskjobb.persistence.entity.Barn
import no.nav.bidrag.automatiskjobb.persistence.repository.BarnRepository
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

/**
 * Custom [ItemReader] som grupperer [Barn]-rader per saksnummer før de sendes til prosessoren.
 *
 * En sak kan ha flere barn som alle mottar forskudd. [OpprettRevurderForskuddBatchProcessor]
 * forventer å motta alle barn tilhørende samme sak samlet i én liste, slik at det kan opprettes
 * én [no.nav.bidrag.automatiskjobb.persistence.entity.RevurderingForskudd] per sak – ikke per barn.
 *
 * Barn hentes side for side fra [BarnRepository.finnBarnSomSkalRevurdereForskuddEtter], sortert på
 * saksnummer og deretter id. Bruker keyset/seek-paginering hvor hver spørring filtrerer på `(saksnummer, id) > (sisteSaksnummer, sisteId)`,
 * der `sisteSaksnummer`/`sisteId` er nøkkelen til forrige rad som ble lest. Dette gjør lesingen
 * uavhengig av at rader andre steder i resultatsettet legges til/fjernes mens jobben kjører.
 *
 * Denne readeren benytter peek-mønster for å oppdage når saksnummeret skifter,
 * og returnerer da den innsamlede gruppen til prosessoren. Det peekede barnet lagres til neste kall
 * av [read], slik at ingen barn går tapt mellom sider.
 */
class OpprettRevurderForskuddBatchReader(
    private val barnRepository: BarnRepository,
    private val forskuddFremTilDato: LocalDate,
    private val pageSize: Int,
) : ItemReader<List<Barn>> {
    /**
     * Nøkkelen (saksnummer, id) til siste barn som er hentet fra databasen. Brukes som cursor for
     * neste side-spørring, slik at paginering skjer basert på verdier.
     */
    private var sisteSaksnummer: String = ""
    private var sisteId: Int = 0

    /**
     * Buffer med barn hentet fra siste side-spørring, men som ennå ikke er konsumert av [read].
     */
    private val buffer: ArrayDeque<Barn> = ArrayDeque()

    /**
     * Buffer for det første barnet i neste sak. Når vi oppdager at saksnummeret har skiftet,
     * lagres det nye barnet her slik at det ikke kastes, men brukes i neste kall til [read].
     */
    private var peeked: Barn? = null

    /**
     * Flagg som settes når databasen ikke har flere rader å lese.
     * Hindrer unødvendige kall til databasen etter at alle rader er behandlet.
     */
    private var exhausted = false

    /**
     * Henter neste side med barn fra databasen basert på keyset-cursoren (sisteSaksnummer, sisteId).
     */
    private fun hentNesteSide(): List<Barn> = barnRepository.finnBarnSomSkalRevurdereForskuddEtter(
        forskuddFremTilDato,
        sisteSaksnummer,
        sisteId,
        PageRequest.of(0, pageSize, Sort.by("saksnummer", "id")),
    )

    /**
     * Returnerer neste barn fra bufferet, og henter en ny side fra databasen om bufferet er tomt.
     * Oppdaterer keyset-cursoren fortløpende slik at neste side-spørring fortsetter der forrige slapp.
     */
    private fun nesteBarn(): Barn? {
        if (buffer.isEmpty() && !exhausted) {
            val side = hentNesteSide()
            if (side.isEmpty()) {
                exhausted = true
            } else {
                buffer.addAll(side)
            }
        }
        val barn = buffer.removeFirstOrNull() ?: return null
        sisteSaksnummer = barn.saksnummer
        sisteId = barn.id ?: sisteId
        return barn
    }

    /**
     * Returnerer alle barn for én sak som én liste, eller `null` når alle saker er behandlet.
     *
     * Algoritme:
     * 1. Hent første barn fra buffer (peek) eller databasen. Returner `null` om ingen flere finnes.
     * 2. Samle alle påfølgende barn med samme saksnummer i en gruppe.
     * 3. Når saksnummeret skifter, lagre det nye barnet i bufferet og returner den ferdige gruppen.
     */
    override fun read(): List<Barn>? {
        val first = peeked ?: nesteBarn() ?: return null
        peeked = null

        val gruppe = mutableListOf(first)
        val saksnummer = first.saksnummer

        while (true) {
            val neste = nesteBarn()
            when {
                // Ingen flere rader – avslutt gruppen
                neste == null -> break

                // Samme sak – legg til i pågående gruppe
                neste.saksnummer == saksnummer -> gruppe.add(neste)

                // Nytt saksnummer – lagre barnet til neste kall og avslutt gruppen
                else -> {
                    peeked = neste
                    break
                }
            }
        }

        return gruppe
    }
}
