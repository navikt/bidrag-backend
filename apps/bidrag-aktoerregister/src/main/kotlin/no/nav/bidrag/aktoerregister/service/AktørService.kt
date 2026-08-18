package no.nav.bidrag.aktoerregister.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import no.nav.bidrag.aktoerregister.consumer.PersonConsumer
import no.nav.bidrag.aktoerregister.consumer.SamhandlerConsumer
import no.nav.bidrag.aktoerregister.dto.AktoerDTO
import no.nav.bidrag.aktoerregister.dto.AktoerIdDTO
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.exception.AktørNotFoundException
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.repository.AktørRepository
import no.nav.bidrag.aktoerregister.persistence.repository.TidligereIdenterRepository
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.transport.samhandler.SamhandlerSøk
import no.nav.bidrag.transport.samhandler.SamhandlersøkeresultatDto
import org.springframework.core.convert.ConversionService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
class AktørService(
    private val aktørRepository: AktørRepository,
    private val tidligereIdenterRepository: TidligereIdenterRepository,
    private val hendelseService: HendelseService,
    private val duplikatHåndteringService: DuplikathåndteringService,
    private val aktørendringstracker: Aktørendringstracker,
    private val samhandlerConsumer: SamhandlerConsumer,
    private val personConsumer: PersonConsumer,
    private val conversionService: ConversionService,
    private val entityManager: EntityManager,
) {

    @Transactional
    fun hentAktoer(aktørId: AktoerIdDTO, tvingOppdatering: Boolean): AktoerDTO {
        val aktørIdent = Ident(aktørId.aktoerId)
        val aktørFraDatabase = hentAktørFraDatabase(aktørIdent)
        var aktør = aktørFraDatabase.first

        if (aktør == null) {
            aktør = hentOgOpprettNyAktør(aktørId, aktørIdent)
            return aktør.tilDto()
        }

        if (tvingOppdatering || aktørFraDatabase.second) {
            val hentetAktør = if (aktørId.identtype == Identtype.AKTOERNUMMER) hentAktørFraSamhandler(aktørIdent) else hentAktørFraPerson(aktørIdent)
            if (aktør != hentetAktør) {
                oppdaterAktør(aktør, hentetAktør, aktørId.aktoerId)
            }
            return hentetAktør.tilDto()
        }

        return aktør.tilDto()
    }

    private fun hentOgOpprettNyAktør(aktørId: AktoerIdDTO, aktørIdent: Ident): Aktør = if (aktørId.identtype == Identtype.AKTOERNUMMER) {
        hentOgLagreSamhandler(aktørIdent)
    } else {
        hentOgLagrePerson(aktørIdent)
    }

    private fun hentOgLagreSamhandler(aktørIdent: Ident): Aktør {
        hentAktørFraSamhandler(aktørIdent).let {
            lagreNyAktør(it)
            return it
        }
    }

    fun hentAktørFraSamhandler(aktørIdent: Ident): Aktør {
        val samhandler = samhandlerConsumer.hentSamhandler(aktørIdent)
            ?: throw AktørNotFoundException("Aktør ikke funnet i bidrag-samhandler.")
        return conversionService.convert(samhandler, Aktør::class.java)
            ?: error("Konvertering av samhandler til aktør for ident: $aktørIdent feilet!")
    }

    private fun hentOgLagrePerson(personIdent: Ident): Aktør {
        LOGGER.debug { "Aktør ikke funnet i databasen. Henter aktør fra bidrag-person" }
        val nyAktør = hentAktørFraPerson(personIdent)

        if (nyAktør.tidligereIdenter.isNotEmpty()) {
            // Finn alle aktører som matcher noen av de tidligere identene
            val tidligereIdenter = nyAktør.tidligereIdenter.map { it.tidligereAktoerIdent }.toSet()
            val matchendeAktører = duplikatHåndteringService.finnAlleMatchendeAktører(tidligereIdenter)

            if (matchendeAktører.isNotEmpty()) {
                // Bruk den eldste aktøren (laveste id) som den primære
                val primærAktør = duplikatHåndteringService.velgPrimærAktør(matchendeAktører)
                val originalIdent = primærAktør.aktørIdent

                // Slett eventuelle duplikater
                duplikatHåndteringService.slettDuplikater(primærAktør, matchendeAktører)

                // Oppdater primær aktør med ny informasjon
                oppdaterAktør(primærAktør, nyAktør, originalIdent)
                return primærAktør
            }
        }

        lagreNyAktør(nyAktør)
        return nyAktør
    }

    fun hentAktørFraPerson(personIdent: Ident): Aktør {
        val person = personConsumer.hentPerson(personIdent)
            ?: throw AktørNotFoundException("Aktør ikke funnet i bidrag-person.")
        return conversionService.convert(person, Aktør::class.java)
            ?: error("Konvertering av person til aktør feilet!")
    }

    fun hentAktørFraDatabase(aktørIdent: Ident): Pair<Aktør?, Boolean> {
        // Boolean-verdien indikerer om vi må tvinge en oppdatering etter å ha funnet duplikat aktør via tidligere identer
        var tvingOppdatering = false
        // Først sjekk om det finnes en aktør med denne identen direkte
        val direkteAktør = aktørRepository.findByAktørIdent(aktørIdent.verdi)
        if (direkteAktør != null) return Pair(direkteAktør, false)

        // Hvis ikke, sjekk om denne identen finnes som tidligere ident
        val tidligereIdenter = tidligereIdenterRepository.findByTidligereAktoerIdent(aktørIdent.verdi)

        if (tidligereIdenter.isEmpty()) return Pair(null, false)

        // Hvis det finnes flere aktører med samme tidligere ident (duplikater),
        // velg den eldste (laveste id)
        val matchendeAktører = tidligereIdenter.mapNotNull { it.aktør }.distinctBy { it.id }

        if (matchendeAktører.size > 1) {
            secureLogger.warn {
                "Fant ${matchendeAktører.size} aktører med tidligere ident ${aktørIdent.verdi}. " +
                    "Aktør-IDer: ${matchendeAktører.map { it.id }.joinToString(", ")}. " +
                    "Velger eldste aktør (id=${matchendeAktører.minBy { it.id!! }.id})."
            }
            tvingOppdatering = true
        }

        return Pair(matchendeAktører.minBy { it.id!! }, tvingOppdatering)
    }

    fun oppdaterAktør(aktør: Aktør, nyAktør: Aktør, originalIdent: String?) {
        try {
            var primærAktør = aktør

            // Sjekk for duplikat aktør hvis aktørIdent har endret seg
            if (aktør.aktørIdent != nyAktør.aktørIdent) {
                val duplikatAktør = hentAktørFraDatabase(Ident(nyAktør.aktørIdent)).first
                if (duplikatAktør != null) {
                    secureLogger.warn { "Fant duplikat aktør for ident: ${nyAktør.aktørIdent}. Starter duplikathåndtering." }
                    val matchendeAktører = listOf(primærAktør, duplikatAktør)
                    primærAktør = duplikatHåndteringService.velgPrimærAktør(matchendeAktører)
                    duplikatHåndteringService.slettDuplikater(primærAktør, matchendeAktører)
                }
            }
            // Track endringer mellom gammel og ny aktør
            val endringer = aktørendringstracker.utledEndringer(primærAktør, nyAktør)

            // Fjern eksisterende tidligere identer
            fjernEksisterendeTidligereIdenter(primærAktør)

            // Oppdater alle felter på aktøren
            primærAktør.oppdaterAlleFelter(nyAktør)
            entityManager.flush()

            // Sett nye relasjoner
            settNyeTidligereIdenter(primærAktør)
            settDødsbo(primærAktør)

            // Opprett hendelser for endringene
            hendelseService.opprettHendelserPåAktør(primærAktør, originalIdent, endringer)

            secureLogger.debug { "Lagrer aktør: ${primærAktør.aktørIdent}" }
            aktørRepository.save(primærAktør)
        } catch (e: Exception) {
            secureLogger.error(e) {
                "Ukjent feil for ident: ${aktør.aktørIdent}. Original ident: $originalIdent. " +
                    "\nFeil: ${e.message} \nStacktrace: ${e.stackTraceToString()}"
            }
            throw e
        }
    }

    fun lagreNyAktør(aktør: Aktør) {
        try {
            aktørRepository.save(aktør)
            settNyeTidligereIdenter(aktør)
            settDødsbo(aktør)

            val felter = aktørendringstracker.trackNyAktør(aktør)
            hendelseService.opprettHendelserPåAktør(aktør, null, felter)
        } catch (e: DataIntegrityViolationException) {
            secureLogger.error(e) { "DataIntegrityViolationException for ident: ${aktør.aktørIdent}. \nFeil: $e " }
            throw e
        }
    }

    private fun fjernEksisterendeTidligereIdenter(aktør: Aktør) {
        aktør.tidligereIdenter.forEach {
            tidligereIdenterRepository.delete(it)
        }
    }

    private fun settDødsbo(aktør: Aktør) {
        aktør.dødsbo?.aktør = aktør
    }

    private fun settNyeTidligereIdenter(aktør: Aktør) {
        aktør.tidligereIdenter.forEach {
            it.aktør = aktør
        }
    }

    @Transactional
    fun slettAktoer(aktoerIdDTO: AktoerIdDTO) {
        aktørRepository.deleteAktørByAktørIdent(aktoerIdDTO.aktoerId)
    }

    fun samhandlerSøk(samhandlerSøk: SamhandlerSøk): SamhandlersøkeresultatDto = samhandlerConsumer.samhandlerSøk(samhandlerSøk)
        ?: throw AktørNotFoundException("Aktør ikke funnet i bidrag-samhandler under søk.")

    fun Aktør.tilDto(): AktoerDTO = (
        conversionService.convert(this, AktoerDTO::class.java)
            ?: error("Konvertering av aktør til AktoerDTO feilet!")
        )
}
