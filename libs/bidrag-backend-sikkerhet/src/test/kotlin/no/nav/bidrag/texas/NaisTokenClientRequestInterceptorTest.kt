package no.nav.bidrag.texas

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

class NaisTokenClientRequestInterceptorTest {
    private val target = "api://target/.default"
    private val tokenService = mockk<NaisTokenService>()
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()

    @Test
    fun `obo-interceptor legger utvekslet token i Authorization-headeren`() {
        every { tokenService.oboToken(target) } returns "obo-token"
        val client = RestClient.builder(restTemplate)
            .requestInterceptor(NaisTokenClientRequestInterceptor(tokenService, target))
            .build()
        server.expect(requestTo("http://downstream/resource"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer obo-token"))
            .andRespond(withStatus(HttpStatus.OK))

        client.get().uri("http://downstream/resource").retrieve().toBodilessEntity()

        server.verify()
        verify(exactly = 1) { tokenService.oboToken(target) }
    }

    @Test
    fun `m2m-interceptor legger applikasjonstoken i Authorization-headeren`() {
        every { tokenService.m2mToken(target) } returns "m2m-token"
        val client = RestClient.builder(restTemplate)
            .requestInterceptor(M2mNaisTokenClientRequestInterceptor(tokenService, target))
            .build()
        server.expect(requestTo("http://downstream/resource"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer m2m-token"))
            .andRespond(withStatus(HttpStatus.OK))

        client.get().uri("http://downstream/resource").retrieve().toBodilessEntity()

        server.verify()
        verify(exactly = 1) { tokenService.m2mToken(target) }
    }
}
