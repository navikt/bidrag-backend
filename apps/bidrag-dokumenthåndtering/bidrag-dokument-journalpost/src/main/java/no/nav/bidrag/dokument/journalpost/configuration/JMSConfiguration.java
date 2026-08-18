package no.nav.bidrag.dokument.journalpost.configuration;

import static com.ibm.mq.constants.CMQC.MQENC_NATIVE;
import static com.ibm.msg.client.jakarta.jms.JmsConstants.JMS_IBM_ENCODING;
import static com.ibm.msg.client.jakarta.wmq.common.CommonConstants.WMQ_CM_CLIENT;

import com.ibm.mq.jakarta.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jakarta.jms.JmsConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.xml.bind.JAXBException;
import no.nav.bidrag.dokument.journalpost.configuration.jms.JmsExceptionListener;
import no.nav.bidrag.dokument.journalpost.configuration.jms.JmsListenerErrorHandler;
import no.nav.bidrag.dokument.journalpost.configuration.jms.MQProperties;
import no.nav.bidrag.dokument.journalpost.dokument.DokumenttilgangMessageConverter;
import no.nav.bidrag.dokument.journalpost.mq.BrevKvittering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.jms.autoconfigure.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MarshallingMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableJms
@ConfigurationPropertiesScan
public class JMSConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(JMSConfiguration.class);

  private final MQProperties mqProperties;

  public JMSConfiguration(MQProperties mqProperties) {
    this.mqProperties = mqProperties;
  }

  @Bean
  public JmsTemplate jmsTemplate(ConnectionFactory mqQueueConnectionFactory) {
    JmsTemplate template = new JmsTemplate();
    template.setConnectionFactory(mqQueueConnectionFactory);
    template.setDefaultDestinationName(mqProperties.getBrevserverQueue());
    template.setMessageConverter(new DokumenttilgangMessageConverter());
    return template;
  }

  @Bean
  @Profile("nais")
  public ConnectionFactory mqQueueConnectionFactory() throws JMSException {
    MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();

    connectionFactory.setHostName(mqProperties.getHostName());
    connectionFactory.setPort(mqProperties.getPort());
    connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true);
    connectionFactory.setQueueManager(mqProperties.getName());
    connectionFactory.setChannel(mqProperties.getChannel().toUpperCase());
    connectionFactory.setTransportType(WMQ_CM_CLIENT);
    connectionFactory.setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE);
    UserCredentialsConnectionFactoryAdapter credentialQueueConnectionFactory = new UserCredentialsConnectionFactoryAdapter();
    credentialQueueConnectionFactory.setUsername(mqProperties.getUsername());
    credentialQueueConnectionFactory.setPassword(mqProperties.getPassword());
    credentialQueueConnectionFactory.setTargetConnectionFactory(connectionFactory);
    return credentialQueueConnectionFactory;
  }

  public DefaultJmsListenerContainerFactory defaultConnetionFactory(){
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
    factory.setSessionTransacted(true);
    factory.setErrorHandler(new JmsListenerErrorHandler());
    factory.setExceptionListener(new JmsExceptionListener());
    ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
    exponentialBackOff.setInitialInterval(mqProperties.getBackOffInitialInterval());
    exponentialBackOff.setMaxInterval(mqProperties.getBackOffMaxInterval());
    factory.setBackOff(exponentialBackOff);
    return factory;
  }
  @Bean
  public JmsListenerContainerFactory<?> brevserverKvitteringListenerFactory(ConnectionFactory mqQueueConnectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = defaultConnetionFactory();
    Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
    jaxb2Marshaller.setClassesToBeBound(BrevKvittering.class);
    factory.setMessageConverter(new MarshallingMessageConverter(jaxb2Marshaller));
    configurer.configure(factory, mqQueueConnectionFactory);
    return factory;
  }

}