package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MultiplikatorYaml {
  private final BigDecimal multiplikator;

  @JsonCreator
  public MultiplikatorYaml(String multiplikatorString) {
    if (multiplikatorString.length() < 1 || !multiplikatorString.endsWith("x")) {
      throw new IllegalArgumentException(
          "'" + multiplikatorString + "' er ikke et gyldig multiplikator.");
    }
    multiplikator =
        new BigDecimal(
            multiplikatorString.replaceAll("x", "").replaceAll("\\.", "").replaceAll(",", "."));
  }
}
