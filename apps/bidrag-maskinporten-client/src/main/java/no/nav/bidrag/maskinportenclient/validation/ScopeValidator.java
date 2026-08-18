package no.nav.bidrag.maskinportenclient.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import no.nav.bidrag.maskinportenclient.config.MaskinportenConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScopeValidator {

  private final MaskinportenConfig maskinportenConfig;

  @Autowired
  public ScopeValidator(MaskinportenConfig maskinportenConfig) {
    this.maskinportenConfig = maskinportenConfig;
  }

  public ScopeValidation validate(String scopes) {
    List<String> requestedScopes = Arrays.stream(scopes.split(",")).toList();
    List<String> validScopes = Arrays.stream(maskinportenConfig.getScope().split(" ")).toList();
    List<String> unsupportedScopes = new ArrayList<>();
    requestedScopes.forEach(s -> {
      if (!validScopes.contains(s)) {
        unsupportedScopes.add(s);
      }
    });
    return new ScopeValidation(unsupportedScopes.isEmpty(), String.join(",", unsupportedScopes), String.join(" ", requestedScopes));
  }
}
