package no.nav.bidrag.sak.service

import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode
import no.nav.bidrag.sak.config.UnleashFeatures
import no.nav.bidrag.sak.domain.Rolle
import no.nav.bidrag.sak.integration.kodeverk.CachedKodeverkService
import no.nav.bidrag.sak.integration.person.BidragPersonClient
import no.nav.bidrag.sak.validering.Valideringsgrunnlag
import no.nav.bidrag.transport.sak.OpprettSakRequest
import no.nav.bidrag.transport.sak.RolleDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ValideringsgrunnlagService(
    private val identConsumer: IdentConsumer,
    private val bidragPersonClient: BidragPersonClient,
    private val cachedKodeverkService: CachedKodeverkService,
) {
    fun hentForOpprettelse(opprettSakRequest: OpprettSakRequest): Valideringsgrunnlag = hent(opprettSakRequest.roller, emptyList(), opprettSakRequest.land)

    fun hentForEndring(lagredeRoller: Collection<Rolle>, forespurteRoller: Collection<RolleDto>, land: Landkode?): Valideringsgrunnlag = hent(forespurteRoller, lagredeRoller.mapNotNull { it.fødselsnummer }, land)

    private fun hent(forespurteRoller: Collection<RolleDto>, lagredeIdenter: List<String>, land: Landkode?) = Valideringsgrunnlag(
        personer = hentPersoner(forespurteRoller),
        fødselsdatoer = hentFødselsdatoer(forespurteRoller),
        identer = hentIdenter(forespurteRoller.mapNotNull { it.fødselsnummer?.verdi } + lagredeIdenter),
        landkoder = if (land == null) emptySet() else cachedKodeverkService.hentLandkoder().keys,
        tillatEksisterendeDobleRoller = UnleashFeatures.TILLAT_EKSISTERENDE_DOBLE_SAKSROLLER.isEnabled,
    )

    private fun hentPersoner(roller: Collection<RolleDto>): Map<String, Valideringsgrunnlag.Person> = ikkeBlanke(roller.mapNotNull { it.fødselsnummer })
        .mapNotNull { fnr -> identConsumer.hentPersonInformasjon(fnr)?.let { fnr.verdi to Valideringsgrunnlag.Person(it.fødselsdato) } }
        .toMap()

    private fun hentFødselsdatoer(roller: Collection<RolleDto>): Map<Personident, LocalDate?> {
        val fødselsnumre = ikkeBlanke(roller.mapNotNull { it.fødselsnummer } + roller.mapNotNull { it.rmFødselsnummer() })
        return if (fødselsnumre.isEmpty()) emptyMap() else bidragPersonClient.hentFødselsdatoer(fødselsnumre)
    }

    private fun hentIdenter(identer: List<String>): Map<String, Set<String>> = identer
        .filter { it.isNotBlank() }
        .distinct()
        .associateWith { bidragPersonClient.hentAlleIdenter(it) }

    private fun ikkeBlanke(identer: List<Personident>): List<Personident> = identer.filter { it.verdi.isNotBlank() }.distinct()
}
