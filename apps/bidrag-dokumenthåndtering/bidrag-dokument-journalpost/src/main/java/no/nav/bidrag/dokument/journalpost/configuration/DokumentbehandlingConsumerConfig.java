package no.nav.bidrag.dokument.journalpost.configuration;

import javax.xml.namespace.QName;
import no.nav.bidrag.dokument.journalpost.dokument.DokumentConsumer;
import no.nav.tjenester.brevogarkiv.dokumentbehandling.DokumentbehandlingPortType;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class DokumentbehandlingConsumerConfig {

  private static final String DOKUMENTBEHANDLING_NAMESPACE = "http://dokumentbehandling.brevogarkiv.tjenester.nav.no/";
  public static final QName DOKUMENTBEHANDLING_SERVICE = new QName(DOKUMENTBEHANDLING_NAMESPACE, "Dokumentbehandling");
  public static final QName DOKUMENTBEHANDLING_PORT = new QName(DOKUMENTBEHANDLING_NAMESPACE, "DokumentbehandlingPort");

  @Bean(name = Bus.DEFAULT_BUS_ID)
  public SpringBus springBus() {
    return new SpringBus();
  }

  @Bean
  public DokumentbehandlingPortType dokumentbehandlingPort(SpringBus bus, @Value("${BREVSERVER_URL}") String dokumentbehandlingEndpointUrl) {
    JaxWsProxyFactoryBean bean = new JaxWsProxyFactoryBean();
    bean.setServiceClass(DokumentbehandlingPortType.class);
    bean.setAddress(dokumentbehandlingEndpointUrl+"/Dokumentbehandling");
    bean.setEndpointName(DOKUMENTBEHANDLING_PORT);
    bean.setServiceName(DOKUMENTBEHANDLING_SERVICE);
    bean.setBus(bus);
    return bean.create(DokumentbehandlingPortType.class);
  }
}
