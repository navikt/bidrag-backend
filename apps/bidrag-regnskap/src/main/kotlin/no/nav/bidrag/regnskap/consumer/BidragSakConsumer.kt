package no.nav.bidrag.regnskap.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.transport.sak.BidragssakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations

@Service
class BidragSakConsumer(
    @param:Value($$"${SAK_URL}") private val sakUrl: String,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-regnskap") {

    companion object {
        const val SAK_PATH = "/bidrag-sak/sak"
        const val DUMMY_NUMMER = "22222222226"
    }

    @Cacheable(value = ["bidrag-sak_cache"], key = "#sakId")
    fun hentBmFraSak(sakId: String): String {
        val requestUrl = "$sakUrl$SAK_PATH/$sakId"
        val bidragssak = restTemplate.getForEntity(requestUrl, BidragssakDto::class.java).body

        return bidragssak?.roller
            ?.find { it.type == Rolletype.BIDRAGSMOTTAKER }
            ?.fødselsnummer?.verdi
            ?: DUMMY_NUMMER
    }
}
