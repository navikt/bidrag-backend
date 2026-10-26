package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Krypteringsverktøy {

  private static final String ADVANCED_ENCRYPTION_STANDARD_ALGORITME = "AES";
  private static final String KRYPTERINGSALGORITME = "PBKDF2WithHmacSHA256";
  private static final String TRANSFORMERINGSALGORITME = ADVANCED_ENCRYPTION_STANDARD_ALGORITME + "/GCM/NoPadding";
  private static final String GCM_FORMATVERSJON = "GCM1:";
  private static final int GCM_NONCE_LENGDE = 12;
  private static final int GCM_TAG_LENGDE_BIT = 128;
  private static String krypteringsPassord;
  private static String krypteringsSalt;

  public Krypteringsverktøy(@Value("${KRYPTERINGSPASSORD:MISSING}") String krypteringsPassord,
                            @Value("${KRYPTERINGSSALT:MISSING}") String krypteringsSalt) {
    if ("MISSING".equals(krypteringsPassord) || "MISSING".equals(krypteringsSalt)) {
      log.error("KRYPTERINGSPASSORD or KRYPTERINGSSALT was not found in the environment!");
      throw new IllegalStateException("KRYPTERINGSPASSORD or KRYPTERINGSSALT was not found in the environment!");
    }
    this.krypteringsPassord = krypteringsPassord.strip();
    this.krypteringsSalt = krypteringsSalt.strip();
  }

  public static String kryptere(String ikkeKryptertStreng) {
    try {
      var nonce = new byte[GCM_NONCE_LENGDE];
      new SecureRandom().nextBytes(nonce);
      var parameterSpec = new GCMParameterSpec(GCM_TAG_LENGDE_BIT, nonce);

      var cipher = Cipher.getInstance(TRANSFORMERINGSALGORITME);
      cipher.init(Cipher.ENCRYPT_MODE, henteHemmeligNøkkel(), parameterSpec);
      var kryptert = cipher.doFinal(ikkeKryptertStreng.getBytes(StandardCharsets.UTF_8));

      var kombinert = new byte[nonce.length + kryptert.length];
      System.arraycopy(nonce, 0, kombinert, 0, nonce.length);
      System.arraycopy(kryptert, 0, kombinert, nonce.length, kryptert.length);

      return GCM_FORMATVERSJON + Base64.getEncoder().encodeToString(kombinert);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException |
             IllegalBlockSizeException e) {
      log.error("Feil ved kryptering", e);
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }

  public static String dekryptere(String kryptertStreng) {
    try {
      var base64Streng = kryptertStreng.substring(GCM_FORMATVERSJON.length());
      var dekodet = Base64.getDecoder().decode(base64Streng);
      var ivLengde = GCM_NONCE_LENGDE;
      if (dekodet.length < ivLengde) {
        throw new InternFeil(Feilkode.KRYPTERING, new IllegalArgumentException("Kryptert verdi er for kort"));
      }

      var iv = new byte[ivLengde];
      System.arraycopy(dekodet, 0, iv, 0, iv.length);

      var kryptert = new byte[dekodet.length - iv.length];
      System.arraycopy(dekodet, iv.length, kryptert, 0, kryptert.length);

      var cipher = Cipher.getInstance(TRANSFORMERINGSALGORITME);
      cipher.init(Cipher.DECRYPT_MODE, henteHemmeligNøkkel(), new GCMParameterSpec(GCM_TAG_LENGDE_BIT, iv));
      return new String(cipher.doFinal(kryptert), StandardCharsets.UTF_8);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException |
             IllegalBlockSizeException e) {
      log.error("Feil ved dekryptering", e);
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }


  private static SecretKey henteHemmeligNøkkel() {
    try {
      var factory = SecretKeyFactory.getInstance(KRYPTERINGSALGORITME);
      var spec = new PBEKeySpec(krypteringsPassord.toCharArray(), krypteringsSalt.getBytes(StandardCharsets.UTF_8), 65536, 256);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ADVANCED_ENCRYPTION_STANDARD_ALGORITME);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }
}
