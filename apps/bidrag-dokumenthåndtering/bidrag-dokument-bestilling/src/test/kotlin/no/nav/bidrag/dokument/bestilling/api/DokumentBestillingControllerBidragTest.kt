package no.nav.bidrag.dokument.bestilling.api

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingForespørsel
import no.nav.bidrag.dokument.bestilling.api.dto.DokumentBestillingResponse
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.bestilling.produksjon.dto.BrevBestilling
import no.nav.bidrag.dokument.bestilling.utils.ANNEN_MOTTAKER
import no.nav.bidrag.dokument.bestilling.utils.BM1
import no.nav.bidrag.dokument.bestilling.utils.BP1
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal

class DokumentBestillingControllerBidragTest : AbstractControllerTest() {
    @Test
    fun `skal produsere XML for vedtaksbrev bidrag`() {
        stubDefaultValues()
        stubUtils.stubHentPerson("16451299577", ANNEN_MOTTAKER)
        stubUtils.stubHentPerson("25451755601", ANNEN_MOTTAKER)
        stubUtils.stubHentVedtak("vedtak_response-bidrag-2.json")
        val enhetKontaktInfo = createEnhetKontaktInformasjon()
        val bmAdresse = createPostAdresseResponse()
        val dokumentMal = hentDokumentMal("BI01E01")!!
        val tittel = "Tittel på dokument"
        val saksnummer = "123213"
        val mottakerId = BM1.ident
        val gjelderId = BP1.ident

        stubUtils.stubHentAdresse(postAdresse = bmAdresse)
        stubUtils.stubEnhetKontaktInfo(enhetKontaktInfo)

        val request =
            DokumentBestillingForespørsel(
                mottakerId = mottakerId.verdi,
                gjelderId = gjelderId.verdi,
                saksnummer = saksnummer,
                tittel = tittel,
                vedtakId = 12312,
                enhet = "4806",
                spraak = "NB",
                dokumentreferanse = "BIF1234567",
            )

        jmsTestConsumer.withOnlinebrev {
            val response =
                httpHeaderTestRestTemplate.exchange(
                    "${rootUri()}/bestill/${dokumentMal.kode}",
                    HttpMethod.POST,
                    HttpEntity(request),
                    DokumentBestillingResponse::class.java,
                )

            response.statusCode shouldBe HttpStatus.OK

            val særbidragVerdi = BigDecimal(1909)
            val message: BrevBestilling = this.getMessageAsObject(BrevBestilling::class.java)!!
            assertSoftly {
                verifyBrevbestillingHeaders(message, dokumentMal)
                message.validateKontaktInformasjon(enhetKontaktInfo, BM1, BP1, bmAdresse)
            }
        }
    }
}
