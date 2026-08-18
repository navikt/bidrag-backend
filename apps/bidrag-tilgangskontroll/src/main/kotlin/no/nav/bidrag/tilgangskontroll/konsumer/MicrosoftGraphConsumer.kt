package no.nav.bidrag.tilgangskontroll.konsumer

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.tilgangskontroll.konfigurasjon.Cache
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerEnheterRespons
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerGrupperResponse
import no.nav.bidrag.tilgangskontroll.model.graph.BrukerinformasjonResponse
import no.nav.bidrag.tilgangskontroll.model.graph.CheckMemberGroupsResponse
import no.nav.bidrag.tilgangskontroll.model.graph.EnhetResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class MicrosoftGraphConsumer(
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "tema-tilgang") {
    companion object {
        private val GRAPH_URL = URI("https://graph.microsoft.com/v1.0/")
    }

    @BrukerCacheable(Cache.BRUKERINFO)
    fun hentBrukerinformasjon(navident: String): BrukerinformasjonResponse? {
        val httpHeaders = HttpHeaders().apply { add("ConsistencyLevel", "eventual") }
        val uri =
            UriComponentsBuilder
                .fromUri(GRAPH_URL)
                .pathSegment("users")
                .queryParam($$"$filter", "onPremisesSamAccountName eq '$navident'")
                .queryParam($$"$count", "true")
                .build()
                .toUri()
        val response = getForEntity<BrukerinformasjonResponse>(uri, httpHeaders)
        return response
    }

    @BrukerCacheable(Cache.BRUKERGRUPPER)
    fun hentGrupperForBruker2(navident: String?): BrukerGrupperResponse? {
        if (TokenUtils.erApplikasjonsbruker()) {
            if (navident.isNullOrBlank()) {
                log.warn("Ingen navident oppgitt for applikasjonsbruker, kan ikke hente grupper.")
                return null
            }
            val id = hentBrukerinformasjon(navident)?.value?.first()?.id ?: error("Fant ikke id for bruker med navident $navident")
            val uri =
                UriComponentsBuilder
                    .fromUri(GRAPH_URL)
                    .pathSegment("users/$id/transitiveMemberOf")
                    .build()
                    .toUri()
            val response = getForEntity<BrukerGrupperResponse>(uri)
            return response
        } else {
            val uri =
                UriComponentsBuilder
                    .fromUri(GRAPH_URL)
                    .pathSegment("me/transitiveMemberOf")
                    .build()
                    .toUri()
            return getForEntity<BrukerGrupperResponse>(uri)
        }
    }

    @BrukerCacheable(Cache.BRUKERGRUPPER)
    fun hentGrupperForBruker(navident: String?): BrukerGrupperResponse? {
        if (TokenUtils.erApplikasjonsbruker()) {
            if (navident.isNullOrBlank()) {
                log.warn("Ingen navident oppgitt for applikasjonsbruker, kan ikke hente grupper.")
                return null
            }
            val id = hentBrukerinformasjon(navident)?.value?.first()?.id ?: error("Fant ikke id for bruker med navident $navident")
            val uri =
                UriComponentsBuilder
                    .fromUri(GRAPH_URL)
                    .pathSegment("users/$id/transitiveMemberOf")
                    .build()
                    .toUri()
            val response = getForEntity<BrukerGrupperResponse>(uri)
            return response
        } else {
            val uri =
                UriComponentsBuilder
                    .fromUri(GRAPH_URL)
                    .pathSegment("me/transitiveMemberOf")
                    .build()
                    .toUri()
            return getForEntity<BrukerGrupperResponse>(uri)
        }
    }

    @BrukerCacheable(Cache.BRUKERE_FOR_ENHET)
    fun hentBrukereForEnhet(enhet: String): BrukerinformasjonResponse? {
        val enhetId = hentEnhetId(enhet) ?: return null
        val temaBidId = hentTemaBidId() ?: return null
        val httpHeaders = HttpHeaders().apply { add("ConsistencyLevel", "eventual") }

        // Get all transitive members (users only) from the enhet group
        val uri =
            UriComponentsBuilder
                .fromUri(GRAPH_URL)
                .pathSegment("groups", enhetId, "transitiveMembers", "microsoft.graph.user")
                .queryParam("\$select", "id,displayName,givenName,surname,mail,officeLocation,jobTitle,onPremisesSamAccountName")
                .build()
                .toUri()

        val response = getForEntity<BrukerinformasjonResponse>(uri, httpHeaders)
        val allUsers = response?.value?.toMutableList() ?: mutableListOf()

        // Handle pagination
        if (response != null && response.nextLink != null) {
            var nextLink = response.nextLink
            while (!nextLink.isNullOrBlank()) {
                val nextResponse = getForEntity<BrukerinformasjonResponse>(URI.create(nextLink), httpHeaders)
                allUsers.addAll(nextResponse?.value ?: emptyList())
                nextLink = nextResponse?.nextLink
            }
        }

        // Filter users who are also members of TEMA_BID group
        val filteredUsers =
            allUsers.filter { user ->
                user.id != null && isMemberOfGroup(user.id, temaBidId)
            }

        return BrukerinformasjonResponse(value = filteredUsers, nextLink = null)
    }

    private fun isMemberOfGroup(
        userId: String,
        groupId: String,
    ): Boolean = try {
        val httpHeaders = HttpHeaders().apply { add("ConsistencyLevel", "eventual") }
        val uri =
            UriComponentsBuilder
                .fromUri(GRAPH_URL)
                .pathSegment("users", userId, "checkMemberGroups")
                .build()
                .toUri()

        val requestBody = mapOf("groupIds" to listOf(groupId))
        val response = postForEntity<CheckMemberGroupsResponse>(uri, requestBody, httpHeaders)
        response?.value?.contains(groupId) ?: false
    } catch (e: Exception) {
        log.warn("Feil ved sjekk av medlemskap for bruker $userId i gruppe $groupId", e)
        false
    }

    @BrukerCacheable(Cache.GRUPPE_DETALJER)
    fun hentEnhetId(enhet: String): String? {
        val httpHeaders = HttpHeaders().apply { add("ConsistencyLevel", "eventual") }
        val uri =
            UriComponentsBuilder
                .fromUri(GRAPH_URL)
                .pathSegment("groups")
                .queryParam("\$select", "id,displayName")
                .queryParam("\$orderby", "displayName")
                .queryParam("\$search", "\"displayName:0000-GA-ENHET-$enhet\"")
                .build()
                .toUri()
        val response = getForEntity<EnhetResponse>(uri, httpHeaders)
        return response?.value?.firstOrNull()?.id
    }

    @BrukerCacheable(Cache.GRUPPE_DETALJER_TEMA)
    fun hentTemaBidId(): String? {
        val httpHeaders = HttpHeaders().apply { add("ConsistencyLevel", "eventual") }
        val uri =
            UriComponentsBuilder
                .fromUri(GRAPH_URL)
                .pathSegment("groups")
                .queryParam("\$select", "id,displayName")
                .queryParam("\$filter", "displayName eq '0000-GA-TEMA_BID'")
                .build()
                .toUri()
        val response = getForEntity<EnhetResponse>(uri, httpHeaders)
        return response?.value?.firstOrNull()?.id
    }

    @BrukerCacheable(Cache.BRUKERENHETER)
    fun hentEnheterForBruker(navident: String): BrukerEnheterRespons {
        val grupper = hentGrupperForBruker(navident)
        val enheter =
            grupper?.value?.filter { it.navn!!.startsWith("0000-GA-ENHET_") }?.map { it.navn!!.split("0000-GA-ENHET_")[1] } ?: emptyList()
        return BrukerEnheterRespons(enheter)
    }
}
