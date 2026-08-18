package no.nav.bidrag.sjablon.yaml;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BeløpYaml {
  private final BigDecimal beløp;

  public BeløpYaml(String beløpString) {
    beløp =
        new BigDecimal(
            beløpString.replaceAll("\\.", "").replaceAll(",-", ".00").replaceAll(",", "."));
  }

  public BeløpYaml erNullEllerOver() {
    checkArgument(BigDecimal.ZERO.compareTo(beløp) <= 0, "Beløp er ikke 0 eller over");
    return this;
  }

  public BeløpYaml erOverNull() {
    checkArgument(BigDecimal.ZERO.compareTo(beløp) < 0, "Beløp er ikke over 0");
    return this;
  }
}
