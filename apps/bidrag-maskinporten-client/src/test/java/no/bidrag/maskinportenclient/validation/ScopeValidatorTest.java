package no.bidrag.maskinportenclient.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.nav.bidrag.maskinportenclient.config.MaskinportenConfig;
import no.nav.bidrag.maskinportenclient.validation.ScopeValidator;
import org.junit.jupiter.api.Test;

public class ScopeValidatorTest {

  @Test
  public void TestIsValid() {
    MaskinportenConfig maskinportenConfig = new MaskinportenConfig();
    maskinportenConfig.setScope("nav:bidrag:aktoerregisteret.read nav:bidrag:test.read nav:bidrag:test2.read");
    ScopeValidator scopeValidator = new ScopeValidator(maskinportenConfig);
    assertTrue(scopeValidator.validate("nav:bidrag:aktoerregisteret.read").isValid());
    assertTrue(scopeValidator.validate("nav:bidrag:test.read").isValid());
    assertTrue(scopeValidator.validate("nav:bidrag:aktoerregisteret.read,nav:bidrag:test.read").isValid());
    assertTrue(scopeValidator.validate("nav:bidrag:aktoerregisteret.read,nav:bidrag:test.read,nav:bidrag:test2.read").isValid());
    assertTrue(scopeValidator.validate("nav:bidrag:test.read,nav:bidrag:test2.read").isValid());
    assertTrue(scopeValidator.validate("nav:bidrag:test2.read,nav:bidrag:test.read").isValid());

    assertFalse(scopeValidator.validate("nav:bidrag:aktoerregisteret.read,nav:bidrag:test.read,nav:bidrag:test3.read").isValid());

  }
}
