package no.nav.bidrag.behandling.transformers.vedtak

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import no.nav.bidrag.behandling.consumer.BidragPersonConsumer
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.transformers.grunnlag.tilGrunnlagPerson
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.testdata.oppretteBehandling
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.personIdent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate

class HentPersonerNyesteIdentTest {
    private val søktIdent = genererFødselsnummer()
    private val personConsumer = mockkClass(BidragPersonConsumer::class)
    private lateinit var personGrunnlag: GrunnlagDto

    @BeforeEach
    fun setup() {
        stubPersonConsumer(personConsumer)
        personGrunnlag =
            Rolle(
                behandling = oppretteBehandling(),
                ident = søktIdent,
                rolletype = Rolletype.BARN,
                fødselsdato = LocalDate.parse("2015-05-01"),
                id = 1L,
            ).tilGrunnlagPerson()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `skal matche på søkt ident når identoppslaget gir 403`() {
        every { personConsumer.hentPerson(søktIdent) } throws HttpClientErrorException(HttpStatus.FORBIDDEN)

        val resultat = listOf(personGrunnlag).hentPersonerNyesteIdent(søktIdent)

        resultat shouldHaveSize 1
        resultat.first().personIdent shouldBe søktIdent
    }

    @Test
    fun `skal matche på søkt ident når identoppslaget gir 404`() {
        every { personConsumer.hentPerson(søktIdent) } throws HttpClientErrorException(HttpStatus.NOT_FOUND)

        val resultat = listOf(personGrunnlag).hentPersonerNyesteIdent(søktIdent)

        resultat shouldHaveSize 1
        resultat.first().personIdent shouldBe søktIdent
    }

    @Test
    fun `skal kaste videre andre feil fra identoppslaget`() {
        every { personConsumer.hentPerson(søktIdent) } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        assertThrows<HttpServerErrorException> {
            listOf(personGrunnlag).hentPersonerNyesteIdent(søktIdent)
        }
    }
}
