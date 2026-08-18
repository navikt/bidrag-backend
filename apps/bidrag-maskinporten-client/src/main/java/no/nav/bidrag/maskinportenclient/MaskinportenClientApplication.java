package no.nav.bidrag.maskinportenclient;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;

@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
public class MaskinportenClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaskinportenClientApplication.class, args);
    }
}
