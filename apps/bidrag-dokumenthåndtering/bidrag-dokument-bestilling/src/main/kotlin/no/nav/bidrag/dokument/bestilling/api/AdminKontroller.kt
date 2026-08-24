package no.nav.bidrag.dokument.bestilling.api

import com.ibm.mq.constants.CMQC
import com.ibm.msg.client.jakarta.jms.JmsConstants
import io.micrometer.core.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.dokument.bestilling.SIKKER_LOGG
import no.nav.bidrag.dokument.bestilling.bestilling.dto.hentDokumentMal
import no.nav.bidrag.dokument.bestilling.model.dokumentMalEksistererIkke
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.xml.parsers.DocumentBuilderFactory

@RestController
@Protected
@Timed
class AdminKontroller(
    private val onlinebrevTemplate: JmsTemplate,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AdminKontroller::class.java)
    }

    @PostMapping("/bestill/xml/{dokumentMalKode}")
    @Operation(
        description = "Bestiller dokument for oppgitt brevkode/dokumentKode",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "400",
                description = "Dokument ble bestilt med ugyldig data",
            ),
        ],
    )
    fun bestillBrev(
        @RequestBody xml: String,
        @PathVariable dokumentMalKode: String,
    ) {
        val dokumentMal =
            hentDokumentMal(dokumentMalKode) ?: dokumentMalEksistererIkke(dokumentMalKode)
        LOGGER.info("Bestiller dokument for dokumentmal $dokumentMal med XML som input")
        SIKKER_LOGG.info("Bestiller dokument for dokumentmal $dokumentMal med XML $xml")
        if (!isValidXml(xml)) throw IllegalArgumentException("Ugyldig XML")
        onlinebrevTemplate.send {
            val message = it.createTextMessage(xml)
            message.setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, 277)
            message.setIntProperty(JmsConstants.JMS_IBM_MSGTYPE, CMQC.MQMT_DATAGRAM)
            message.setIntProperty(JmsConstants.JMS_IBM_PUTAPPLTYPE, CMQC.MQAT_CICS)
            SIKKER_LOGG.info("Sending message \n\n$xml\n\n")
            message
        }
    }

    private fun isValidXml(xml: String): Boolean =
        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(java.io.ByteArrayInputStream(xml.toByteArray()))
            true
        } catch (e: Exception) {
            false
        }
}
