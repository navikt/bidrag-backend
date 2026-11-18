package no.nav.bidrag.sak.service

import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.validering.Valideringsgrunnlag
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service

@Service
class ValideringsgrunnlagService(
    private val identConsumer: IdentConsumer,
    private val cachedKodeverkService: CachedKodeverkService,
) {
    fun hentForOpprettelse(opprettSakRequest: OpprettSakRequest): Valideringsgrunnlag = hent(opprettSakRequest.roller, emptyList(), opprettSakRequest.land)

    fun hentForEndring(lagredeRoller: Collection<Rolle>, forespurteRoller: Collection<RolleDto>, land: Landkode?): Valideringsgrunnlag = hent(forespurteRoller, lagredeRoller.mapNotNull { it.fødselsnummer }, land)

    private fun hent(forespurteRoller: Collection<RolleDto>, lagredeIdenter: List<String>, land: Landkode?): Valideringsgrunnlag {
        val personinformasjon = hentPersoninformasjon(forespurteRoller)
        return Valideringsgrunnlag(
            personer = personer(forespurteRoller, personinformasjon),
            fødselsdatoer = personinformasjon.mapNotNull { (ident, person) -> person?.let { ident to it.fødselsdato } }.toMap(),
            identer = hentIdenter(forespurteRoller.mapNotNull { it.fødselsnummer?.verdi } + lagredeIdenter),
            landkoder = if (land == null) emptySet() else cachedKodeverkService.hentLandkoder().keys,
        )
    }

    private fun hentPersoninformasjon(roller: Collection<RolleDto>): Map<Personident, PersonDto?> = ikkeBlanke(roller.mapNotNull { it.fødselsnummer } + roller.mapNotNull { it.rmFødselsnummer() })
        .associateWith { identConsumer.hentPersonInformasjon(it) }

    private fun personer(roller: Collection<RolleDto>, personinformasjon: Map<Personident, PersonDto?>): Map<String, Valideringsgrunnlag.Person> = ikkeBlanke(roller.mapNotNull { it.fødselsnummer })
        .mapNotNull { fnr -> personinformasjon[fnr]?.let { fnr.verdi to Valideringsgrunnlag.Person(it.fødselsdato) } }
        .toMap()

    private fun hentIdenter(identer: List<String>): Map<String, Set<String>> = identer
        .filter { it.isNotBlank() }
        .distinct()
        .associateWith { ident -> identConsumer.hentAlleIdenter(ident).orEmpty().toSet() }

    private fun ikkeBlanke(identer: List<Personident>): List<Personident> = identer.filter { it.verdi.isNotBlank() }.distinct()
}
