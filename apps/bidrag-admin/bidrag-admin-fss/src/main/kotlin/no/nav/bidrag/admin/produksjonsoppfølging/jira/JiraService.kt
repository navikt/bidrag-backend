package no.nav.bidrag.admin.produksjonsoppfølging.jira

import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.CodeField
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.Comment
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.EditIssue
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.Issue
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.JiraSak
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.SearchResponse
import no.nav.bidrag.admin.produksjonsoppfølging.jira.dto.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject
import java.util.Collections.emptyList

@Service
class JiraService(
    @param:Value($$"${JIRA_URL}") private val jiraUrl: String,
    @param:Value($$"${JIRA_PASSWORD}") private val jiraPassword: String,
) {
    private val restTemplate =
        RestTemplateBuilder()
            .interceptors({ request, body, execution ->
                request.headers.add("Authorization", "Bearer $jiraPassword")
                execution.execute(request, body)
            })
            .build()

    fun finnFagsystemsaker(summary: String): MutableList<String?> {
        val jql = "project=FAGSYSTEM and summary~\"$summary\" AND resolution in (Unresolved)"

        val response: SearchResponse? =
            restTemplate.getForObject<SearchResponse>(
                "$jiraUrl/api/2/search?fieldsByKeys=true&fields=issue&jql={jql}",
                jql,
            )

        return response?.issues?.map { it.key }?.toMutableList() ?: emptyList()
    }

    fun opprettFagsystemsak(
        summary: String,
        beskrivelse: String,
        assignee: String?,
    ): String {
        val request =
            no.nav.bidrag.admin.produksjonsoppfølging.jira.dto
                .OpprettSakRequest()
        request.fields["project"] =
            CodeField
                .withKey("FAGSYSTEM")
        request.fields["issuetype"] =
            CodeField
                .withId("13101")
        request.fields["customfield_20813"] =
            listOf(
                CodeField
                    .withKey("CMDB-314134"),
            )
        request.fields["customfield_20768"] =
            listOf(
                CodeField
                    .withKey("CMDB-32629"),
            )
        request.fields["customfield_20730"] =
            listOf(
                CodeField
                    .withKey("CMDB-801"),
            )
        request.fields["customfield_21414"] =
            CodeField
                .withId("25672")
        request.fields["customfield_21417"] =
            CodeField
                .withId("25684")
        request.fields["summary"] = summary
        request.fields["description"] = beskrivelse
        if (assignee != null) {
            request.fields["assignee"] =
                User
                    .withName(assignee)
        }

        val issue: Issue? = restTemplate.postForObject<Issue>("$jiraUrl/api/2/issue", request)
        return issue?.key ?: error("Kunne ikke opprette sak i Jira")
    }

    fun leggInnKommentar(
        issue: String,
        kommentar: String?,
    ) {
        val request = Comment()
        request.body = kommentar

        restTemplate.postForObject<Comment>(
            "$jiraUrl/api/2/issue/{issueIdOrKey}/comment",
            request,
            issue,
        )
    }

    fun finnÅpnePortenSakerUtenSlackLabel(): MutableList<JiraSak> {
        val jql = "filter=51431 AND issuetype != 16201 AND (labels is EMPTY OR labels != Slack)"

        val response =
            restTemplate.getForObject<SearchResponse>(
                "$jiraUrl/api/2/search?fieldsByKeys=true&fields=issue,summary&jql={jql}",
                jql,
            )

        return response?.issues?.map { JiraSak(it.key, it.fields?.summary) }?.toMutableList() ?: emptyList()
    }

    fun addLabel(
        issueKey: String,
        label: String,
    ) {
        val request = EditIssue()
        request.update.addLabel(label)

        restTemplate.put("$jiraUrl/api/2/issue/{issueKey}", request, issueKey)
    }
}
