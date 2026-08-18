package no.nav.bidrag.sjablon.yaml;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TallYaml {
  private final BigDecimal verdi;

  public TallYaml(String tallString) {
    // TODO: Validere...

    verdi =
        new BigDecimal(
            tallString.replaceAll("\\.", "").replaceAll(",-", ".00").replaceAll(",", "."));
  }
}
