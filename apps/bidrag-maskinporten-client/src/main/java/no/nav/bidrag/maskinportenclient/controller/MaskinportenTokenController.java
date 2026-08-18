package no.nav.bidrag.maskinportenclient.controller;

import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.text.ParseException;
import no.nav.bidrag.maskinportenclient.service.MaskinportenTokenClient;
import no.nav.bidrag.maskinportenclient.service.TokenResponse;
import no.nav.bidrag.maskinportenclient.validation.ScopeValidation;
import no.nav.bidrag.maskinportenclient.validation.ScopeValidator;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Protected
@RestController
@Tag(name = "Token")
public class MaskinportenTokenController {

  private final MaskinportenTokenClient maskinportenTokenClient;

  private final ScopeValidator scopeValidator;


  @Autowired
  public MaskinportenTokenController(MaskinportenTokenClient maskinportenTokenClient,
      ScopeValidator scopeValidator) {
    this.maskinportenTokenClient = maskinportenTokenClient;
    this.scopeValidator = scopeValidator;
  }

  @GetMapping(value = "/token")
  @Operation(
      summary = "Hent Maskinporten-token med gitte scopes.",
      description = "Gyldig Maskinporten-token returneres dersom alle oppgitte scopes finnes i konfigurasjonen til klienten.\n\nKjente scopes: \n\nMot ELINs kravApi: nav:bidrag/v1/bidragskrav \n\nFra bidrag-aktoerregister: nav:bidrag:aktoerregister.read",
      security = {@SecurityRequirement(name = "bearer-key")}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Gyldig token returnert"),
      @ApiResponse(responseCode = "400", description = "Noen eller alle oppgitte scopes finnes ikke i konfigurasjonen", content = @Content()),
  })
  public ResponseEntity<String> getToken(String scopes)
      throws IOException, JOSEException, InterruptedException, ParseException {
    ScopeValidation scopeValidation = scopeValidator.validate(scopes);
    if (!scopeValidation.isValid()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Some or all of the provided scopes are currently not supported. The following scopes are missing in the configuration: "
              + scopeValidation.getUnsupportedScopes());
    }
    TokenResponse tokenResponse = maskinportenTokenClient.getToken(
        scopeValidation.getRequestedScopes());
    if (tokenResponse.getError() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Error: " + tokenResponse.getError() + ". " + tokenResponse.getError_description());
    }
    return ResponseEntity.ok(tokenResponse.getAccess_token());
  }
}
