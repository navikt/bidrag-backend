package no.nav.bidrag.sjablon.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ProsentYaml {
  public static final BigDecimal PROSENT_NEVNER = BigDecimal.valueOf(100);
  private final BigDecimal faktor;

  @JsonCreator
  public ProsentYaml(String prosentString) {
    if (prosentString.length() < 1 || !prosentString.endsWith("%")) {
      throw new IllegalArgumentException("'" + prosentString + "' er ikke et gyldig prosent-tall.");
    }
    faktor =
        new BigDecimal(
                prosentString.replaceAll("\\%", "").replaceAll("\\.", "").replaceAll(",", "."))
            .divide(PROSENT_NEVNER);
  }

  public BigDecimal getProsentsats() {
    return faktor != null ? faktor.multiply(PROSENT_NEVNER) : null;
  }
}
