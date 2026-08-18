package no.nav.bidrag.aktoerregister.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.aktoerregister.dto.AktoerIdDTO
import no.nav.bidrag.aktoerregister.dto.HendelseDTO
import no.nav.bidrag.aktoerregister.dto.enumer.Hendelsestype
import no.nav.bidrag.aktoerregister.dto.enumer.Identtype
import no.nav.bidrag.aktoerregister.persistence.entities.Aktør
import no.nav.bidrag.aktoerregister.persistence.entities.Hendelse
import no.nav.bidrag.aktoerregister.persistence.repository.HendelseRepository
import no.nav.bidrag.aktoerregister.persistence.repository.SekvensnummerOgIdent
import no.nav.bidrag.domene.ident.Ident
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger { }

@Service
class HendelseService(
    private val hendelseRepository: HendelseRepository,
) {

    fun hentHendelser(sekvensunummer: Int, antallHendelser: Int): List<HendelseDTO> {
        LOGGER.info { "HENDELSE: Henter hendelser fra sekvensnummer: $sekvensunummer. Antall hendelser: $antallHendelser." }
        val hendelser = hendelseRepository.hentAlleHendelserMedSekvensnummerOgIdent(sekvensunummer, antallHendelser)

        val hendelseDTOer = hendelser
            .sortedByDescending { it.sekvensnummer }
            .distinctBy { it.aktoer_ident }
            .map {
                HendelseDTO(
                    sekvensnummer = it.sekvensnummer,
                    aktoerId = AktoerIdDTO(
                        aktoerId = it.aktoer_ident,
                        identtype = finnIdenttype(it),
                    ),
                )
            }.sortedBy { it.sekvensnummer }
        if (hendelseDTOer.isNotEmpty()) {
            LOGGER.info { "HENDELSE: Hendelser fra sekvensnummer ${hendelseDTOer.first().sekvensnummer} til ${hendelseDTOer.last().sekvensnummer} utlevert." }
        } else {
            LOGGER.info { "HENDELSE: Ingen nye hendelser fra sekvensnummer $sekvensunummer." }
        }
        return hendelseDTOer
    }

    private fun finnIdenttype(sekvensnummerOgIdent: SekvensnummerOgIdent): Identtype = if (Ident(sekvensnummerOgIdent.aktoer_ident).erPersonIdent()) {
        Identtype.PERSONNUMMER
    } else {
        Identtype.AKTOERNUMMER
    }

    fun opprettHendelserPåAktør(aktør: Aktør, originalIdent: String?, endringer: Set<Hendelsestype> = emptySet()) {
        if (originalIdent != null && originalIdent != aktør.aktørIdent) {
            val hendelse = Hendelse(
                aktørIdent = originalIdent,
                aktør = aktør,
                kontonummerOppdatering = endringer.contains(Hendelsestype.KONTONUMMER_OPPDATERING),
                identOppdatering = endringer.contains(Hendelsestype.IDENT_OPPDATERING),
                navnOppdatering = endringer.contains(Hendelsestype.NAVN_OPPDATERING),
                adresseOppdatering = endringer.contains(Hendelsestype.ADRESSE_OPPDATERING),
                fødtDatoOppdatering = endringer.contains(Hendelsestype.FODT_DATO_OPPDATERING),
                dødDatoOppdatering = endringer.contains(Hendelsestype.DOD_DATO_OPPDATERING),
                graderingOppdatering = endringer.contains(Hendelsestype.GRADERING_OPPDATERING),
                dødsboOppdatering = endringer.contains(Hendelsestype.DODSBO_OPPDATERING),
                språkOppdatering = endringer.contains(Hendelsestype.SPRAK_OPPDATERING),
            )
            aktør.addHendelse(hendelse)
        }
        aktør.addHendelse(
            Hendelse(
                aktørIdent = aktør.aktørIdent,
                aktør = aktør,
                kontonummerOppdatering = endringer.contains(Hendelsestype.KONTONUMMER_OPPDATERING),
                identOppdatering = endringer.contains(Hendelsestype.IDENT_OPPDATERING),
                navnOppdatering = endringer.contains(Hendelsestype.NAVN_OPPDATERING),
                adresseOppdatering = endringer.contains(Hendelsestype.ADRESSE_OPPDATERING),
                fødtDatoOppdatering = endringer.contains(Hendelsestype.FODT_DATO_OPPDATERING),
                dødDatoOppdatering = endringer.contains(Hendelsestype.DOD_DATO_OPPDATERING),
                graderingOppdatering = endringer.contains(Hendelsestype.GRADERING_OPPDATERING),
                dødsboOppdatering = endringer.contains(Hendelsestype.DODSBO_OPPDATERING),
                språkOppdatering = endringer.contains(Hendelsestype.SPRAK_OPPDATERING),
            ),
        )
    }
}
