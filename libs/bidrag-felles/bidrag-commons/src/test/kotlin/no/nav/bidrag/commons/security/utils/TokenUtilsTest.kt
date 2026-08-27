package no.nav.bidrag.commons.security.utils

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.commons.security.SikkerhetsKontekst.medApplikasjonKontekst
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

internal class TokenUtilsTest {
    // Generated using http://jwtbuilder.jamiekurtz.com/
    private val issoUser =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2lzc28tcS5hZGVvLm5vOjQ0My9pc3NvL29hdXRoMiIsImlh" +
            "dCI6MTY1NTg3NzQyNCwiZXhwIjoxNjg3NDEzNDI0LCJhdWQiOiJiaWRyYWctdWktZmVhdHVyZS1xMSIsInN1YiI6Ilo5OTQ5N" +
            "zciLCJ0b2tlbk5hbWUiOiJpZF90b2tlbiIsImF6cCI6ImJpZHJhZy11aS1mZWF0dXJlLXExIn0.NYxxExStmzxqvjf-uKn7En" +
            "T9rOzluRxipclj0IH_0XQ"
    private val stsToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NlY3VyaXR5LXRva2VuLXNlcnZpY2UubmFpcy5wcmVwcm9k" +
            "LmxvY2FsIiwiaWF0IjoxNjU1ODc3NDI0LCJleHAiOjE2ODc0MTM0MjQsImF1ZCI6InNydmJpc3lzIiwic3ViIjoic3J2Ymlze" +
            "XMiLCJpZGVudFR5cGUiOiJTeXN0ZW1yZXNzdXJzIiwiYXpwIjoic3J2YmlzeXMifQ.ivpkYHclkl9z3fOfCSIMKKOsRSOGzr-" +
            "y9AqerJEy9BA"
    private val azureSystemToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vdGVzdC92Mi4w" +
            "IiwiaWF0IjoxNjU1ODc3MDQwLCJleHAiOjE2ODc0MTMwNDAsImF1ZCI6IjY3NjY2NDUtNTNkNS00OGY5LWJlOTctOTljN2ZjN" +
            "zRmMDlhIiwic3ViIjoiNTU1NTU1LTUzZDUtNDhmOS1iZTk3LTk5YzdmYzc0ZjA5YSIsImF6cF9uYW1lIjoiZGV2LWZzczpiaW" +
            "RyYWc6YmlkcmFnLWRva3VtZW50LWZlYXR1cmUiLCJyb2xlcyI6WyJhY2Nlc3NfYXNfYXBwbGljYXRpb24iLCJzb21ldGhpbmc" +
            "gZWxzZSJdfQ.XvdyJCtIt-ME4t956z76xOf2hrkM7WOvTRWjI6QcYiA"
    private val azureUserToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vdGVzdC92Mi4w" +
            "IiwiaWF0IjoxNjU1ODc3MDQwLCJleHAiOjE2ODc0MTMwNDAsImF1ZCI6IjY3NjY2NDUtNTNkNS00OGY5LWJlOTctOTljN2ZjN" +
            "zRmMDlhIiwic3ViIjoiNTU1NTU1LTUzZDUtNDhmOS1iZTk3LTk5YzdmYzc0ZjA5YSIsImF6cF9uYW1lIjoiZGV2LWZzczpiaW" +
            "RyYWc6YmlkcmFnLXVpLWZlYXR1cmUiLCJSb2xlIjoiYWNjZXNzX2FzX2FwcGxpY2F0aW9uIiwiTkFWaWRlbnQiOiJaOTk0OTc" +
            "3In0.7XhNn27iaKY-z4voUp-ZfR__5u3Rv5rJCgTpSNVW1nY"
    private val tokenXUserToken =
        "eyJraWQiOiJjNmY0YjdmYy0zMDM1LTRmNjctOGM1ZC04YTZhZmIwODUyYWQiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9" +
            ".eyJzdWIiOiIxOTUyNzAzMDkzOCIsImlzcyI6Imh0dHBz" +
            "Oi8vdG9rZW54LmRldi1nY3AubmF2LmNsb3VkLm5haXMuaW8iLCJjbGllbnRfYW1yIjoicHJpdmF0ZV9rZXlfand0IiwicGlkI" +
            "joiMTk1MjcwMzA5MzgiLCJjbGllbnRfaWQiOiJkZXYtZ2NwOm5haXM6dG9rZW54LXRva2VuLWdlbmVyYXRvciIsImF1ZCI6Im" +
            "Rldi1nY3A6YmlkcmFnOmJpZHJhZy1iaWRyYWdza2" +
            "Fsa3VsYXRvci1hcGkiLCJhY3IiOiJMZXZlbDQiLCJuYmYiOjE3NTE4ODYxNzIsImlkcCI6Imh0dHBzOi8vdGVzdC5pZHBvcnRlb" +
            "i5ubyIsInNjb3BlIjoib3BlbmlkIiwiZXhwIjoxNzUxODg5NzcyLCJpYXQiOjE3NTE4ODYxNzIsImp0aSI6ImMwMjA5ODlhL" +
            "WRjYTItNGJhNC1iNzQ3LTA1MDQ2NzE0MTg4ZSIs" +
            "ImNvbnN1bWVyIjp7ImF1dGhvcml0eSI6ImlzbzY1MjMtYWN0b3JpZC11cGlzIiwiSUQiOiIwMTkyOjg4OTY0MDc4MiJ9fQ.Tp0hKRbzE" +
            "_NnEJ6FrdWOG2ajbvi6Vl1X2sLX_204-TS7ZS_zs6pwgvQ_MxzmbmFmqEIKhyKPQBRg5VK-vtSnIXKCZx-C6uCtW6RpuV" +
            "7FBnXwhDm9xUH-Dv-gxep8rDYzvro4O_T8XIhHvy" +
            "UXYYbDw-SQs62Jhav18jt5x46IGt-1Ke63lwkA0bnA9jqwVCvrNySc0qKumqZIkHb44DcaxehumF-xBclYAulIArjRRe80WB5QlZrhi2Wa" +
            "sNh6qg6i0ZsUz5PfP22wapAo6suG8X16rZrLSNST_l4iGhuUBonCRGjgLyIbc6_c5IZaXMSso_UI1DHhzf58aob4SbyPSw\n"

