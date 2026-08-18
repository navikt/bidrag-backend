package no.nav.bidrag.aktoerregister.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.aktoerregister.persistence.repository.HendelseRepository
import no.nav.bidrag.aktoerregister.persistence.repository.TidligereIdenterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
class DuplikathåndteringService(
    private val aktørRepository: AktørRepository,
    private val hendelseRepository: HendelseRepository,
    private val tidligereIdenterRepository: TidligereIdenterRepository,
    private val entityManager: EntityManager,
) {

    /**
     * Finner alle aktører i databasen som matcher noen av de tidligere identene.
     * Returnerer en liste med unike aktører sortert etter id (eldste først).
     */
    fun finnAlleMatchendeAktører(tidligereIdenter: Set<String>): List<Aktør> {
        if (tidligereIdenter.isEmpty()) return emptyList()

        val aktørerFraAktørIdent = tidligereIdenter.mapNotNull { ident ->
            aktørRepository.findByAktørIdent(ident)
        }

        val aktørerFraTidligereIdenter = tidligereIdenter.flatMap { ident ->
            tidligereIdenterRepository.findByTidligereAktoerIdent(ident).mapNotNull { it.aktør }
        }

        return (aktørerFraAktørIdent + aktørerFraTidligereIdenter)
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    /**
     * Velger den primære aktøren fra en liste av matchende aktører.
     * Returnerer den eldste aktøren (laveste id) som den primære.
     */
    fun velgPrimærAktør(matchendeAktører: List<Aktør>): Aktør = matchendeAktører.minBy { it.id!! }

    fun finnDuplikater(): List<String> = aktørRepository.finnDuplikateIdenter()

    /**
     * Rydd opp duplikate aktører ved å rydde de som deler samme ident.
     * Beholder den aktøren som sist ble oppdatert, og fjerner resten.
     */
    @Transactional
    fun ryddOppDuplikater() {
        val duplikateIdenter = aktørRepository.finnDuplikateIdenter()
        if (duplikateIdenter.isEmpty()) {
            LOGGER.info { "Ingen duplikate identer funnet for opprydding." }
            return
        }

        val behandledeAktører = mutableSetOf<Int>()

        for (ident in duplikateIdenter) {
            val matchendeAktører = finnAlleMatchendeAktører(setOf(ident))
                .filter { it.id!! !in behandledeAktører }

            if (matchendeAktører.size > 1) {
                // Beholder den nyest oppdaterte aktøren
                val primærAktør = velgSistOppdaterteAktør(matchendeAktører)

                LOGGER.info {
                    "Rydder opp i duplikate aktører for ident. Beholder aktør id: ${primærAktør.id} " +
                        "sist endret: ${primærAktør.sistEndret}. Duplikater: ${matchendeAktører.filter { it.id != primærAktør.id }.map { it.id }}"
                }

                slettDuplikater(primærAktør, matchendeAktører)
                behandledeAktører.addAll(matchendeAktører.map { it.id!! })
            }
        }
    }

    fun velgSistOppdaterteAktør(matchendeAktører: List<Aktør>): Aktør = matchendeAktører.maxBy { it.sistEndret?.time ?: it.id!!.toLong() }

    /**
     * Sletter alle duplikat-aktører bortsett fra den primære.
     * Sletter også alle relaterte hendelser og tidligere identer.
     */
    fun slettDuplikater(primærAktør: Aktør, alleMachendeAktører: List<Aktør>) {
        val duplikater = alleMachendeAktører.filter { it.id != primærAktør.id }

        if (duplikater.isEmpty()) return

        duplikater.forEach { duplikat ->
            LOGGER.info { "Sletter duplikat aktør med ident: ${duplikat.aktørIdent}, id: ${duplikat.id}" }
            slettAktør(duplikat)
        }

        entityManager.flush()
    }

    /**
     * Sletter en aktør med alle relaterte entiteter.
     */
    private fun slettAktør(aktør: Aktør) {
        // Slett hendelser manuelt for å unngå cascade-problemer
        aktør.hendelser.forEach { hendelse ->
            hendelseRepository.delete(hendelse)
        }

        // Slett tidligere identer manuelt
        aktør.tidligereIdenter.forEach { tidligereIdent ->
            tidligereIdenterRepository.delete(tidligereIdent)
        }

        // Slett selve aktøren
        aktørRepository.delete(aktør)
    }
}
