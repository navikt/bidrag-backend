package no.nav.bidrag.dokument.journalpost;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.LOCAL;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.transport.dokument.JournalpostHendelse;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"springfox.documentation.swagger.web.ApiResourceController"})
@ComponentScan(excludeFilters = {@Filter(type = ASSIGNABLE_TYPE, value = BidragDokumentJournalpost.class)})
public class BidragDokumentJournalpostLocalTest {

  public static void main(String... args) {
    String profile = args.length < 1 ? LOCAL : args[0];

    SpringApplication app = new SpringApplication(BidragDokumentJournalpostLocalTest.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

  @Configuration
  @Profile(LOCAL)
  @EmbeddedKafka
  public static class NoKafkaSupportConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoKafkaSupportConfiguration.class);

    @Bean
    JournalpostKafkaEventProducer journalpostKafkaEventProducer() {
      return new JournalpostKafkaEventProducer(null, null, null) {
        @Override
        public void publish(JournalpostHendelse journalpostHendelse) {
          LOGGER.info("Deployed application will publish {}!", journalpostHendelse);
        }
      };
    }
  }
}
