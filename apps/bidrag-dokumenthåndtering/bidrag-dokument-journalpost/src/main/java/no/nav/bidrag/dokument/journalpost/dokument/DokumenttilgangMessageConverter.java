package no.nav.bidrag.dokument.journalpost.dokument;

import static no.nav.bidrag.dokument.journalpost.dto.Dokumenttilgang.ROOT_ELEMENT_NAME;

import java.io.StringWriter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import no.nav.bidrag.dokument.journalpost.dto.Dokumenttilgang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

public class DokumenttilgangMessageConverter implements MessageConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DokumenttilgangMessageConverter.class);

  @Override
  public Message toMessage(Object object, Session session) throws JMSException {
    var dokumenttilgang = (Dokumenttilgang) object;
    var sw = new StringWriter();
    try {
      var jaxbContext = JAXBContext.newInstance(dokumenttilgang.getClass());
      var jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
      var qName = new QName(ROOT_ELEMENT_NAME);
      JAXBElement<Dokumenttilgang> root = new JAXBElement<>(qName, Dokumenttilgang.class, dokumenttilgang);
      jaxbMarshaller.marshal(root, sw);
    } catch (JAXBException e) {
      LOGGER.error("Kunne ikke konvertere Dokumenttilgang til JMS melding: {}", object, e);
    }
    var message = session.createTextMessage();
    message.setText(sw.toString());
    return message;
  }

  @Override
  public Object fromMessage(Message message) throws JMSException, MessageConversionException {
    return null;
  }
}