    @AfterEach
    fun clearTokenContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun skalHenteSubjectFraAzureSystemToken() {
        mockTokenContext(azureSystemToken)
        val subject = TokenUtils.hentApplikasjonsnavn()

        // then
        subject shouldBe "bidrag-dokument-feature"
    }

    @Test
    fun skalHenteSaksbehandlerFraAzureToken() {
        mockTokenContext(azureUserToken)
        val subject = TokenUtils.hentBruker()

        // then
        subject shouldBe "Z994977"
    }

    @Test
    fun skalHenteSaksbehandlerIdentFraAzureToken() {
        mockTokenContext(azureUserToken)
        val subject = TokenUtils.hentSaksbehandlerIdent()

        // then
        subject shouldBe "Z994977"
    }

    @Test
    fun skalHenteSubjectFraIssoToken() {
        mockTokenContext(issoUser)
        val subject = TokenUtils.hentBruker()

        // then
        subject shouldBe "Z994977"
    }

    @Test
    fun skalHenteAppNavnFraIssoToken() {
        mockTokenContext(issoUser)
        val subject = TokenUtils.hentApplikasjonsnavn()

        // then
        subject shouldBe "bidrag-ui-feature-q1"
    }

    @Test
    fun skalHenteAppNavnFraAzureToken() {
        mockTokenContext(azureUserToken)
        val subject = TokenUtils.hentApplikasjonsnavn()

        // then
        subject shouldBe "bidrag-ui-feature"
    }

    @Test
    fun skalIkkeHenteSaksbehandlerHvisApplikasjonToken() {
        mockTokenContext(azureSystemToken)

        // then
        TokenUtils.hentSaksbehandlerIdent() shouldBe null
    }

    @Test
    fun skalHenteApplikasjonsnavnFraTokenxToken() {
        mockTokenContext(tokenXUserToken)
        TokenUtils.hentApplikasjonsnavn() shouldBe "tokenx-token-generator"
    }

    @Test
    fun skalHenteFødselsnummerFraTokenxToken() {
        mockTokenContext(tokenXUserToken)
        TokenUtils.hentBruker() shouldHaveLength 11
    }

    @Test
    fun shouldValidateSystemToken() {
        mockTokenContext(azureSystemToken)
        val resultAzure = TokenUtils.erApplikasjonsbruker()

        mockTokenContext(stsToken)
        val resultSTS = TokenUtils.erApplikasjonsbruker()

        mockTokenContext(azureUserToken)
        val resultAzureUser = TokenUtils.erApplikasjonsbruker()

        mockTokenContext(issoUser)
        val resultIsso = TokenUtils.erApplikasjonsbruker()

        // then
        resultAzure shouldBe true
        resultSTS shouldBe true
        resultAzureUser shouldBe false
        resultIsso shouldBe false
    }

    @Test
    fun shouldValidateTokenIssuedBy() {
        mockTokenContext(azureSystemToken)
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.AZURE) shouldBe true
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.STS) shouldBe false
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.TOKENX) shouldBe false

        mockTokenContext(stsToken)
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.AZURE) shouldBe false
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.STS) shouldBe true
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.TOKENX) shouldBe false

        mockTokenContext(tokenXUserToken)
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.AZURE) shouldBe false
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.STS) shouldBe false
        TokenUtils.erTokenUtstedtAv(TokenUtsteder.TOKENX) shouldBe true
    }

    @Test
    fun skalValidereApplikasjonBrukerIAppKontekst() {
        medApplikasjonKontekst {
            TokenUtils.erApplikasjonsbruker() shouldBe true
            TokenUtils.hentApplikasjonsnavn() shouldBe null
        }
    }

    fun mockTokenContext(token: String) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val requestAttributes = mockk<RequestAttributes>()
        RequestContextHolder.setRequestAttributes(requestAttributes)
        every {
            requestAttributes.getAttribute(
                SpringTokenValidationContextHolder::class.java.name,
                RequestAttributes.SCOPE_REQUEST,
            )
        } returns tokenValidationContext
        every { tokenValidationContext.hasValidToken() } returns true
        every { tokenValidationContext.firstValidToken } returns JwtToken(token)
    }
}
