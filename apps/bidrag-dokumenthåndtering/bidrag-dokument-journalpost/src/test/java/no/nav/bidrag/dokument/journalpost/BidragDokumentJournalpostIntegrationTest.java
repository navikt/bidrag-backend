package no.nav.bidrag.dokument.journalpost;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"springfox.documentation.swagger.web.ApiResourceController"})
@ComponentScan(excludeFilters = {@Filter(type = ASSIGNABLE_TYPE, value = BidragDokumentJournalpost.class),
    @Filter(type = ASSIGNABLE_TYPE, value = BidragDokumentJournalpostLocalTest.class)})
public class BidragDokumentJournalpostIntegrationTest {

  public static void main(String... args) {

    String profile = args.length < 1 ? BidragDokumentJournalpostProfiles.INTEGRATION_ABAC : args[0];

    SpringApplication app = new SpringApplication(BidragDokumentJournalpostIntegrationTest.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }
}
