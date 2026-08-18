package no.nav.bidrag.dokument.journalpost;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.LIVE;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
public class BidragDokumentJournalpost {

  public static final Logger SECURE_LOGGER = LoggerFactory.getLogger("secureLogger");

  public static void main(String... args) {

    String profile = args.length < 1 ? LIVE : args[0];

    SpringApplication app = new SpringApplication(BidragDokumentJournalpost.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

}
