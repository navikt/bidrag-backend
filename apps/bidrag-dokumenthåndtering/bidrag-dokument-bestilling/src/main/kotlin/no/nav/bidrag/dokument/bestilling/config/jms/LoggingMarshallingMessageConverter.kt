package no.nav.bidrag.dokument.bestilling.config.jms

import com.ibm.mq.constants.CMQC
import com.ibm.msg.client.jakarta.jms.JmsConstants
import jakarta.jms.Destination
import jakarta.jms.Session
import jakarta.jms.TextMessage
import no.nav.bidrag.dokument.bestilling.SIKKER_LOGG
import org.springframework.jms.support.converter.MarshallingMessageConverter
import org.springframework.jms.support.converter.MessageType
import org.springframework.oxm.Marshaller
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import java.io.StringWriter
import javax.xml.transform.stream.StreamResult

class LoggingMarshallingMessageConverter(
    jaxb2Marshaller: Jaxb2Marshaller,
    private val replyDestinationQueue: Destination,
) : MarshallingMessageConverter(jaxb2Marshaller) {
    init {
        setTargetType(MessageType.TEXT)
    }

    override fun marshalToTextMessage(
        o: Any,
        session: Session,
        marshaller: Marshaller,
    ): TextMessage {
        val writer = StringWriter(1024)
        val result = StreamResult(writer)
        marshaller.marshal(o, result)
        val messageString = writer.toString()
        val cleanedMessageString =
            messageString
                .replace("xsi:nil=\"true\"", "")
                .replace("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "")
                .replace("\\s{2,}".toRegex(), "")
        SIKKER_LOGG.info("Sending message \n\n$cleanedMessageString\n\n")
        val message = session.createTextMessage(cleanedMessageString)
        message.setIntProperty(JmsConstants.JMS_IBM_CHARACTER_SET, 277)
        message.setIntProperty(JmsConstants.JMS_IBM_MSGTYPE, CMQC.MQMT_DATAGRAM)
        message.setIntProperty(JmsConstants.JMS_IBM_PUTAPPLTYPE, CMQC.MQAT_CICS)
        message.jmsReplyTo = replyDestinationQueue
        return message
    }
}
