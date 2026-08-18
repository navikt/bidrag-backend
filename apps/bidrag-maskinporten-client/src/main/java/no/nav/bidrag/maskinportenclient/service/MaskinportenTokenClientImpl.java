package no.nav.bidrag.maskinportenclient.service;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import no.nav.bidrag.maskinportenclient.config.MaskinportenConfig;
import no.nav.bidrag.maskinportenclient.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class MaskinportenTokenClientImpl implements MaskinportenTokenClient {

  private final HttpClient httpClient;

  private final MaskinportenConfig maskinportenConfig;

  @Autowired
  public MaskinportenTokenClientImpl(MaskinportenConfig maskinportenConfig) {
    httpClient = HttpClient.newBuilder().build();
    this.maskinportenConfig = maskinportenConfig;
  }

  @Override
  public TokenResponse getToken(String scopes) throws JOSEException, IOException, InterruptedException, ParseException {
    HttpResponse<String> response = httpClient.send(getTokenRequest(scopes), ofString());
    return JsonUtil.jsonStringToObject(response.body(), TokenResponse.class);
  }

  private HttpRequest getTokenRequest(String scopes) throws JOSEException, ParseException {
    String requestBody = getMaskinportenRequestBody(scopes);
    return HttpRequest.newBuilder()
        .uri(URI.create(maskinportenConfig.getTokenUrl()))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .POST(BodyPublishers.ofString(requestBody))
        .build();
  }

  private String getMaskinportenRequestBody(String scopes) throws JOSEException, ParseException {
    RSAKey rsaKey = RSAKey.parse(maskinportenConfig.getPrivateKey());
    SignedJWT signedJWT = new SignedJWT(getSignatureHeader(rsaKey), getJwtClaimSet(scopes, new Date()));
    signedJWT.sign(new RSASSASigner(rsaKey.toPrivateKey()));
    String signedJWTGrant = signedJWT.serialize();
    return "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" + "&assertion=" + signedJWTGrant;
  }

  private JWSHeader getSignatureHeader(RSAKey rsaKey) {
    return new JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(rsaKey.getKeyID())
        .type(JOSEObjectType.JWT)
        .build();
  }

  private JWTClaimsSet getJwtClaimSet(String scopes, Date issueTime) {
    return new JWTClaimsSet.Builder()
        .audience(maskinportenConfig.getAudience())
        .issuer(maskinportenConfig.getClientId())
        .claim("scope", scopes)
        .issueTime(issueTime)
        .expirationTime(getExpirationTime(issueTime))
        .build();
  }

  private Date getExpirationTime(Date issueTime) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(issueTime);
    calendar.add(Calendar.SECOND, 120);
    return calendar.getTime();
  }
}
