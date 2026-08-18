package no.nav.bidrag.maskinportenclient.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("maskinporten")
public class MaskinportenConfig {

  private String tokenUrl;
  private String audience;
  private String clientId;
  private String scope;
  private String privateKey;
}
